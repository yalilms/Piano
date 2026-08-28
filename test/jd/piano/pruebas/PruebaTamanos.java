package jd.piano.pruebas;

import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.programa.AnalisisMidi;
import jd.piano.programa.Bateria;
import jd.piano.programa.Cascada;
import jd.piano.programa.Conjunto;
import jd.piano.programa.Escena;
import jd.piano.programa.MultiPiano;
import jd.piano.programa.NotaMidi;
import jd.piano.programa.Particulas;
import jd.piano.programa.Partitura;
import jd.piano.programa.RastroLibre;
import jd.piano.programa.Reproductor;
import jd.piano.programa.TipoInstrumento;
import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Que cada instrumento quepa en el hueco que le dan, a cualquier tamano de
 * ventana.
 *
 * Esto no se comprobaba, y no se comprobaba nada: todo se habia mirado en una
 * ventana de 1351x673. Y habia un fallo de los gordos escondido ahi. ajustarA
 * recibe el hueco y promete que el instrumento cabe; el teclado se pasaba de
 * ancho en todos los tamanos, entre 8 y 16 px, y el multipiano hasta 40.
 *
 * La causa era una linea: MedidasTeclado redondeaba el ancho de tecla en vez de
 * truncarlo. Quien pide la escala la calcula para que el teclado quepa justo, y
 * al redondear hacia arriba cada tecla se pasaba hasta un pixel; con 52 blancas,
 * el teclado acababa hasta 52 px mas ancho de lo que cabia. Se centra, asi que
 * las teclas de los extremos salian cortadas.
 *
 * Los tamanos que se prueban empiezan en el lienzo que da la ventana mas
 * pequena que se puede poner, medido: la ventana resta 284 px de ancho y 39 de
 * alto para el marco y el panel lateral.
 */
public final class PruebaTamanos {

    /** El lienzo de la ventana minima (1000x520), y de ahi hacia arriba. */
    private static final int[][] LIENZOS = {
            {716, 481}, {760, 500}, {860, 540}, {1067, 634},
            {1400, 800}, {1920, 1040}, {2560, 1400},
    };

    private static final int MARGEN = 48;
    private static final int ALTO_TAPA_LIBRE = 46;
    private static final int NOTA_INICIAL = 21;
    private static final int NOTA_FINAL = 108;

