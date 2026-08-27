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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Un xilofono de cinco octavas, en realidad una marimba: barras de madera
 * puestas de mayor a menor, las alteradas en una fila levantada por detras.
 *
 * Es el instrumento mas facil de los tres, y por un motivo concreto: cada nota
 * tiene una barra y solo una. En la guitarra hay que elegir entre varias
 * posiciones posibles, aqui no hay nada que decidir.
 *
 * Las barras se acortan conforme suben, que es lo que pasa de verdad: una
 * barra corta suena mas aguda. Da la silueta en cuna que hace que se reconozca
 * al instrumento de un vistazo.
 */
public class Xilofono implements InstrumentoVisual {

    /** Do2 a Do7: cinco octavas, como una marimba grande. */
    public static final int NOTA_MINIMA = 36;
    public static final int NOTA_MAXIMA = 96;

    /** Largo de la barra mas grave y de la mas aguda, en fracciones de fila. */
    private static final double LARGO_GRAVE = 1.0;
    private static final double LARGO_AGUDO = 0.46;

    private static final int SEPARACION = 3;
    private static final int HUECO_ENTRE_FILAS = 9;
    /** Parte del alto que se lleva la fila de atras, la de las alteradas. */
    private static final double PARTE_FILA_ALTA = 0.42;

    private static final int PASO_MINIMO = 7;
    private static final int PASO_BASE = 24;

    private static final Color PALO_ROSA_ALTO = new Color(178, 108, 62);
    private static final Color PALO_ROSA_BAJO = new Color(122, 66, 34);
    private static final Color ALTERADA_ALTO = new Color(126, 72, 40);
    private static final Color ALTERADA_BAJO = new Color(82, 42, 20);
    private static final Color CANTO = new Color(226, 186, 146, 130);
    private static final Color CONTORNO = new Color(38, 20, 10, 170);
    private static final Color CAJA = new Color(30, 18, 11, 205);
    private static final Color CUERDA = new Color(64, 44, 28);
    private static final Color TEXTO = new Color(46, 26, 14);

    private final Barra[] barras;
    private final int naturales;

    private Point posicion;
    private int paso;
    /** Alto con el que se reparten las dos filas. */
    private int altoTotal;

    public Xilofono() {
        this.barras = new Barra[NOTA_MAXIMA - NOTA_MINIMA + 1];
        for (int nota = NOTA_MINIMA; nota <= NOTA_MAXIMA; nota++) {
            this.barras[nota - NOTA_MINIMA] = new Barra(nota);
        }
        this.naturales = contarNaturales(NOTA_MINIMA, NOTA_MAXIMA);
        this.posicion = null;
        this.paso = PASO_BASE;
        this.altoTotal = PASO_BASE * 8;
    }

    @Override
    public String getNombre() {
        return "Xilofono";
    }

    public int getNotaMinima() {
        return NOTA_MINIMA;
    }

    public int getNotaMaxima() {
        return NOTA_MAXIMA;
    }

    private static boolean esAlterada(int nota) {
        int clase = nota % 12;
        return clase == 1 || clase == 3 || clase == 6 || clase == 8 || clase == 10;
    }

    /** Cuantas notas naturales hay entre las dos, ambas incluidas. */
    private static int contarNaturales(int desde, int hasta) {
        int cuantas = 0;
        for (int nota = desde; nota <= hasta; nota++) {
            if (!esAlterada(nota)) {
                cuantas++;
            }
        }
        return cuantas;
    }

    // ---------- posiciones ----------

    private Barra barraDe(int nota) {
        return nota < NOTA_MINIMA || nota > NOTA_MAXIMA
                ? null : this.barras[nota - NOTA_MINIMA];
    }

    @Override
    public PiezaPulsable piezaDe(int canal, int nota) {
        // Un xilofono no distingue canales: la barra del Do es la misma para todos
        return barraDe(nota);
    }

    @Override
    public PiezaPulsable piezaEn(int x, int y) {
        if (this.posicion == null) {
            return null;
        }
        // Las alteradas van por delante en el orden de busqueda porque su fila
        // esta separada; aun asi se comprueba la forma, no la fila
        for (Barra barra : this.barras) {
            if (esAlterada(barra.getNumeroNota()) && barra.getForma().contains(x, y)) {
                return barra;
            }
        }
        for (Barra barra : this.barras) {
            if (!esAlterada(barra.getNumeroNota()) && barra.getForma().contains(x, y)) {
                return barra;
            }
        }
        return null;
    }

