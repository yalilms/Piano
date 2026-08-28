package jd.piano.pruebas;

import jd.piano.programa.MultiPiano;
import jd.piano.programa.PianoSencillo;
import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.List;

/**
 * Las teclas tienen que encajar exactas: ni pisarse ni dejar huecos, a
 * cualquier escala.
 *
 * Aqui se han roto de verdad dos invariantes distintos, y por eso se vigilan
 * los dos. El primero: TeclaBlanca2 y 3 enumeraban vertices a mano y una
 * coordenada mal metia una cuna blanca sobre la tecla negra vecina. El segundo:
 * al pasar a restar areas, el hueco de la negra vecina se restaba mirando solo
 * la nota, asi que en los extremos del teclado se restaba un hueco que ninguna
 * negra iba a llenar y quedaba un mordisco en la esquina.
 */
public final class PruebaGeometria {

    private static final int NOTA_MINIMA = 21;
    private static final int NOTA_MAXIMA = 108;

    private PruebaGeometria() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("geometria de las teclas");
        encaje(c);
        extremos(c);
        hitTesting(c);
        return c;
    }

    /**
     * La fila de arriba del teclado tiene que estar cubierta de punta a punta.
     *
     * Este es el segundo fallo de geometria que se ha colado de verdad. La forma
     * de una blanca se hace restando el hueco de sus negras vecinas, y eso se
     * decidia solo por la nota: un Do siempre restaba el hueco del Do#. En el
     * ultimo Do de un piano no hay Do#, asi que se restaba un hueco que nadie
     * llenaba y quedaba un mordisco en la esquina. Eran 15 pixeles de la fila de
     * arriba sin tecla, en los dos extremos.
     */
    private static void extremos(Comprobador c) {
        double[] escalas = {1.0, 0.7, 0.45, 0.28, 0.16};
        int peorHueco = 0;
        String donde = "";

        for (double escala : escalas) {
            MedidasTeclado medidas = new MedidasTeclado(escala);
            PianoSencillo piano = new PianoSencillo(NOTA_MINIMA, NOTA_MAXIMA, medidas);
            piano.setPosicion(0, 0);
            int hueco = huecosArriba(piano, NOTA_MINIMA, NOTA_MAXIMA);
            if (hueco > peorHueco) {
                peorHueco = hueco;
                donde = String.format("escala %.2f", escala);
            }
        }
        c.dato(peorHueco == 0 ? "ninguna escala deja hueco arriba"
                : peorHueco + " px sin tecla en " + donde);
        c.comprobar("la fila de arriba esta cubierta a cualquier escala",
                peorHueco == 0);

        // Y en los extremos, la tecla tiene que estar entera: ni la primera ni
        // la ultima llevan muesca, porque su negra vecina no existe
        PianoSencillo piano = new PianoSencillo(NOTA_MINIMA, NOTA_MAXIMA);
        piano.setPosicion(0, 0);
        Tecla primera = piano.getTecla(0, NOTA_MINIMA);
        Tecla ultima = piano.getTecla(0, NOTA_MAXIMA);
        c.comprobar("la primera tecla llega entera hasta su esquina",
                primera.getForma().contains(1, 1));
        c.comprobar("la ultima tecla llega entera hasta su esquina",
                ultima.getForma().contains(piano.getAnchura() - 2, 1));

        // Un teclado que empieza y acaba en negra: ahi si hay que recortar,
        // porque las vecinas de dentro existen
        PianoSencillo trozo = new PianoSencillo(60, 72);
        trozo.setPosicion(0, 0);
        c.comprobar("un trozo suelto tampoco deja hueco arriba",
                huecosArriba(trozo, 60, 72) == 0);

        // Con un solo teclado por canal el fallo se veia cuatro veces
        List<Integer> canales = List.of(0, 1, 2, 3);
        MultiPiano multi = new MultiPiano(canales, NOTA_MINIMA, NOTA_MAXIMA,
                LayoutTeclados.calcular(canales.size(),
                        Piano.contarBlancas(NOTA_MINIMA, NOTA_MAXIMA), 1300, 700));
        multi.setPosicion(0, 0);
        boolean sinHuecos = true;
        for (int canal : canales) {
            Rectangle caja = multi.getTecla(canal, NOTA_MINIMA).getForma().getBounds();
            int y = caja.y + 1;
            for (int x = caja.x; x < caja.x + anchoDeLaFila(multi, canal); x++) {
                sinHuecos = sinHuecos && contieneAlguna(multi, canal, x, y);
            }
        }
        c.comprobar("ningun teclado del multipiano deja hueco arriba", sinHuecos);
    }

    /** Pixeles de la fila de arriba que no pertenecen a ninguna tecla. */
    private static int huecosArriba(PianoSencillo piano, int minima, int maxima) {
        int sinTecla = 0;
        for (int x = 0; x < piano.getAnchura(); x++) {
            boolean hay = false;
            for (int n = minima; n <= maxima && !hay; n++) {
                Tecla t = piano.getTecla(0, n);
                hay = t != null && t.getForma().contains(x, 1);
            }
            if (!hay) {
                sinTecla++;
            }
        }
        return sinTecla;
    }

    private static int anchoDeLaFila(MultiPiano multi, int canal) {
        Rectangle primera = multi.getTecla(canal, NOTA_MINIMA).getForma().getBounds();
        Rectangle ultima = multi.getTecla(canal, NOTA_MAXIMA).getForma().getBounds();
        return ultima.x + ultima.width - primera.x;
    }

    private static boolean contieneAlguna(MultiPiano multi, int canal,
                                          int x, int y) {
        for (int n = NOTA_MINIMA; n <= NOTA_MAXIMA; n++) {
            Tecla t = multi.getTecla(canal, n);
            if (t != null && t.getForma().contains(x, y)) {
                return true;
            }
        }
        return false;
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
