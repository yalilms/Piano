package jd.piano.teclas;

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

public abstract class Tecla implements Pulsable {

    private static final Color COLOR_PULSADO_POR_DEFECTO = new Color(255, 170, 60);

    /** Holgura al repintar, para que entren el hundimiento y las sombras. */
    public static final int MARGEN_REPINTADO = 6;

    protected Point posicion;
    protected MedidasTeclado medidas;
    private final int nota;
    private boolean pulsada;
    private int velocidad;
    private Color colorPulsada;
    private long instanteSuelte;

    /**
     * Construir la forma cuesta (son operaciones de Area), y a 60 fps se pide
     * muchas veces por segundo. Se cachea y se invalida al mover o reescalar
     * la tecla. Se guarda como Path2D y no como Area porque rellenar un Area
     * obliga a Java2D a retesetelar la geometria en cada fill.
     */
    private Shape formaCache;
    private Rectangle cajaCache;

    public Tecla(int n, MedidasTeclado medidas) {
        this.nota = n;
        this.medidas = medidas;
        this.posicion = null;
        this.pulsada = false;
        this.velocidad = 0;
        this.colorPulsada = null;
        this.instanteSuelte = 0;
        this.formaCache = null;
        this.cajaCache = null;
    }

    public int getNumeroNota() {
        return this.nota;
    }

    public MedidasTeclado getMedidas() {
        return this.medidas;
    }

    /** Cambia el tamano de la tecla sin perder su estado ni su identidad. */
    public void setMedidas(MedidasTeclado medidas) {
        this.medidas = medidas;
        invalidarForma();
    }

    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
        invalidarForma();
    }

    private void invalidarForma() {
        this.formaCache = null;
        this.cajaCache = null;
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
        dibujar(g, getIntensidad());
    }

    /**
     * Dibuja la tecla apagada, ignorando si esta pulsada ahora mismo.
     * La Escena lo usa para hornear el teclado en reposo en su imagen cacheada,
     * y necesita que salga apagado aunque la cancion ya este sonando.
     */
    public void dibujarEnReposo(Graphics2D g) {
        dibujar(g, 0);
    }

    protected abstract void dibujar(Graphics2D g, double intensidad);

    /**
     * Contorno de la tecla en coordenadas absolutas, sin hundimiento.
     * Es lo que hay que usar para saber si un clic del raton cae dentro.
     */
    public final Shape getForma() {
        if (this.formaCache == null) {
            if (this.posicion == null) {
                throw new IllegalStateException(
                        "Hay que llamar a setPosicion antes de usar la forma");
            }
            this.formaCache = new Path2D.Double(construirForma());
        }
        return this.formaCache;
    }

    /**
     * Caja que hay que repintar para esta tecla: su contorno mas holgura para
     * el hundimiento y la sombra. Cacheada porque se consulta en cada frame.
     * Quien la reciba no debe modificarla.
     */
    public Rectangle getCajaRepintado() {
        if (this.cajaCache == null) {
            this.cajaCache = getForma().getBounds();
            this.cajaCache.grow(MARGEN_REPINTADO, MARGEN_REPINTADO);
        }
        return this.cajaCache;
    }

    /** La forma tal y como se pinta: baja unos pixeles si esta pulsada. */
    protected Shape getFormaDibujo() {
        Shape forma = getForma();
        if (this.pulsada) {
            forma = AffineTransform
                    .getTranslateInstance(0, this.medidas.getHundimiento())
                    .createTransformedShape(forma);
        }
        return forma;
    }

    /**
     * Resplandor que se derrama por el borde inferior de la tecla pulsada.
     * Se pinta en una pasada aparte, encima de todo, para que la luz cruce por
     * delante de las teclas vecinas.
     *
     * El degradado radial se pre-renderiza a una imagen y se cachea por color y
     * radio: generarlo en cada frame para cada tecla pulsada salia carisimo.
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
                (int) Math.round(caja.getMaxY()) - radio, null);
        g.setComposite(anterior);
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

    protected abstract Shape construirForma();

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
