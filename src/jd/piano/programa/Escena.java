package jd.piano.programa;

import jd.piano.teclas.Piano;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo el dibujado del programa, sin depender de una ventana.
 * Al no tocar Swing se puede volcar a una imagen sin pantalla, asi que lo que
 * se ve en un PNG de prueba es exactamente lo que se ve en la ventana.
 *
 * Casi nada de la escena cambia entre frames: el fondo, la tapa, el reflejo y
 * las teclas en reposo son siempre iguales. Todo eso se pre-compone en dos
 * imagenes y cada frame solo se repinta el trozo donde hay teclas con color.
 * Medido: repintar las 88 teclas cuesta 14 ms y volcar una imagen 0,5 ms.
 */
public class Escena {

    private static final Color FONDO_LISO = new Color(90, 60, 40);

    /** Alto de la tapa brillante bajo el teclado, donde cae el reflejo. */
    private static final int ALTO_TAPA = 34;
    private static final float OPACIDAD_REFLEJO = 0.28f;

    private final int ancho;
    private final int alto;
    private BufferedImage fondo;
    private Piano piano;

    /** Fondo, tapa y reflejo, sin teclas. Sirve para borrar la region sucia. */
    private BufferedImage escenarioSinTeclas;
    /** Lo anterior mas las teclas en reposo. Es el punto de partida del frame. */
    private BufferedImage escenarioCompleto;
    private BufferedImage vineta;

    /** Se reutiliza entre frames para no ir asignando listas a 60 fps. */
    private final List<Rectangle> sucias = new ArrayList<>();

    public Escena(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        this.fondo = null;
        this.piano = null;
        this.escenarioSinTeclas = null;
        this.escenarioCompleto = null;
        this.vineta = null;
    }