    private PruebaTamanos() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("tamanos de ventana");
        File[] canciones = PruebaMidi.canciones();
        if (canciones.length == 0) {
            c.comprobar("hay canciones para probar", false);
            return c;
        }
        // La que mas canales y mas percusion tiene es la que aprieta
        File laPeor = laDeMasCanales(canciones);
        try {
            AnalisisMidi analisis = AnalisisMidi.de(laPeor.getPath());
            Partitura partitura = Partitura.de(laPeor.getPath());
            c.dato(laPeor.getName() + ": " + analisis.getCanalesUsados().size()
                    + " canales, " + analisis.getPercusionUsada().size()
                    + " piezas de percusion");

            contrato(c, analisis);
            cabeEnLaEscena(c, analisis, partitura);
            teclasSinCortar(c);
        } catch (Exception error) {
            c.comprobar("los tamanos no lanzan excepciones: " + error, false);
        }
        return c;
    }

    /** Un instrumento por cada tipo, mas el conjunto y el multipiano. */
    private static List<InstrumentoVisual> todos(AnalisisMidi analisis) {
        List<InstrumentoVisual> lista = new ArrayList<>();
        for (TipoInstrumento tipo : TipoInstrumento.values()) {
            lista.add(tipo.crear());
        }
        lista.add(Conjunto.para(analisis));
        List<Integer> canales = analisis.getCanalesUsados();
        lista.add(new MultiPiano(canales, NOTA_INICIAL, NOTA_FINAL,
                LayoutTeclados.calcular(canales.size(),
                        Piano.contarBlancas(NOTA_INICIAL, NOTA_FINAL), 1300, 700)));
        return lista;
    }

    /**
     * El contrato de ajustarA: lo que sale nunca puede ser mas grande que el
     * hueco que se le da.
     */
    private static void contrato(Comprobador c, AnalisisMidi analisis) {
        int incumplen = 0;
        int peorExceso = 0;
        String donde = "";

        for (int[] hueco : LIENZOS) {
            int ancho = hueco[0] - 2 * MARGEN;
            int alto = hueco[1] - 2 * MARGEN;
            for (InstrumentoVisual i : todos(analisis)) {
                i.ajustarA(ancho, alto);
                i.setPosicion(0, 0);
                int excesoAncho = i.getAnchura() - ancho;
                int excesoAlto = i.getAltura() - alto;
                int exceso = Math.max(excesoAncho, excesoAlto);
                if (exceso > 0) {
                    incumplen++;
                    if (exceso > peorExceso) {
                        peorExceso = exceso;
                        donde = i.getNombre() + " en " + ancho + "x" + alto
                                + " ocupa " + i.getAnchura() + "x" + i.getAltura();
                    }
                }
            }
        }
        if (incumplen > 0) {
            c.dato("se pasa hasta " + peorExceso + " px: " + donde);
        }
        c.comprobar("ningun instrumento se pasa del hueco que le dan",
                incumplen == 0);
    }

    /**
     * Y colocado como lo coloca la ventana, que quepa dentro de la escena y no
     * se meta debajo de los parches de bateria.
     */
    private static void cabeEnLaEscena(Comprobador c, AnalisisMidi analisis,
                                       Partitura partitura) {
        int seSalen = 0;
        int debajoDeLaBateria = 0;
        String donde = "";
        String bajoLosParches = "";

        for (int[] lienzo : LIENZOS) {
            for (InstrumentoVisual instrumento : todos(analisis)) {
                Rectangle limites = colocarComoLaVentana(instrumento, analisis,
                        partitura, lienzo[0], lienzo[1], true);
                if (!new Rectangle(0, 0, lienzo[0], lienzo[1]).contains(limites)) {
                    seSalen++;
                    donde = instrumento.getNombre() + " en "
                            + lienzo[0] + "x" + lienzo[1] + ": " + limites;
                }
                // Los centrados son los que se metian debajo de los parches:
                // los que llevan cascada van pegados abajo y no llegan
                if (!instrumento.admiteCascada()
                        && limites.y < franjaDeArriba(analisis, lienzo[0]) - 4) {
                    debajoDeLaBateria++;
                    bajoLosParches = instrumento.getNombre() + " en "
                            + lienzo[0] + "x" + lienzo[1] + ": empieza en y="
                            + limites.y + " y la franja acaba en "
                            + franjaDeArriba(analisis, lienzo[0]);
                }
            }
        }
        if (seSalen > 0) {
            c.dato("se sale: " + donde);
        }
        if (debajoDeLaBateria > 0) {
            c.dato(bajoLosParches);
        }
        c.comprobar("ningun instrumento se sale de la escena", seSalen == 0);
        c.comprobar("ninguno se mete debajo de los parches de bateria",
                debajoDeLaBateria == 0);

        // Y que pintar no lance nada, con las capas montadas y notas encendidas
        boolean sinExcepciones = true;
        String cual = "";
        for (int[] lienzo : LIENZOS) {
            for (InstrumentoVisual instrumento : todos(analisis)) {
                try {
                    dibujarDeVerdad(instrumento, analisis, partitura,
                            lienzo[0], lienzo[1]);
                } catch (Throwable error) {
                    sinExcepciones = false;
                    cual = instrumento.getNombre() + " en " + lienzo[0] + "x"
                            + lienzo[1] + ": " + error;
                }
            }
        }
        if (!sinExcepciones) {
            c.dato(cual);
        }
        c.comprobar("pintar no lanza nada a ningun tamano", sinExcepciones);
    }

    /**
     * Donde acaba la franja de los parches de bateria. Por debajo de ahi empieza
     * el sitio libre para el instrumento.
     */
    private static int franjaDeArriba(AnalisisMidi analisis, int ancho) {
        Bateria bateria = new Bateria();
        bateria.construir(analisis.getPercusionUsada());
        int arriba = MARGEN / 2;
        if (!bateria.estaVacia()) {
            bateria.setZona(new Rectangle(arriba, arriba, ancho - 2 * arriba, 0));
            arriba += bateria.getAlturaNecesaria() + MARGEN / 3;
        }
        return arriba;
    }

    /** La misma colocacion que hace recolocar() en la ventana. */
    private static Rectangle colocarComoLaVentana(InstrumentoVisual instrumento,
                                                  AnalisisMidi analisis,
                                                  Partitura partitura,
                                                  int ancho, int alto,
                                                  boolean conBateria) {
        int arriba = conBateria ? franjaDeArriba(analisis, ancho) : MARGEN / 2;
        int y;
        if (instrumento.admiteCascada()) {
            instrumento.ajustarA(ancho - 2 * MARGEN, alto - 2 * MARGEN);
            y = alto - instrumento.getAltura() - ALTO_TAPA_LIBRE;
        } else {
            int libre = alto - arriba - MARGEN / 2;
            instrumento.ajustarA(ancho - 2 * MARGEN, libre);
            y = arriba + (libre - instrumento.getAltura()) / 2;
        }
        instrumento.setPosicion((ancho - instrumento.getAnchura()) / 2, y);
        return instrumento.getLimites();
    }

    /** Monta la escena entera y pinta dos frames, como la ventana de verdad. */
    private static void dibujarDeVerdad(InstrumentoVisual instrumento,
                                        AnalisisMidi analisis,
                                        Partitura partitura,
                                        int ancho, int alto) {
        Escena escena = new Escena(ancho, alto);
        escena.setInstrumento(instrumento);
        Reproductor reproductor = new Reproductor();
        reproductor.conectar(instrumento);
        try {
            Rectangle limites = colocarComoLaVentana(instrumento, analisis,
                    partitura, ancho, alto, true);
            if (instrumento.admiteCascada()) {
                Cascada cascada = new Cascada(partitura, reproductor);
                cascada.setZona(MARGEN / 2, limites.y - 4 - MARGEN / 2);
                escena.anadirSuperpuesta(cascada);
            } else {
                RastroLibre rastro = new RastroLibre();
                rastro.setYTope(MARGEN / 2);
                escena.anadirSuperpuesta(rastro);
            }
            escena.anadirSuperpuesta(new Particulas());
            escena.prepararCaches();

            for (NotaMidi n : partitura.getNotas()) {
                if (n.inicioUs() > 30_000_000L) {
                    break;
                }
                if (n.inicioUs() > 28_000_000L) {
                    instrumento.pulsar(n.canal(), n.nota(), n.velocidad());
                }
            }

            BufferedImage img = new BufferedImage(ancho, alto,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            escena.dibujar(g);
            escena.dibujar(g);
            g.dispose();
        } finally {
            reproductor.cerrar();
        }
    }

    /**
     * El ancho de tecla se trunca, no se redondea. Es la linea que causaba que
     * el teclado se pasara del hueco, asi que conviene vigilarla aparte.
     */
    private static void teclasSinCortar(Comprobador c) {
        int blancas = Piano.contarBlancas(NOTA_INICIAL, NOTA_FINAL);
        int seSalen = 0;
        for (int disponible = 220; disponible <= 1600; disponible += 7) {
            MedidasTeclado medidas = LayoutTeclados.calcular(1, blancas,
                    disponible, 400).getMedidas();
            if (medidas.anchuraDe(blancas) > disponible) {
                seSalen++;
            }
        }
        c.dato("anchos probados de 220 a 1600 px con " + blancas + " blancas");
        c.comprobar("el teclado nunca se pasa del ancho disponible", seSalen == 0);

        // Y que las escalas exactas no se caigan al entero de abajo por la coma
        // flotante: 25 * 0,6 da 14,999999999999998
        c.comprobar("las escalas exactas dan el ancho de tecla exacto",
                new MedidasTeclado(1.0).getAnchuraBlanca() == 25
                        && new MedidasTeclado(0.8).getAnchuraBlanca() == 20
                        && new MedidasTeclado(0.6).getAnchuraBlanca() == 15
                        && new MedidasTeclado(0.4).getAnchuraBlanca() == 10
                        && new MedidasTeclado(0.2).getAnchuraBlanca() == 5);
    }

    private static File laDeMasCanales(File[] canciones) {
        File mejor = canciones[0];
        int mas = -1;
        for (File f : canciones) {
            try {
                AnalisisMidi a = AnalisisMidi.de(f.getPath());
                int cuantos = a.getCanalesUsados().size()
                        + a.getPercusionUsada().size();
                if (cuantos > mas) {
                    mas = cuantos;
                    mejor = f;
                }
            } catch (Exception error) {
                // esa no cuenta
            }
        }
        return mejor;
    }
}