    @Override
    public void soltarTodas() {
        for (Barra barra : this.barras) {
            if (barra.estaPulsado()) {
                barra.soltar();
            }
        }
    }

    @Override
    public void soltarCanal(int canal) {
        soltarTodas();
    }

    // ---------- colocacion ----------

    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
        colocarBarras();
    }

    @Override
    public Rectangle getLimites() {
        return this.posicion == null ? null
                : new Rectangle(this.posicion.x, this.posicion.y,
                        getAnchura(), getAltura());
    }

    @Override
    public int getAnchura() {
        return this.paso * this.naturales;
    }

    @Override
    public int getAltura() {
        return this.altoFilaAlta() + HUECO_ENTRE_FILAS + this.altoFilaBaja();
    }

    private int altoFilaAlta() {
        return (int) Math.round((this.altoTotal - HUECO_ENTRE_FILAS) * PARTE_FILA_ALTA);
    }

    private int altoFilaBaja() {
        return this.altoTotal - HUECO_ENTRE_FILAS - altoFilaAlta();
    }

    @Override
    public void ajustarA(int ancho, int alto) {
        this.paso = Math.max(PASO_MINIMO, Math.min(PASO_BASE, ancho / this.naturales));
        // Sin tope el instrumento se estiraria hasta ocupar toda la pantalla y
        // las barras saldrian absurdamente largas
        this.altoTotal = Math.max(80, Math.min(alto, this.paso * 9));
        colocarBarras();
    }

    /** Largo relativo de la barra: 1 la mas grave, menos conforme sube. */
    private static double largoRelativo(int nota) {
        double avance = (double) (nota - NOTA_MINIMA) / (NOTA_MAXIMA - NOTA_MINIMA);
        return LARGO_GRAVE + (LARGO_AGUDO - LARGO_GRAVE) * avance;
    }

    private void colocarBarras() {
        if (this.posicion == null) {
            return;
        }
        int anchoNatural = Math.max(3, this.paso - SEPARACION);
        int anchoAlterada = Math.max(3, (int) Math.round(this.paso * 0.68));
        int yAlta = this.posicion.y;
        int yBaja = this.posicion.y + altoFilaAlta() + HUECO_ENTRE_FILAS;

        for (Barra barra : this.barras) {
            int nota = barra.getNumeroNota();
            boolean alterada = esAlterada(nota);
            int banda = alterada ? altoFilaAlta() : altoFilaBaja();
            int yBanda = alterada ? yAlta : yBaja;
            int largo = Math.max(6, (int) Math.round(banda * largoRelativo(nota)));

            int x;
            int anchoBarra;
            if (alterada) {
                // Centrada en la juntura de las dos naturales que la rodean
                int centro = this.posicion.x
                        + contarNaturales(NOTA_MINIMA, nota - 1) * this.paso;
                anchoBarra = anchoAlterada;
                x = centro - anchoBarra / 2;
            } else {
                anchoBarra = anchoNatural;
                x = this.posicion.x
                        + (contarNaturales(NOTA_MINIMA, nota) - 1) * this.paso
                        + SEPARACION / 2;
            }
            barra.setCaja(anchoBarra, largo);
            barra.setPosicion(x, yBanda + (banda - largo) / 2);
        }
    }

    // ---------- dibujado ----------

    @Override
    public void dibujar(Graphics2D g) {
        dibujarArmazon(g);
        for (Barra barra : this.barras) {
            barra.dibujar(g);
        }
    }

    @Override
    public void dibujarEnReposo(Graphics2D g) {
        dibujarArmazon(g);
    }

    @Override
    public void dibujarBrillos(Graphics2D g) {
        for (Barra barra : this.barras) {
            barra.dibujarBrillo(g);
        }
    }

    @Override
    public void anadirRegionesActivas(List<Rectangle> destino) {
        for (Barra barra : this.barras) {
            if (barra.getIntensidad() > 0) {
                destino.add(new Rectangle(barra.getCajaRepintado()));
            }
        }
    }

    /**
     * La caja de resonancia, los cordeles y la madera de las barras.
     *
     * El degradado se pone una sola vez por fila, no una por barra: crear
     * sesenta y un LinearGradientPaint en cada frame fue justo lo que hundio
     * los fotogramas la primera vez. Y sale mejor asi, porque la luz viene de
     * arriba para todo el instrumento a la vez, no para cada barra por su
     * cuenta.
     */
    private void dibujarArmazon(Graphics2D g) {
        if (this.posicion == null) {
            return;
        }
        int yAlta = this.posicion.y;
        int yBaja = this.posicion.y + altoFilaAlta() + HUECO_ENTRE_FILAS;

        dibujarCaja(g, yAlta, altoFilaAlta());
        dibujarCaja(g, yBaja, altoFilaBaja());

        rellenarFila(g, true, yAlta, altoFilaAlta());
        rellenarFila(g, false, yBaja, altoFilaBaja());

        // Cantos y contornos, que son trazos y salen baratos
        g.setStroke(new BasicStroke(1f));
        for (Barra barra : this.barras) {
            Rectangle caja = barra.getForma().getBounds();
            g.setColor(CONTORNO);
            g.draw(barra.getForma());
            g.setColor(CANTO);
            g.drawLine(caja.x + 2, caja.y + 1, caja.x + caja.width - 3, caja.y + 1);
        }
    }

    private void dibujarCaja(Graphics2D g, int y, int banda) {
        g.setColor(CAJA);
        g.fill(new RoundRectangle2D.Double(this.posicion.x - 6, y - 5,
                getAnchura() + 12, banda + 10, 8, 8));

        // Los dos cordeles de los que cuelgan las barras
        g.setColor(CUERDA);
        g.setStroke(new BasicStroke(2f));
        int arriba = y + (int) Math.round(banda * 0.20);
        int abajo = y + (int) Math.round(banda * 0.80);
        g.drawLine(this.posicion.x - 4, arriba, this.posicion.x + getAnchura() + 4, arriba);
        g.drawLine(this.posicion.x - 4, abajo, this.posicion.x + getAnchura() + 4, abajo);
    }

    private void rellenarFila(Graphics2D g, boolean alteradas, int y, int banda) {
        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, y), new Point2D.Float(0, y + banda),
                new float[]{0f, 1f},
                alteradas ? new Color[]{ALTERADA_ALTO, ALTERADA_BAJO}
                        : new Color[]{PALO_ROSA_ALTO, PALO_ROSA_BAJO}));
        for (Barra barra : this.barras) {
            if (esAlterada(barra.getNumeroNota()) == alteradas) {
                g.fill(barra.getForma());
            }
        }
    }

    /**
     * Una barra del xilofono. La madera la pinta el instrumento en una pasada
     * por filas; lo que dibuja la barra es solo el color de haberla golpeado.
     */
    private static final class Barra extends PiezaPulsable {

        private int ancho;
        private int largo;

        Barra(int nota) {
            super(nota);
            this.ancho = 20;
            this.largo = 60;
        }

        void setCaja(int ancho, int largo) {
            this.ancho = ancho;
            this.largo = largo;
            invalidarForma();
        }

        @Override
        public Color getColorNoPulsado() {
            return esAlterada(getNumeroNota()) ? ALTERADA_ALTO : PALO_ROSA_ALTO;
        }

        @Override
        protected int getHundimiento() {
            // Una barra no se hunde: se la golpea y vibra en el sitio
            return 0;
        }

        @Override
        protected Shape construirForma() {
            return new RoundRectangle2D.Double(this.posicion.x, this.posicion.y,
                    this.ancho, this.largo, 4, 4);
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

            // El relleno se aclara, pero el contorno se queda con el color
            // puro del canal: aclarando los dos, una barra estrecha salia casi
            // blanca y no se sabia de que canal era
            g.setColor(Estilo.conAlfa(Estilo.mezclar(tinte, Color.WHITE, 0.22),
                    (int) Math.round(235 * intensidad)));
            g.fill(forma);
            g.setStroke(new BasicStroke(1.8f));
            g.setColor(Estilo.conAlfa(tinte, (int) Math.round(250 * intensidad)));
            g.draw(forma);

            // El nombre de la nota si la barra da para leerlo
            if (this.ancho >= 14 && Estilo.isMostrarNombres()) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD,
                        Math.min(12, this.ancho - 4)));
                FontMetrics m = g.getFontMetrics();
                String texto = Estilo.nombreDeNota(getNumeroNota());
                Rectangle2D caja = forma.getBounds2D();
                g.setColor(Estilo.conAlfa(TEXTO, (int) Math.round(240 * intensidad)));
                g.drawString(texto,
                        (float) (caja.getCenterX() - m.stringWidth(texto) / 2.0),
                        (float) (caja.getMaxY() - 5));
            }
        }
    }
}
