package jd.piano.instrumentos;

import jd.piano.teclas.Estilo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Un mastil de cuerda: guitarra o bajo. Cambia la afinacion y el numero de
 * cuerdas, el resto es igual.
 *
 * Lo interesante es que una nota se puede tocar en varias posiciones: el Mi de
 * la cuarta cuerda es el mismo que en la quinta con cinco trastes. Se elige
 * siempre la del traste mas bajo, que es la que queda cerca de la cejuela y la
 * mas legible.
 *
 * Los trastes se separan como en una guitarra de verdad, cada vez menos
 * conforme se sube: la distancia del traste n es 1 - 2^(-n/12).
 */
public class Mastil implements InstrumentoVisual {

    /** Mi, La, Re, Sol, Si, Mi. */
    public static final int[] AFINACION_GUITARRA = {40, 45, 50, 55, 59, 64};

    /** Mi, La, Re, Sol, una octava por debajo. */
    public static final int[] AFINACION_BAJO = {28, 33, 38, 43};

    private static final int TRASTES = 15;
    private static final int[] MARCAS = {3, 5, 7, 9, 12, 15};

    private static final int SEPARACION_CUERDAS_BASE = 22;
    private static final int MARGEN_IZQUIERDO = 30;
    private static final int MARGEN_DERECHO = 10;
    private static final int ALTO_NUMEROS = 16;

    private static final Color MADERA_ALTA = new Color(74, 46, 28);
    private static final Color MADERA_BAJA = new Color(44, 26, 15);
    private static final Color TRASTE_METAL = new Color(176, 172, 164);
    private static final Color CEJUELA = new Color(232, 226, 210);
    private static final Color MARCA = new Color(214, 206, 188, 120);
    private static final Color CUERDA = new Color(198, 192, 178);
    private static final Color NUMERO = new Color(178, 162, 140);

    private final String nombre;
    private final int[] afinacion;
    /** puntos[cuerda][traste] */
    private final Punto[][] puntos;

    private Point posicion;
    private int separacionCuerdas;
    private int anchoMastil;

    public Mastil(String nombre, int[] afinacion) {
        this.nombre = nombre;
        this.afinacion = afinacion.clone();
        this.posicion = null;
        this.separacionCuerdas = SEPARACION_CUERDAS_BASE;
        this.anchoMastil = 520;
        this.puntos = new Punto[afinacion.length][TRASTES + 1];
        for (int cuerda = 0; cuerda < afinacion.length; cuerda++) {
            for (int traste = 0; traste <= TRASTES; traste++) {
                this.puntos[cuerda][traste] = new Punto(this.afinacion[cuerda] + traste);
            }
        }
    }

    public static Mastil guitarra() {
        return new Mastil("Guitarra", AFINACION_GUITARRA);
    }

    public static Mastil bajo() {
        return new Mastil("Bajo", AFINACION_BAJO);
    }

    @Override
    public String getNombre() {
        return this.nombre;
    }

    public int getCuerdas() {
        return this.afinacion.length;
    }

    /** La nota mas grave y la mas aguda que llega a representar. */
    public int getNotaMinima() {
        return this.afinacion[0];
    }

    public int getNotaMaxima() {
        return this.afinacion[this.afinacion.length - 1] + TRASTES;
    }

    // ---------- posiciones ----------

    /**
     * Cuerda y traste donde se toca esa nota, o null si no llega.
     * Se busca el traste mas bajo posible, que es la posicion natural.
     */
    private int[] posicionDe(int nota) {
        int mejorCuerda = -1;
        int mejorTraste = Integer.MAX_VALUE;
        for (int cuerda = 0; cuerda < this.afinacion.length; cuerda++) {
            int traste = nota - this.afinacion[cuerda];
            if (traste >= 0 && traste <= TRASTES && traste < mejorTraste) {
                mejorTraste = traste;
                mejorCuerda = cuerda;
            }
        }
        return mejorCuerda < 0 ? null : new int[]{mejorCuerda, mejorTraste};
    }

    private Punto puntoDe(int nota) {
        int[] donde = posicionDe(nota);
        return donde == null ? null : this.puntos[donde[0]][donde[1]];
    }

    @Override
    public PiezaPulsable piezaDe(int canal, int nota) {
        // Un mastil no distingue canales: todos comparten las mismas posiciones
        return puntoDe(nota);
    }

    @Override
    public void soltarTodas() {
        for (Punto[] cuerda : this.puntos) {
            for (Punto punto : cuerda) {
                if (punto.estaPulsado()) {
                    punto.soltar();
                }
            }
        }
    }

    @Override
    public void soltarCanal(int canal) {
        soltarTodas();
    }

    @Override
    public PiezaPulsable piezaEn(int x, int y) {
        if (this.posicion == null) {
            return null;
        }
        for (Punto[] cuerda : this.puntos) {
            for (Punto punto : cuerda) {
                if (punto.getForma().contains(x, y)) {
                    return punto;
                }
            }
        }
        return null;
    }

