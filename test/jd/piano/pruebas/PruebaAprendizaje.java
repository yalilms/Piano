package jd.piano.pruebas;

import jd.piano.programa.Bateria;
import jd.piano.programa.ModoAprendizaje;
import jd.piano.programa.NotaMidi;
import jd.piano.programa.Partitura;
import jd.piano.programa.PianoSencillo;
import jd.piano.programa.PiezasPercusion;
import jd.piano.programa.Reproductor;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** El modo aprendizaje y la percusion, que son las dos piezas nuevas. */
public final class PruebaAprendizaje {

    private PruebaAprendizaje() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("aprendizaje y percusion");
        File[] canciones = PruebaMidi.canciones();
        if (canciones.length == 0) {
            c.comprobar("hay canciones para probar", false);
            return c;
        }
        percusion(c, canciones);
        melodia(c, canciones);
        esperaYAvance(c, canciones[0].getPath());
        return c;
    }

    private static void percusion(Comprobador c, File[] canciones) {
        c.comprobar("el mapa de percusion cubre de la 27 a la 87",
                PiezasPercusion.esConocida(27) && PiezasPercusion.esConocida(87)
                        && !PiezasPercusion.esConocida(26)
                        && !PiezasPercusion.esConocida(88));
        c.comprobar("el 36 es el bombo y el 42 el charles cerrado",
                PiezasPercusion.nombre(36).toLowerCase().contains("bombo")
                        && PiezasPercusion.nombre(42).toLowerCase().contains("charles"));
        c.comprobar("una nota desconocida da un nombre de reserva",
                PiezasPercusion.nombre(200) != null
                        && !PiezasPercusion.nombre(200).isBlank());

        boolean todas = true;
        for (int n = PiezasPercusion.NOTA_MINIMA; n <= PiezasPercusion.NOTA_MAXIMA; n++) {
            todas = todas && PiezasPercusion.esConocida(n)
                    && !PiezasPercusion.nombre(n).isBlank();
        }
        c.comprobar("las 61 piezas tienen nombre", todas);

        // El bombo y la caja tienen que salir antes que lo accesorio
        c.comprobar("el orden pone la base del ritmo primero",
                PiezasPercusion.ordenDe(36) < PiezasPercusion.ordenDe(38)
                        && PiezasPercusion.ordenDe(38) < PiezasPercusion.ordenDe(82));

        Bateria bateria = new Bateria();
        bateria.setZona(new Rectangle(0, 0, 900, 0));
        c.comprobar("una bateria sin piezas esta vacia", bateria.estaVacia());
        bateria.construir(List.of(36, 38, 42, 46, 49));
        c.comprobar("monta un pad por pieza", bateria.getCuantosPads() == 5);
        c.comprobar("pide alto para sus filas", bateria.getAlturaNecesaria() > 0);

        // Golpear una pieza que no tiene pad no debe reventar
        bateria.golpear(99, 100);
        bateria.golpear(36, 100);
        c.comprobar("golpear no revienta con piezas fuera de la lista", true);

        // Y que la percusion de las canciones se detecte
        int conBateria = 0;
        for (File f : canciones) {
            try {
                if (jd.piano.programa.AnalisisMidi.de(f.getPath())
                        .getTotalPercusion() > 0) {
                    conBateria++;
                }
            } catch (Exception error) {
                // se cuenta como sin bateria
            }
        }
        c.dato(conBateria + " de " + canciones.length + " canciones tienen bateria");
        c.comprobar("se detecta la percusion de las canciones", conBateria > 0);
    }

    private static void melodia(Comprobador c, File[] canciones) {
        boolean todasDanCanal = true;
        for (File f : canciones) {
            try {
                Partitura p = Partitura.de(f.getPath());
                int canal = ModoAprendizaje.canalDeMelodia(p);
                boolean valido = canal >= 0 && canal < 16 && tieneNotas(p, canal);
                todasDanCanal = todasDanCanal && valido;
            } catch (Exception error) {
                todasDanCanal = false;
            }
        }
        c.comprobar("el canal de melodia elegido siempre tiene notas", todasDanCanal);
    }

    private static boolean tieneNotas(Partitura p, int canal) {
        boolean hay = false;
        for (NotaMidi n : p.getNotas()) {
            if (n.canal() == canal) {
                hay = true;
                break;
            }
        }
        return hay;
    }

    /**
     * La prueba de verdad: se activa el modo, se deja correr la cancion y tiene
     * que pararse sola. Luego se pulsan las notas que pide y tiene que seguir.
     */
    private static void esperaYAvance(Comprobador c, String cancion) {
        try {
            Partitura partitura = Partitura.de(cancion);
            Piano piano = new PianoSencillo(24, 108);
            piano.setPosicion(0, 0);
            Reproductor r = new Reproductor();
            r.conectar(piano);
            r.cargar(cancion);

            int canal = ModoAprendizaje.canalDeMelodia(partitura);
            ModoAprendizaje modo = new ModoAprendizaje(partitura, r, canal);
            c.dato("canal a practicar: " + canal + ", "
                    + modo.getCuantosGrupos() + " acordes");
            c.comprobar("agrupa las notas en acordes", modo.getCuantosGrupos() > 0);

            // Hay canciones donde el canal de la melodia no entra hasta el
            // segundo 22, asi que esperar desde el principio no sirve: se
            // comprueba que al activarse el modo se adelanta el solo
            long antes = r.getPosicionUs();
            modo.setActivo(true);
            c.comprobar("se puede activar", modo.isActivo());
            c.dato(String.format("al activarse salta de %.1f s a %.1f s",
                    antes / 1e6, r.getPosicionUs() / 1e6));
            c.comprobar("se adelanta hasta la primera nota que hay que tocar",
                    r.getPosicionUs() > 0);

            Graphics2D g = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
                    .createGraphics();
            r.reproducir();

            // Se deja correr hasta que el modo pare la cancion
            boolean paro = false;
            long limite = System.currentTimeMillis() + 12_000;
            while (!paro && System.currentTimeMillis() < limite) {
                modo.dibujar(g, piano);
                paro = modo.estaEsperando() && !r.estaSonando();
                if (!paro) {
                    Thread.sleep(20);
                }
            }
            c.comprobar("para la cancion al llegar a un acorde", paro);

            if (paro) {
                // Se pulsan las notas que pide, como haria el usuario
                List<Integer> pedidas = notasPedidas(partitura, r.getPosicionUs(), canal);
                c.dato("notas que pide: " + pedidas);
                for (int nota : pedidas) {
                    Tecla t = piano.getTecla(canal, nota);
                    if (t != null) {
                        t.pulsar(100);
                    }
                }
                boolean siguio = false;
                long limite2 = System.currentTimeMillis() + 4000;
                while (!siguio && System.currentTimeMillis() < limite2) {
                    modo.dibujar(g, piano);
                    siguio = !modo.estaEsperando() && r.estaSonando();
                    if (!siguio) {
                        Thread.sleep(20);
                    }
                }
                c.comprobar("sigue al tocar las notas que pedia", siguio);
            }

            modo.setActivo(false);
            c.comprobar("se puede desactivar", !modo.isActivo());
            g.dispose();
            r.cerrar();
        } catch (Exception error) {
            c.comprobar("el modo aprendizaje no lanza excepciones: " + error, false);
        }
    }

    /** Las notas del grupo que toca alrededor de esa posicion. */
    private static List<Integer> notasPedidas(Partitura p, long posicionUs, int canal) {
        List<Integer> notas = new ArrayList<>();
        long mejor = Long.MAX_VALUE;
        for (NotaMidi n : p.getNotas()) {
            if (n.canal() != canal) {
                continue;
            }
            long distancia = Math.abs(n.inicioUs() - posicionUs);
            if (distancia < mejor - 60_000) {
                mejor = distancia;
                notas.clear();
                notas.add(n.nota());
            } else if (Math.abs(distancia - mejor) <= 60_000 && !notas.contains(n.nota())) {
                notas.add(n.nota());
            }
        }
        return notas;
    }
}
