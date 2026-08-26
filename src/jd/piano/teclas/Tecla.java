package jd.piano.teclas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public abstract class Tecla implements Pulsable {

    /** Radio de las esquinas de abajo. Las de arriba se quedan rectas. */
    protected static final double RADIO = 3;

    private static final Color COLOR_PULSADO_POR_DEFECTO = new Color(255, 170, 60);

    protected Point posicion;
    private final int nota;
    private boolean pulsada;
    private int velocidad;
    private Color colorPulsada;
    private long instanteSuelte;

    public Tecla(int n) {
        this.nota = n;
        this.posicion = null;
        this.pulsada = false;
        this.velocidad = 0;
        this.colorPulsada = null;
        this.instanteSuelte = 0;
    }

    public int getNumeroNota() {
        return this.nota;
    }

    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
    }

    @Override
    public void pulsar(int velocidad) {
        this.pulsada = true;
        this.velocidad = velocidad;
    }

    @Override
    public void soltar() {
        this.pulsada = false;
        this.instanteSuelte = System.currentTimeMillis();
    }

    @Override
    public boolean estaPulsado() {
        return this.pulsada;
    }

    /** Fuerza de la ultima pulsacion, 0-127. */
    public int getVelocidad() {
        return this.velocidad;
    }

    /** Milisegundos desde que se solto, para poder desvanecer el color. */
    public long getMsDesdeSuelte() {
        long ms = Long.MAX_VALUE;
        if (this.instanteSuelte != 0) {
            ms = System.currentTimeMillis() - this.instanteSuelte;
        }
        return ms;
    }

    @Override
    public void setColorPulsado(Color c) {
        this.colorPulsada = c;
    }

    @Override
    public Color getColorPulsado() {
        Color c = this.colorPulsada;
        if (c == null) {
            c = COLOR_PULSADO_POR_DEFECTO;
        }
        return c;
    }

    @Override
    public void dibujar(Graphics2D g) {
        if (this.posicion == null) {
            throw new IllegalStateException(
                    "Hay que llamar a setPosicion antes de dibujar");
        }
        Shape forma = getForma();
        g.setColor(getColor());
        g.fill(forma);
        g.setColor(Color.BLACK);
        g.draw(forma);
    }

    /**
     * Contorno de la tecla en coordenadas absolutas. Vale tanto para pintarla
     * como para saber si un clic del raton cae dentro.
     */
    public abstract Shape getForma();

    /** Rectangulo con las esquinas de abajo redondeadas y las de arriba rectas. */
    protected static Area cuerpoRedondeadoAbajo(double x, double y,
                                                double ancho, double alto,
                                                double radio) {
        Area cuerpo = new Area(new RoundRectangle2D.Double(x, y, ancho, alto,
                radio * 2, radio * 2));
        cuerpo.add(new Area(new Rectangle2D.Double(x, y, ancho, alto - radio)));
        return cuerpo;
    }
}