    public void setFondo(Image imagen) {
        BufferedImage escalado = new BufferedImage(this.ancho, this.alto,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = escalado.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(imagen, 0, 0, this.ancho, this.alto, null);
        g.dispose();
        this.fondo = escalado;
        invalidarCaches();
    }

    public void setPiano(Piano piano) {
        this.piano = piano;
        invalidarCaches();
    }

    public Piano getPiano() {
        return this.piano;
    }

    /**
     * Fuerza a recomponer las imagenes cacheadas. Hay que llamarlo si se mueve
     * el piano o cambia el fondo.
     */
    public void invalidarCaches() {
        this.escenarioSinTeclas = null;
        this.escenarioCompleto = null;
        this.vineta = null;
    }

    /**
     * Compone las imagenes cacheadas. Conviene llamarlo con el piano ya
     * colocado y antes de que empiece a sonar nada, porque las teclas se
     * hornean en reposo.
     */
    public void prepararCaches() {
        this.vineta = crearVineta();
        BufferedImage capaReposo = crearCapaPiano();

        this.escenarioSinTeclas = new BufferedImage(this.ancho, this.alto,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = this.escenarioSinTeclas.createGraphics();
        aplicarCalidad(g);
        dibujarFondo(g);
        if (this.piano != null && this.piano.getLimites() != null && capaReposo != null) {
            dibujarTapaConReflejo(g, this.piano.getLimites(), capaReposo);
        }
        g.dispose();

        this.escenarioCompleto = new BufferedImage(this.ancho, this.alto,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = this.escenarioCompleto.createGraphics();
        g2.drawImage(this.escenarioSinTeclas, 0, 0, null);
        if (capaReposo != null) {
            g2.drawImage(capaReposo, 0, 0, null);
        }
        g2.dispose();
    }

    public void dibujar(Graphics2D g) {
        if (this.escenarioCompleto == null) {
            prepararCaches();
        }
        aplicarCalidad(g);
        g.drawImage(this.escenarioCompleto, 0, 0, null);

        if (this.piano != null) {
            this.sucias.clear();
            this.piano.anadirRegionesActivas(this.sucias);
            fusionar(this.sucias);

            Shape clipAnterior = g.getClip();
            for (Rectangle sucia : this.sucias) {
                g.setClip(sucia);
                // Se restaura el escenario sin teclas para no acumular sombras
                // semitransparentes al volver a pintarlas encima.
                g.drawImage(this.escenarioSinTeclas, 0, 0, null);
                this.piano.dibujar(g);
            }
            g.setClip(clipAnterior);

            // Los brillos van sin recortar: la luz se derrama lejos de la tecla
            this.piano.dibujarBrillos(g);
        }

        g.drawImage(this.vineta, 0, 0, null);
    }

    /**
     * Une los rectangulos que se pisan entre si. Hace falta porque cada region
     * se repinta restaurando el escenario debajo: si dos se solapasen, la
     * segunda borraria parte de lo que acaba de dibujar la primera.
     */
    private static void fusionar(List<Rectangle> regiones) {
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (int i = 0; i < regiones.size() && !cambio; i++) {
                for (int j = i + 1; j < regiones.size() && !cambio; j++) {
                    if (regiones.get(i).intersects(regiones.get(j))) {
                        regiones.get(i).add(regiones.get(j));
                        regiones.remove(j);
                        cambio = true;
                    }
                }
            }
        }
    }

    private BufferedImage crearCapaPiano() {
        BufferedImage capa = null;
        if (this.piano != null && this.piano.getLimites() != null) {
            capa = new BufferedImage(this.ancho, this.alto,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = capa.createGraphics();
            aplicarCalidad(g);
            this.piano.dibujar(g);
            g.dispose();
        }
        return capa;
    }

    private static void aplicarCalidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
    }

    private void dibujarFondo(Graphics2D g) {
        if (this.fondo != null) {
            g.drawImage(this.fondo, 0, 0, null);
        } else {
            g.setColor(FONDO_LISO);
            g.fillRect(0, 0, this.ancho, this.alto);
        }
    }

    /**
     * La tapa laqueada bajo el teclado con el piano reflejado encima, volteado
     * y desvaneciendose. Es el detalle que mas hace que parezca un instrumento
     * de verdad y no un dibujo.
     */
    private void dibujarTapaConReflejo(Graphics2D g, Rectangle limites,
                                       BufferedImage capaPiano) {
        int yBase = limites.y + limites.height;
        int altoTapa = Math.min(ALTO_TAPA, this.alto - yBase);
        if (altoTapa <= 0) {
            return;
        }

        Shape clipAnterior = g.getClip();
        g.clip(new Rectangle2D.Double(0, yBase, this.ancho, altoTapa));

        g.setPaint(new GradientPaint(
                0f, yBase, new Color(34, 21, 12, 240),
                0f, yBase + altoTapa, new Color(12, 7, 4, 255)));
        g.fill(new Rectangle2D.Double(0, yBase, this.ancho, altoTapa));

        // Reflejo especular sobre y = yBase: el punto y pasa a 2*yBase - y
        AffineTransform volteo = new AffineTransform();
        volteo.translate(0, 2.0 * yBase);
        volteo.scale(1, -1);
        Composite composicionAnterior = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, OPACIDAD_REFLEJO));
        g.drawImage(capaPiano, volteo, null);
        g.setComposite(composicionAnterior);

        g.setPaint(new GradientPaint(
                0f, yBase, new Color(12, 7, 4, 0),
                0f, yBase + altoTapa, new Color(12, 7, 4, 255)));
        g.fill(new Rectangle2D.Double(0, yBase, this.ancho, altoTapa));

        g.setClip(clipAnterior);
    }

    /** Oscurece los bordes para que la vista se vaya al teclado. */
    private BufferedImage crearVineta() {
        BufferedImage img = new BufferedImage(this.ancho, this.alto,
                BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = img.createGraphics();
        float cx = this.ancho / 2f;
        float cy = this.alto / 2f;
        float radio = (float) Math.hypot(cx, cy);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), radio,
                new float[]{0.40f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 130)}));
        g.fillRect(0, 0, this.ancho, this.alto);
        g.dispose();
        return img;
    }

    /** Vuelca la escena a una imagen. Para pruebas y para exportar capturas. */
    public BufferedImage aImagen() {
        BufferedImage img = new BufferedImage(this.ancho, this.alto,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        dibujar(g);
        g.dispose();
        return img;
    }

    public int getAnchura() {
        return this.ancho;
    }

    public int getAltura() {
        return this.alto;
    }
}
