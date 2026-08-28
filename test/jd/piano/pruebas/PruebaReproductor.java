package jd.piano.pruebas;

import jd.piano.programa.Bateria;
import jd.piano.programa.MultiPiano;
import jd.piano.programa.Partitura;
import jd.piano.programa.PianoSencillo;
import jd.piano.programa.Reproductor;
import jd.piano.programa.TipoInstrumento;
import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import javax.sound.midi.ShortMessage;
import java.io.File;
import java.util.List;

/**
 * El transporte, el silenciado por canal, la transposicion y el bucle.
 */
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
        tono(c);
        bucle(c, canciones[0].getPath());
        cuantoGanaTransponer(c);
        return c;
    }

    /**
     * La transposicion. Lo que hay que vigilar es que no queden notas colgadas:
     * una nota se enciende con un tono y se apaga con otro si el usuario mueve
     * el control mientras suena, y entonces su apagado no cuadra con nada.
     */
    private static void tono(Comprobador c) {
        Piano piano = new PianoSencillo(21, 108);
        piano.setPosicion(0, 0);
        Reproductor r = new Reproductor();
        r.conectar(piano);
        try {
            c.comprobar("empieza en el tono original", r.getTransposicion() == 0);

            r.setTransposicion(99);
            boolean topeArriba = r.getTransposicion() == Reproductor.TONO_MAXIMO;
            r.setTransposicion(-99);
            boolean topeAbajo = r.getTransposicion() == -Reproductor.TONO_MAXIMO;
            c.comprobar("el tono tiene tope por los dos lados",
                    topeArriba && topeAbajo);

            r.setTransposicion(3);
            c.comprobar("notaMostrada devuelve la nota transpuesta",
                    r.notaMostrada(60) == 63);

            r.send(nota(ShortMessage.NOTE_ON, 0, 60, 100), -1);
            c.comprobar("la nota se enciende tres semitonos mas arriba",
                    piano.getTecla(0, 63).estaPulsado()
                            && !piano.getTecla(0, 60).estaPulsado());

            // El apagado llega con la nota original y tiene que apagar la
            // transpuesta, no dejarla encendida
            r.send(nota(ShortMessage.NOTE_OFF, 0, 60, 0), -1);
            c.comprobar("el apagado apaga la tecla transpuesta",
                    contar(piano) == 0);

            // Mover el control con una nota sonando: se corta todo antes
            r.setTransposicion(0);
            r.send(nota(ShortMessage.NOTE_ON, 0, 60, 100), -1);
            r.setTransposicion(5);
            c.comprobar("cambiar el tono apaga lo que estuviera sonando",
                    contar(piano) == 0);
            r.send(nota(ShortMessage.NOTE_OFF, 0, 60, 0), -1);
            c.comprobar("el apagado que llega despues no enciende nada",
                    contar(piano) == 0);

            // Lo que se sale de las 128 notas del MIDI se descarta entero
            r.setTransposicion(12);
            r.send(nota(ShortMessage.NOTE_ON, 0, 120, 100), -1);
            c.comprobar("una nota que se sale del MIDI no se pinta",
                    contar(piano) == 0);
            r.send(nota(ShortMessage.NOTE_OFF, 0, 120, 0), -1);
            c.comprobar("y su apagado tampoco revienta", contar(piano) == 0);

            // La percusion no se transpone: la nota es una pieza, no una altura
            Bateria bateria = new Bateria();
            bateria.construir(List.of(36, 38, 42));
            r.setBateria(bateria);
            r.setTransposicion(3);
            r.send(nota(ShortMessage.NOTE_ON, 9, 36, 100), -1);
            c.comprobar("la percusion no se transpone",
                    bateria.estaGolpeado(36) && !bateria.estaGolpeado(39));
        } catch (Exception error) {
            c.comprobar("el tono no lanza excepciones: " + error, false);
        }
        r.cerrar();
    }

    /**
     * El bucle para practicar un trozo: el mismo boton pone la A, luego la B y
     * luego lo quita.
     */
    private static void bucle(Comprobador c, String cancion) {
        Reproductor r = new Reproductor();
        try {
            r.cargar(cancion);
            c.comprobar("empieza sin bucle",
                    !r.tieneBucle() && !r.esperaFinDeBucle());

            r.buscar(2_000_000);
            r.marcarBucle();
            c.comprobar("la primera marca pone la A y espera la B",
                    r.esperaFinDeBucle() && !r.tieneBucle());

            r.buscar(6_000_000);
            r.marcarBucle();
            c.dato(String.format("bucle marcado: %.1f s a %.1f s",
                    r.getBucleInicioUs() / 1e6, r.getBucleFinUs() / 1e6));
            c.comprobar("la segunda marca cierra el bucle",
                    r.tieneBucle() && !r.esperaFinDeBucle()
                            && r.getBucleFinUs() > r.getBucleInicioUs());

            r.marcarBucle();
            c.comprobar("la tercera marca lo quita",
                    !r.tieneBucle() && !r.esperaFinDeBucle());

            // Una B pegada a la A no da un bucle utilizable: se recoloca la A
            r.buscar(2_000_000);
            r.marcarBucle();
            r.buscar(2_100_000);
            r.marcarBucle();
            c.comprobar("una B demasiado cerca recoloca la A en vez de cerrar",
                    r.esperaFinDeBucle() && !r.tieneBucle());

            // Y que de verdad vuelva atras al pasarse del final
            r.quitarBucle();
            r.buscar(2_000_000);
            r.marcarBucle();
            r.buscar(6_000_000);
            r.marcarBucle();

            r.buscar(8_000_000);
            c.comprobar("en pausa no vuelve atras, para poder moverse a mano",
                    posicionTrasAtender(r) > 7_000_000);

            r.reproducir();
            r.buscar(8_000_000);
            r.atenderBucle();
            long vuelta = r.getPosicionUs();
            r.pausar();
            c.dato("tras pasarse del final vuelve a " + vuelta / 1000 + " ms");
            c.comprobar("sonando, al pasar del final vuelve al principio",
                    vuelta < 4_000_000);

            r.cargar(cancion);
            c.comprobar("cargar otra cancion limpia el bucle", !r.tieneBucle());
        } catch (Exception error) {
            c.comprobar("el bucle no lanza excepciones: " + error, false);
        }
        r.cerrar();
    }

    private static long posicionTrasAtender(Reproductor r) {
        r.atenderBucle();
        return r.getPosicionUs();
    }

    /**
     * Cuanto se gana de verdad transponiendo. No es un adorno: la guitarra deja
     * fuera las notas graves, y subir la cancion mete parte de ellas en su
     * rango. Aqui se mide en las canciones reales.
     */
    private static void cuantoGanaTransponer(Comprobador c) {
        File[] canciones = PruebaMidi.canciones();
        double mejorGanancia = 0;
        String donde = "";
        boolean nuncaEmpeoraElMejor = true;

        for (File f : canciones) {
            try {
                Partitura p = Partitura.de(f.getPath());
                double sinTocar = TipoInstrumento.GUITARRA.coberturaDe(p, 0);
                double mejor = sinTocar;
                int mejorTono = 0;
                for (int t = -Reproductor.TONO_MAXIMO; t <= Reproductor.TONO_MAXIMO; t++) {
                    double cobertura = TipoInstrumento.GUITARRA.coberturaDe(p, t);
                    if (cobertura > mejor) {
                        mejor = cobertura;
                        mejorTono = t;
                    }
                }
                if (mejor - sinTocar > mejorGanancia) {
                    mejorGanancia = mejor - sinTocar;
                    donde = String.format("%s: %.0f%% -> %.0f%% con %+d semitonos",
                            f.getName(), sinTocar, mejor, mejorTono);
                }
                nuncaEmpeoraElMejor = nuncaEmpeoraElMejor && mejor >= sinTocar;
            } catch (Exception error) {
                nuncaEmpeoraElMejor = false;
            }
        }
        c.dato("lo que mas gana en la guitarra -> " + donde);
        c.comprobar("transponer mejora la cobertura de alguna cancion",
                mejorGanancia > 5);
        c.comprobar("el mejor tono nunca es peor que el original",
                nuncaEmpeoraElMejor);

        // Y el tono original siempre tiene que dar lo mismo que sin transponer
        boolean coincide = true;
        for (File f : canciones) {
            try {
                Partitura p = Partitura.de(f.getPath());
                coincide = coincide && TipoInstrumento.TECLADO.coberturaDe(p)
                        == TipoInstrumento.TECLADO.coberturaDe(p, 0);
            } catch (Exception error) {
                coincide = false;
            }
        }
        c.comprobar("sin transponer, la cobertura es la de siempre", coincide);
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
            c.comprobar("buscar en pausa mueve la posicion",
                    Math.abs(r.getPosicionUs() - r.getDuracionUs() / 2) < 600_000);

            // El caso que se colaba: buscar MIENTRAS suena. El secuenciador de
            // Java ignora el cambio de posicion si esta en marcha, asi que hay
            // que pararlo y volver a arrancarlo.
            r.reproducir();
            Thread.sleep(200);
            long destino = r.getDuracionUs() / 4;
            r.buscar(destino);
            c.comprobar("buscar sonando mueve la posicion",
                    Math.abs(r.getPosicionUs() - destino) < 600_000);
            c.comprobar("y sigue sonando despues de buscar", r.estaSonando());
            r.pausar();
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