    // ---------- colocacion ----------

    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
        colocarPuntos();
    }

    @Override
    public Rectangle getLimites() {
        return this.posicion == null ? null
                : new Rectangle(this.posicion.x, this.posicion.y,
                        getAnchura(), getAltura());
    }

    @Override
    public int getAnchura() {
        return MARGEN_IZQUIERDO + this.anchoMastil + MARGEN_DERECHO;
    }

    @Override
    public int getAltura() {
        return this.separacionCuerdas * (this.afinacion.length + 1) + ALTO_NUMEROS;
    }

    /**
     * El minimo es pequeno a proposito: ajustarA promete que el instrumento
     * cabe en el hueco, y un tope alto lo incumplia. Un mastil de 60 px es poco
     * util, pero salirse del lienzo y aparecer cortado es peor.
     */
    private static final int ANCHO_MASTIL_MINIMO = 60;
    private static final int SEPARACION_CUERDAS_MINIMA = 4;

    @Override
    public void ajustarA(int ancho, int alto) {
        this.anchoMastil = Math.max(ANCHO_MASTIL_MINIMO,
                ancho - MARGEN_IZQUIERDO - MARGEN_DERECHO);
        int cabe = (alto - ALTO_NUMEROS) / (this.afinacion.length + 1);
        this.separacionCuerdas = Math.max(SEPARACION_CUERDAS_MINIMA,
                Math.min(SEPARACION_CUERDAS_BASE, cabe));
        colocarPuntos();
    }

    /** Distancia relativa de un traste a la cejuela, como en un mastil real. */
    private double fraccionDelTraste(int traste) {
        double total = 1 - Math.pow(2, -TRASTES / 12.0);
        return (1 - Math.pow(2, -traste / 12.0)) / total;
    }

    private int xDelTraste(int traste) {
        return (int) Math.round(this.posicion.x + MARGEN_IZQUIERDO
                + fraccionDelTraste(traste) * this.anchoMastil);
    }

    private int yDeLaCuerda(int cuerda) {
        // La cuerda 0 es la mas grave y se dibuja abajo, como al mirar el mastil
        int desdeArriba = this.afinacion.length - 1 - cuerda;
        return this.posicion.y + this.separacionCuerdas * (desdeArriba + 1);
    }

    private void colocarPuntos() {
        if (this.posicion == null) {
            return;
        }
        int radio = Math.max(3, (int) Math.round(this.separacionCuerdas * 0.40));
        for (int cuerda = 0; cuerda < this.afinacion.length; cuerda++) {
            for (int traste = 0; traste <= TRASTES; traste++) {
                // El punto va entre el traste y el anterior, que es donde se
                // pisa la cuerda de verdad; el traste 0 va sobre la cejuela
                int x = traste == 0 ? xDelTraste(0)
                        : (xDelTraste(traste) + xDelTraste(traste - 1)) / 2;
                Punto punto = this.puntos[cuerda][traste];
                punto.setRadio(radio);
                punto.setPosicion(x - radio, yDeLaCuerda(cuerda) - radio);
            }
        }
    }

    // ---------- dibujado ----------

    @Override
    public void dibujar(Graphics2D g) {
        dibujarMastil(g);
        for (Punto[] cuerda : this.puntos) {
            for (Punto punto : cuerda) {
                punto.dibujar(g);
            }
        }
    }

    @Override
    public void dibujarEnReposo(Graphics2D g) {
        // Los puntos apagados no se ven, asi que en reposo es solo el mastil
        dibujarMastil(g);
    }

    @Override
    public void dibujarBrillos(Graphics2D g) {
        for (Punto[] cuerda : this.puntos) {
            for (Punto punto : cuerda) {
                punto.dibujarBrillo(g);
            }
        }
    }

    @Override
    public void anadirRegionesActivas(List<Rectangle> destino) {
        for (Punto[] cuerda : this.puntos) {
            for (Punto punto : cuerda) {
                if (punto.getIntensidad() > 0) {
                    destino.add(new Rectangle(punto.getCajaRepintado()));
                }
            }
        }
    }

    private void dibujarMastil(Graphics2D g) {
        if (this.posicion == null) {
            return;
        }
        int arriba = yDeLaCuerda(this.afinacion.length - 1) - this.separacionCuerdas / 2;
        int abajo = yDeLaCuerda(0) + this.separacionCuerdas / 2;
        int alto = abajo - arriba;
        int x0 = xDelTraste(0);
        int x1 = xDelTraste(TRASTES);

        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, arriba), new Point2D.Float(0, abajo),
                new float[]{0f, 1f}, new Color[]{MADERA_ALTA, MADERA_BAJA}));
        g.fill(new RoundRectangle2D.Double(x0, arriba, x1 - x0, alto, 5, 5));

        // Marcas de posicion, antes de los trastes para que queden debajo
        g.setColor(MARCA);
        for (int traste : MARCAS) {
            if (traste > TRASTES) {
                continue;
            }
            int cx = (xDelTraste(traste) + xDelTraste(traste - 1)) / 2;
            double radio = Math.max(2.5, this.separacionCuerdas * 0.26);
            if (traste == 12) {
                // El doce lleva dos puntos, como en una guitarra
                circulo(g, cx, arriba + alto * 0.3, radio);
                circulo(g, cx, arriba + alto * 0.7, radio);
            } else {
                circulo(g, cx, arriba + alto / 2.0, radio);
            }
        }

        // Trastes de metal, y la cejuela mas gruesa
        g.setStroke(new BasicStroke(1.4f));
        for (int traste = 1; traste <= TRASTES; traste++) {
            g.setColor(TRASTE_METAL);
            int x = xDelTraste(traste);
            g.drawLine(x, arriba, x, abajo);
        }
        g.setStroke(new BasicStroke(3f));
        g.setColor(CEJUELA);
        g.drawLine(x0, arriba, x0, abajo);

        // Cuerdas: la mas grave, mas gruesa
        for (int cuerda = 0; cuerda < this.afinacion.length; cuerda++) {
            float grosor = 2.4f - 1.5f * cuerda / Math.max(1, this.afinacion.length - 1);
            g.setStroke(new BasicStroke(Math.max(0.8f, grosor)));
            g.setColor(CUERDA);
            int y = yDeLaCuerda(cuerda);
            g.drawLine(x0, y, x1, y);
        }

        dibujarNumeros(g, abajo);
    }

    private void dibujarNumeros(Graphics2D g, int abajo) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        FontMetrics m = g.getFontMetrics();
        g.setColor(NUMERO);
        for (int traste : MARCAS) {
            if (traste > TRASTES) {
                continue;
            }
            String texto = String.valueOf(traste);
            int cx = (xDelTraste(traste) + xDelTraste(traste - 1)) / 2;
            g.drawString(texto, cx - m.stringWidth(texto) / 2,
                    abajo + m.getAscent() + 3);
        }
        // El nombre de cada cuerda al aire, a la izquierda
        for (int cuerda = 0; cuerda < this.afinacion.length; cuerda++) {
            String texto = Estilo.nombreDeNota(this.afinacion[cuerda]);
            g.drawString(texto,
                    this.posicion.x + MARGEN_IZQUIERDO - m.stringWidth(texto) - 8,
                    yDeLaCuerda(cuerda) + m.getAscent() / 2 - 1);
        }
    }

    private static void circulo(Graphics2D g, double cx, double cy, double radio) {
        g.fill(new Ellipse2D.Double(cx - radio, cy - radio, radio * 2, radio * 2));
    }

    /**
     * Un sitio del mastil donde se puede pisar una nota. Apagado no se ve: si
     * se pintaran los noventa y seis, el mastil seria un colador.
     */
    private static final class Punto extends PiezaPulsable {

        private int radio;

        Punto(int nota) {
            super(nota);
            this.radio = 6;
        }

        void setRadio(int radio) {
            this.radio = radio;
            invalidarForma();
        }

        @Override
        public Color getColorNoPulsado() {
            return new Color(0, 0, 0, 0);
        }

        @Override
        protected int getHundimiento() {
            return 0;
        }

        @Override
        protected Shape construirForma() {
            return new Ellipse2D.Double(this.posicion.x, this.posicion.y,
                    this.radio * 2, this.radio * 2);
        }

        @Override
        protected double getCentroDelBrillo(Rectangle2D caja) {
            return caja.getCenterY();
        }

        @Override
        protected void dibujar(Graphics2D g, double intensidad) {
            if (intensidad <= 0.02) {
                return;
            }
            Shape forma = getForma();
            Color tinte = getColorPulsado();
            Color claro = Estilo.mezclar(tinte, Color.WHITE, 0.45);

            g.setColor(Estilo.conAlfa(claro, (int) Math.round(255 * intensidad)));
            g.fill(forma);
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(Estilo.conAlfa(tinte, (int) Math.round(235 * intensidad)));
            g.draw(forma);

            // El nombre de la nota dentro, si el punto da para tanto
            if (this.radio >= 8) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, this.radio));
                FontMetrics m = g.getFontMetrics();
                String texto = Estilo.nombreDeNota(getNumeroNota());
                Rectangle2D caja = forma.getBounds2D();
                g.setColor(Estilo.conAlfa(new Color(28, 22, 14),
                        (int) Math.round(235 * intensidad)));
                g.drawString(texto,
                        (float) (caja.getCenterX() - m.stringWidth(texto) / 2.0),
                        (float) (caja.getCenterY() + m.getAscent() / 2.0 - 1));
            }
        }
    }
}
