package jd.piano.instrumentos;

import jd.piano.teclas.Estilo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Algo que se puede pulsar y se ilumina: una tecla, un traste de la guitarra,
 * una barra del xilofono.
 *
 * Todo eso comparte lo mismo: una forma, si esta pulsada, con que fuerza, y un
 * desvanecido al soltarla. Estaba metido en Tecla, y sacarlo aqui es lo que
 * permite tener otros instrumentos sin repetirlo.
 */
public abstract class PiezaPulsable {

    private static final Color COLOR_PULSADO_POR_DEFECTO = new Color(255, 170, 60);

    /** Holgura al repintar, para que entren el hundimiento y las sombras. */
    public static final int MARGEN_REPINTADO = 6;

    protected volatile Point posicion;
    private final int nota;

    /*
     * Estos cinco los escribe el hilo del MIDI (o el del teclado) y los lee el
     * hilo de Swing al pintar. Con volatile la lectura ve siempre el ultimo
     * valor escrito; sin el, un frame podia quedarse con estado viejo.
     */
    private volatile boolean pulsada;
    private volatile int velocidad;
    private volatile Color colorPulsada;
    private volatile long instanteSuelte;
    private volatile long instantePulsacion;

    /**
     * Construir la forma cuesta, y a 60 fps se pide muchas veces por segundo.
     * Se cachea como Path2D y no como Area porque rellenar un Area obliga a
     * Java2D a retesetelar la geometria en cada fill.
     *
     * Volatiles, y se leen a una variable local antes de mirarlas: quien
     * recoloca el instrumento las pone a null, y comprobar el campo y devolverlo
     * despues dejaba una rendija por la que se colaba un null. Si dos hilos
     * coinciden, lo peor que pasa es que la forma se construya dos veces.
     */
    private volatile Shape formaCache;
    private volatile Rectangle cajaCache;

    protected PiezaPulsable(int nota) {
        this.nota = nota;
        this.posicion = null;
        this.pulsada = false;
        this.velocidad = 0;
        this.colorPulsada = null;
        this.instanteSuelte = 0;
        this.instantePulsacion = 0;
        this.formaCache = null;
        this.cajaCache = null;
    }

