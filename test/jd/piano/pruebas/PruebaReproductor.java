package jd.piano.pruebas;

import jd.piano.programa.MultiPiano;
import jd.piano.programa.PianoSencillo;
import jd.piano.programa.Reproductor;
import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import javax.sound.midi.ShortMessage;
import java.io.File;
import java.util.List;

/** El transporte y el silenciado por canal. */
public final class PruebaReproductor {

    private PruebaReproductor() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("reproductor");
        File[] canciones = PruebaMidi.canciones();
        if (canciones.length == 0) {
            c.comprobar("hay canciones para probar", false);
            return c;
        }
        transporte(c, canciones[0].getPath());
        notasColgadas(c);
        silenciado(c);
        return c;
    }

    private static void transporte(Comprobador c, String cancion) {
        Piano piano = new PianoSencillo(24, 108);
        piano.setPosicion(0, 0);
        Reproductor r = new Reproductor();
        r.conectar(piano);
        try {
            c.comprobar("sin cancion cargada, no hay cancion", !r.hayCancion());
            r.cargar(cancion);
            c.comprobar("tras cargar hay cancion y dura algo",
                    r.hayCancion() && r.getDuracionUs() > 0);
            c.comprobar("empieza parado y en el principio",
                    !r.estaSonando() && r.getPosicionUs() == 0);

            r.reproducir();
            c.comprobar("al reproducir empieza a sonar", r.estaSonando());
            Thread.sleep(500);
            c.comprobar("la posicion avanza", r.getPosicionUs() > 0);

            // Hay canciones que empiezan con segundos de silencio, asi que hay
            // que esperar a que de verdad haya notas antes de comprobarlo
            int encendidas = esperarNotas(piano, 9000);
            c.dato("teclas encendidas al detectarlas: " + encendidas);
            c.comprobar("los eventos llegan y encienden teclas", encendidas > 0);

            r.pausar();
            c.comprobar("al pausar se para y se sueltan las teclas",
                    !r.estaSonando() && contar(piano) == 0);
            long enPausa = r.getPosicionUs();
            Thread.sleep(300);
            c.comprobar("en pausa la posicion no avanza",
                    Math.abs(r.getPosicionUs() - enPausa) < 60_000);

            r.buscar(r.getDuracionUs() / 2);
            c.comprobar("buscar mueve la posicion",
                    Math.abs(r.getPosicionUs() - r.getDuracionUs() / 2) < 600_000);
            r.buscar(-9_000_000);
            c.comprobar("buscar antes del principio se queda en 0",
                    r.getPosicionUs() == 0);
            r.buscar(r.getDuracionUs() * 5);
            c.comprobar("buscar tras el final se queda en la duracion",
                    r.getPosicionUs() <= r.getDuracionUs() + 2000);

            r.setFactorTempo(2f);
            c.comprobar("el tempo se aplica", Math.abs(r.getFactorTempo() - 2f) < 0.01);
            r.setFactorTempo(50f);
            c.comprobar("el tempo tiene tope arriba", r.getFactorTempo() <= 3.01f);
            r.setFactorTempo(0.001f);
            c.comprobar("el tempo tiene tope abajo", r.getFactorTempo() >= 0.24f);
            r.setFactorTempo(1f);

            r.parar();
            c.comprobar("al parar vuelve al principio y sin teclas",
                    r.getPosicionUs() == 0 && !r.estaSonando() && contar(piano) == 0);
            r.cerrar();
            r.cerrar();
            c.comprobar("cerrar dos veces no revienta", true);
        } catch (Exception error) {
            c.comprobar("el transporte no lanza excepciones: " + error, false);
        }
    }

    /** Las dos vias por las que se quedaban notas encendidas para siempre. */
    private static void notasColgadas(Comprobador c) {
        Piano piano = new PianoSencillo(24, 108);
        piano.setPosicion(0, 0);
        Reproductor r = new Reproductor();
        r.conectar(piano);
        try {
            r.send(nota(ShortMessage.NOTE_ON, 0, 60, 100), -1);
            c.comprobar("un NOTE_ON enciende su tecla", contar(piano) == 1);

            r.send(nota(ShortMessage.CONTROL_CHANGE, 0, 123, 0), -1);
            c.comprobar("el controlador 123 (All Notes Off) apaga", contar(piano) == 0);

            r.send(nota(ShortMessage.NOTE_ON, 0, 60, 100), -1);
            r.send(nota(ShortMessage.CONTROL_CHANGE, 0, 120, 0), -1);
            c.comprobar("el controlador 120 (All Sound Off) apaga", contar(piano) == 0);

            r.send(nota(ShortMessage.NOTE_ON, 9, 60, 100), -1);
            c.comprobar("la percusion se ignora", contar(piano) == 0);
        } catch (Exception error) {
            c.comprobar("las notas colgadas no lanzan excepciones: " + error, false);
        }
        r.cerrar();
    }

    private static void silenciado(Comprobador c) {
        // En el multipiano cada canal tiene su teclado, que es donde se nota
        List<Integer> canales = List.of(0, 1, 2);
        MultiPiano piano = new MultiPiano(canales, 24, 108,
                LayoutTeclados.calcular(3, Piano.contarBlancas(24, 108), 1200, 600));
        piano.setPosicion(0, 0);
        Reproductor r = new Reproductor();
        r.conectar(piano);
        try {
            r.send(nota(ShortMessage.NOTE_ON, 1, 64, 100), -1);
            c.comprobar("sin silenciar, la nota enciende su tecla",
                    piano.getTecla(1, 64).estaPulsado());

            // Otra nota en otro canal, y se silencia el primero
            r.send(nota(ShortMessage.NOTE_ON, 2, 67, 100), -1);
            r.setSilenciado(1, true);
            c.comprobar("silenciar un canal no apaga los otros",
                    piano.getTecla(2, 67).estaPulsado());
            c.comprobar("silenciar apaga las teclas de su canal",
                    !piano.getTecla(1, 64).estaPulsado());

            r.send(nota(ShortMessage.NOTE_ON, 1, 64, 100), -1);
            c.comprobar("un canal silenciado ya no enciende nada",
                    !piano.getTecla(1, 64).estaPulsado());

            // Con el canal silenciado, un apagado si tiene que pasar: si no, una
            // nota que sonara al silenciarlo se quedaria encendida para siempre
            r.setSilenciado(1, false);
            r.send(nota(ShortMessage.NOTE_ON, 1, 64, 100), -1);
            r.setSilenciado(1, true);
            r.send(nota(ShortMessage.NOTE_OFF, 1, 64, 0), -1);
            r.setSilenciado(1, false);
            c.comprobar("los apagados pasan aunque el canal este silenciado",
                    !piano.getTecla(1, 64).estaPulsado());

            c.comprobar("un canal fuera de rango no revienta", fueraDeRango(r));

            // Cargar otra cancion no debe arrastrar los silenciados, porque el
            // panel de canales se rehace con todo activado
            r.setSilenciado(2, true);
            r.cargar(PruebaMidi.canciones()[0].getPath());
            c.comprobar("al cargar otra cancion se limpian los silenciados",
                    !r.estaSilenciado(2));
        } catch (Exception error) {
            c.comprobar("el silenciado no lanza excepciones: " + error, false);
        }
        r.cerrar();
    }

    private static boolean fueraDeRango(Reproductor r) {
        boolean ok = true;
        try {
            r.setSilenciado(-5, true);
            r.setSilenciado(999, true);
            r.estaSilenciado(-1);
            r.estaSilenciado(999);
        } catch (Exception error) {
            ok = false;
        }
        return ok;
    }

    private static ShortMessage nota(int comando, int canal, int dato1, int dato2)
            throws Exception {
        ShortMessage m = new ShortMessage();
        m.setMessage(comando, canal, dato1, dato2);
        return m;
    }

    private static int esperarNotas(Piano piano, long plazoMs) throws Exception {
        long limite = System.currentTimeMillis() + plazoMs;
        int n = 0;
        while (n == 0 && System.currentTimeMillis() < limite) {
            n = contar(piano);
            if (n == 0) {
                Thread.sleep(50);
            }
        }
        return n;
    }

    private static int contar(Piano piano) {
        int n = 0;
        for (int nota = 24; nota <= 108; nota++) {
            Tecla t = piano.getTecla(0, nota);
            if (t != null && t.estaPulsado()) {
                n++;
            }
        }
        return n;
    }
}
