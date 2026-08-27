package jd.piano.pruebas;

import jd.piano.programa.PianoSencillo;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Tecla;

import java.awt.geom.Area;

/**
 * Las teclas tienen que encajar exactas: ni pisarse ni dejar huecos, a
 * cualquier escala.
 *
 * Este es el invariante que se rompio una vez de verdad: TeclaBlanca2 y 3
 * enumeraban vertices a mano y una coordenada mal metia una cuna blanca sobre
 * la tecla negra vecina. Ahora las formas se construyen restando areas, pero
 * conviene seguir vigilandolo.
 */
public final class PruebaGeometria {

    private static final int NOTA_MINIMA = 21;
    private static final int NOTA_MAXIMA = 108;

    private PruebaGeometria() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("geometria de las teclas");
        encaje(c);
        hitTesting(c);
        return c;
    }

    private static void encaje(Comprobador c) {
        double[] escalas = {1.0, 0.8, 0.6, 0.45, 0.32, 0.24, 0.16};
        boolean todoBien = true;

        for (double escala : escalas) {
            MedidasTeclado medidas = new MedidasTeclado(escala);
            PianoSencillo piano = new PianoSencillo(NOTA_MINIMA, NOTA_MAXIMA, medidas);
            piano.setPosicion(0, 0);

            boolean solapan = false;
            for (int a = NOTA_MINIMA; a <= NOTA_MAXIMA && !solapan; a++) {
                for (int b = a + 1; b <= Math.min(a + 2, NOTA_MAXIMA) && !solapan; b++) {
                    Area corte = new Area(piano.getTecla(0, a).getForma());
                    corte.intersect(new Area(piano.getTecla(0, b).getForma()));
                    solapan = !corte.isEmpty();
                }
            }

            // Sin huecos: la union de todas debe medir lo que mide el teclado
            Area union = new Area();
            for (int n = NOTA_MINIMA; n <= NOTA_MAXIMA; n++) {
                union.add(new Area(piano.getTecla(0, n).getForma()));
            }
            int anchoUnion = (int) Math.round(union.getBounds2D().getWidth());
            boolean sinHuecos = anchoUnion == piano.getAnchura();

            c.dato(String.format("escala %.2f: blanca %d px, negra %d px, ancho %d px",
                    medidas.getEscala(), medidas.getAnchuraBlanca(),
                    medidas.getAnchuraNegra(), piano.getAnchura()));
            todoBien = todoBien && !solapan && sinHuecos;
        }
        c.comprobar("ninguna tecla pisa a otra, de escala 1,00 a 0,16", todoBien);
        c.comprobar("la union de las teclas mide lo que el teclado", todoBien);
    }

    /**
     * Se barre pixel a pixel toda la superficie del teclado y se comprueba que
     * teclaEn devuelve siempre la tecla que de verdad contiene el punto.
     */
    private static void hitTesting(Comprobador c) {
        PianoSencillo piano = new PianoSencillo(NOTA_MINIMA, NOTA_MAXIMA);
        int origen = 40;
        piano.setPosicion(origen, origen);

        int dentro = 0;
        int equivocadas = 0;
        int perdidos = 0;
        for (int y = origen; y < origen + piano.getAltura(); y++) {
            for (int x = origen; x < origen + piano.getAnchura(); x++) {
                Tecla real = laQueContiene(piano, x, y);
                if (real == null) {
                    continue;
                }
                dentro++;
                Tecla devuelta = piano.teclaEn(x, y);
                if (devuelta == null) {
                    perdidos++;
                } else if (devuelta != real) {
                    equivocadas++;
                }
            }
        }
        c.dato("puntos barridos dentro de alguna tecla: " + dentro);
        c.comprobar("ningun punto devuelve la tecla equivocada", equivocadas == 0);
        c.comprobar("ningun punto dentro de una tecla se pierde", perdidos == 0);
        c.comprobar("fuera del teclado no devuelve nada",
                piano.teclaEn(0, 0) == null && piano.teclaEn(9999, 9999) == null);
    }

    private static Tecla laQueContiene(PianoSencillo piano, int x, int y) {
        Tecla encontrada = null;
        for (int n = NOTA_MINIMA; n <= NOTA_MAXIMA && encontrada == null; n++) {
            Tecla t = piano.getTecla(0, n);
            if (t != null && t.getForma().contains(x, y)) {
                encontrada = t;
            }
        }
        return encontrada;
    }
}