    public int getNumeroNota() {
        return this.nota;
    }

    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
        invalidarForma();
    }

    protected void invalidarForma() {
        this.formaCache = null;
        this.cajaCache = null;
    }

    public void pulsar(int velocidad) {
        this.pulsada = true;
        this.velocidad = velocidad;
        this.instantePulsacion = System.currentTimeMillis();
    }

    public void soltar() {
        this.pulsada = false;
        this.instanteSuelte = System.currentTimeMillis();
    }

    public boolean estaPulsado() {
        return this.pulsada;
    }

    /** Fuerza de la ultima pulsacion, 0-127. */
    public int getVelocidad() {
        return this.velocidad;
    }

    /**
     * Cuando se pulso por ultima vez, en milisegundos del reloj del sistema.
     * Lo usan las particulas y el rastro para saber que golpes son nuevos.
     */
    public long getInstantePulsacion() {
        return this.instantePulsacion;
    }

    /**
     * Cuanto color de pulsacion se ve, de 0 a 1. Mientras esta pulsada depende
     * de la velocidad MIDI, asi que una nota suave se ve tenue y un acorde
     * fuerte destella. Al soltarla decae hasta 0 en FUNDIDO_MS.
     */
    public double getIntensidad() {
        double porVelocidad = 0.35 + 0.65 * (this.velocidad / 127.0);
        double intensidad;
        if (this.pulsada) {
            intensidad = porVelocidad;
        } else if (this.instanteSuelte == 0) {
            intensidad = 0;
        } else {
            long ms = System.currentTimeMillis() - this.instanteSuelte;
            if (ms >= Estilo.FUNDIDO_MS) {
                intensidad = 0;
            } else {
                double queda = 1.0 - (double) ms / Estilo.FUNDIDO_MS;
                intensidad = queda * queda * porVelocidad;
            }
        }
        return intensidad;
    }

    public void setColorPulsado(Color c) {
        this.colorPulsada = c;
    }

    public Color getColorPulsado() {
        Color c = this.colorPulsada;
        if (c == null) {
            c = COLOR_PULSADO_POR_DEFECTO;
        }
        return c;
    }

    public abstract Color getColorNoPulsado();

    public Color getColor() {
        return estaPulsado() ? getColorPulsado() : getColorNoPulsado();
    }

    /**
     * Contorno en coordenadas absolutas, sin hundimiento. Es lo que hay que
     * usar para saber si un clic del raton cae dentro.
     */
    public final Shape getForma() {
        Shape forma = this.formaCache;
        if (forma == null) {
            if (this.posicion == null) {
                throw new IllegalStateException(
                        "Hay que llamar a setPosicion antes de usar la forma");
            }
            forma = new Path2D.Double(construirForma());
            this.formaCache = forma;
        }
        return forma;
    }

    /**
     * Caja que hay que repintar: el contorno mas holgura para el hundimiento y
     * la sombra. Cacheada porque se consulta en cada frame. Quien la reciba no
     * debe modificarla.
     */
    public Rectangle getCajaRepintado() {
        Rectangle caja = this.cajaCache;
        if (caja == null) {
            caja = getForma().getBounds();
            caja.grow(MARGEN_REPINTADO, MARGEN_REPINTADO);
            this.cajaCache = caja;
        }
        return caja;
    }

    /** La forma tal y como se pinta: baja unos pixeles si esta pulsada. */
    protected Shape getFormaDibujo() {
        Shape forma = getForma();
        int hundimiento = getHundimiento();
        if (this.pulsada && hundimiento != 0) {
            forma = AffineTransform.getTranslateInstance(0, hundimiento)
                    .createTransformedShape(forma);
        }
        return forma;
    }

    /** Cuanto se desplaza al pulsarla. Cero si no se hunde. */
    protected abstract int getHundimiento();

    public void dibujar(Graphics2D g) {
        dibujar(g, getIntensidad());
    }

    /**
     * Dibuja la pieza apagada, ignorando si esta pulsada ahora mismo.
     * La Escena lo usa para hornear su imagen cacheada, y necesita que salga
     * en reposo aunque la cancion ya este sonando.
     */
    public void dibujarEnReposo(Graphics2D g) {
        dibujar(g, 0);
    }

    protected abstract void dibujar(Graphics2D g, double intensidad);

    protected abstract Shape construirForma();

    /**
     * Resplandor que se derrama por el borde de la pieza pulsada. Se pinta en
     * una pasada aparte, encima de todo, para que la luz cruce por delante de
     * las vecinas.
     *
     * El degradado radial se pre-renderiza a una imagen y se cachea por color y
     * radio: generarlo en cada frame para cada pieza pulsada salia carisimo.
     */
    public void dibujarBrillo(Graphics2D g) {
        double intensidad = getIntensidad();
        if (intensidad <= 0.05) {
            return;
        }
        Rectangle2D caja = getForma().getBounds2D();
        int radio = (int) Math.max(8, caja.getWidth() * 2.2);
        BufferedImage sprite = sprite(getColorPulsado(), radio);

        Composite anterior = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, (float) Math.min(1, intensidad)));
        g.drawImage(sprite,
                (int) Math.round(caja.getCenterX()) - radio,
                (int) Math.round(getCentroDelBrillo(caja)) - radio, null);
        g.setComposite(anterior);
    }

    /**
     * Donde nace el resplandor. En una tecla es el borde de abajo, porque la
     * luz se derrama hacia el que toca; en una pieza redonda, su centro.
     */
    protected double getCentroDelBrillo(Rectangle2D caja) {
        return caja.getMaxY();
    }

    private static final Map<String, BufferedImage> CACHE_BRILLO = new HashMap<>();

    private static BufferedImage sprite(Color tinte, int radio) {
        String clave = tinte.getRGB() + "/" + radio;
        BufferedImage sprite = CACHE_BRILLO.get(clave);
        if (sprite == null) {
            sprite = new BufferedImage(radio * 2, radio * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprite.createGraphics();
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(radio, radio), radio,
                    new float[]{0f, 1f},
                    new Color[]{Estilo.conAlfa(tinte, 110), Estilo.conAlfa(tinte, 0)}));
            g.fillRect(0, 0, radio * 2, radio * 2);
            g.dispose();
            CACHE_BRILLO.put(clave, sprite);
        }
        return sprite;
    }

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
