package jd.piano.programa;

import jd.piano.instrumentos.InstrumentoVisual;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
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

    /** Mas trozos de estos y sale mas barato juntarlos en rectangulos mayores. */
    private static final int MAXIMO_REGIONES = 14;

    /** A partir de esta fraccion de la escena cubierta, se repinta todo. */
    private static final double FRACCION_COLAPSO = 0.45;

    private int ancho;
    private int alto;
    private BufferedImage fondo;
    private Image fondoOriginal;
    private InstrumentoVisual instrumento;

    /** Aviso centrado para cuando no hay piano que dibujar. */
    private String mensaje;
    private final List<CapaSuperpuesta> superpuestas = new ArrayList<>();

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
        this.fondoOriginal = null;
        this.instrumento = null;
        this.mensaje = null;
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
        this.fondoOriginal = imagen;
        invalidarCaches();
    }

    public void setInstrumento(InstrumentoVisual instrumento) {
        this.instrumento = instrumento;
        if (instrumento != null) {
            this.mensaje = null;
        }
        invalidarCaches();
    }

    /**
     * Texto centrado en el lienzo, para cuando todavia no hay nada que dibujar.
     * Sin esto la pantalla de espera se quedaba en madera pelada.
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
        invalidarCaches();
    }

    public InstrumentoVisual getInstrumento() {
        return this.instrumento;
    }

    /**
     * Capas que se pintan encima del piano en cada frame, como las notas
     * cayendo o las chispas. No entran en las imagenes cacheadas porque
     * cambian siempre.
     */
    public void anadirSuperpuesta(CapaSuperpuesta capa) {
        if (capa != null) {
            this.superpuestas.add(capa);
        }
    }

    public void quitarSuperpuestas() {
        this.superpuestas.clear();
    }

    /**
     * Cambia el tamano de la escena y rehace todo lo cacheado. Devuelve true
     * si el tamano ha cambiado de verdad, para no recomponer de balde en cada
     * evento de redimensionado que manda Swing.
     */
    public boolean redimensionar(int ancho, int alto) {
        boolean cambia = ancho > 0 && alto > 0
                && (ancho != this.ancho || alto != this.alto);
        if (cambia) {
            this.ancho = ancho;
            this.alto = alto;
            invalidarCaches();
            if (this.fondoOriginal != null) {
                setFondo(this.fondoOriginal);
            }
        }
        return cambia;
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
     * Compone las imagenes cacheadas. Se puede llamar en cualquier momento:
     * las teclas se hornean con dibujarEnReposo, asi que salen apagadas aunque
     * la cancion ya este sonando.
     */
    public void prepararCaches() {
        this.vineta = crearVineta();
        BufferedImage capaReposo = crearCapaPiano();

        this.escenarioSinTeclas = new BufferedImage(this.ancho, this.alto,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = this.escenarioSinTeclas.createGraphics();
        aplicarCalidad(g);
        dibujarFondo(g);
        if (this.instrumento != null && this.instrumento.getLimites() != null && capaReposo != null) {
            dibujarTapaConReflejo(g, this.instrumento.getLimites(), capaReposo);
        }
        g.dispose();

        this.escenarioCompleto = new BufferedImage(this.ancho, this.alto,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = this.escenarioCompleto.createGraphics();
        g2.drawImage(this.escenarioSinTeclas, 0, 0, null);
        if (capaReposo != null) {
            g2.drawImage(capaReposo, 0, 0, null);
        }
        // La vineta va horneada aqui para no tener que volcar una imagen ARGB
        // a pantalla completa en cada frame; sobre las zonas que se repintan se
        // vuelve a aplicar, pero solo en ese trozo.
        g2.drawImage(this.vineta, 0, 0, null);
        if (this.mensaje != null) {
            dibujarMensaje(g2);
        }
        g2.dispose();
    }

    private void dibujarMensaje(Graphics2D g) {
        aplicarCalidad(g);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        FontMetrics m = g.getFontMetrics();
        int x = (this.ancho - m.stringWidth(this.mensaje)) / 2;
        int y = this.alto / 2;
        g.setColor(new Color(0, 0, 0, 140));
        g.drawString(this.mensaje, x + 2, y + 2);
        g.setColor(new Color(238, 226, 204));
        g.drawString(this.mensaje, x, y);
    }

    public void dibujar(Graphics2D g) {
        if (this.escenarioCompleto == null) {
            prepararCaches();
        }
        aplicarCalidad(g);
        g.drawImage(this.escenarioCompleto, 0, 0, null);

        if (this.instrumento != null) {
            this.sucias.clear();
            this.instrumento.anadirRegionesActivas(this.sucias);
            recortarALaEscena(this.sucias);
            fusionar(this.sucias);
            limitar(this.sucias, MAXIMO_REGIONES);
            colapsarSiCubreCasiTodo(this.sucias);

            Shape clipAnterior = g.getClip();
            for (Rectangle sucia : this.sucias) {
                g.setClip(sucia);
                // Se restaura el escenario sin teclas para no acumular sombras
                // semitransparentes al volver a pintarlas encima. Se copia solo
                // el trozo: volcar la imagen entera en cada region sale caro.
                volcarTrozo(g, this.escenarioSinTeclas, sucia);
                this.instrumento.dibujar(g);
                volcarTrozo(g, this.vineta, sucia);
            }
            g.setClip(clipAnterior);

            // Los brillos van sin recortar: la luz se derrama lejos de la tecla
            this.instrumento.dibujarBrillos(g);

            for (CapaSuperpuesta capa : this.superpuestas) {
                capa.dibujar(g, this.instrumento);
            }
        }
    }

    /**
     * Junta regiones hasta bajar del maximo, eligiendo cada vez el par cuya
     * union desperdicia menos area. Con muchas teclas sonando a la vez salen
     * decenas de trozos, y cada uno cuesta un setClip y dos volcados: a partir
     * de cierto punto sale mas barato repintar rectangulos mas grandes.
     */
    private static void limitar(List<Rectangle> regiones, int maximo) {
        while (regiones.size() > maximo) {
            int mejorI = 0;
            int mejorJ = 1;
            long mejorCoste = Long.MAX_VALUE;
            for (int i = 0; i < regiones.size(); i++) {
                for (int j = i + 1; j < regiones.size(); j++) {
                    Rectangle union = regiones.get(i).union(regiones.get(j));
                    long coste = (long) union.width * union.height
                            - area(regiones.get(i)) - area(regiones.get(j));
                    if (coste < mejorCoste) {
                        mejorCoste = coste;
                        mejorI = i;
                        mejorJ = j;
                    }
                }
            }
            regiones.get(mejorI).add(regiones.get(mejorJ));
            regiones.remove(mejorJ);
        }
    }

    private static long area(Rectangle r) {
        return (long) r.width * r.height;
    }

    /**
     * Si entre todos los trozos ya cubren casi la escena, sale mas barato un
     * unico repintado completo que catorce grandes que se pisan. Sin esto, con
     * muchisimas teclas sonando a la vez el coste subia en vez de bajar.
     */
    private void colapsarSiCubreCasiTodo(List<Rectangle> regiones) {
        if (regiones.size() <= 1) {
            return;
        }
        long total = 0;
        for (Rectangle r : regiones) {
            total += area(r);
        }
        if (total > (long) this.ancho * this.alto * FRACCION_COLAPSO) {
            regiones.clear();
            regiones.add(new Rectangle(0, 0, this.ancho, this.alto));
        }
    }

    /** Copia solo el rectangulo indicado, no la imagen completa. */
    private static void volcarTrozo(Graphics2D g, BufferedImage img, Rectangle r) {
        g.drawImage(img,
                r.x, r.y, r.x + r.width, r.y + r.height,
                r.x, r.y, r.x + r.width, r.y + r.height, null);
    }

    /** Deja las regiones dentro de la escena y descarta las que se salen. */
    private void recortarALaEscena(List<Rectangle> regiones) {
        Rectangle limite = new Rectangle(0, 0, this.ancho, this.alto);
        for (int i = regiones.size() - 1; i >= 0; i--) {
            Rectangle recortada = regiones.get(i).intersection(limite);
            if (recortada.isEmpty()) {
                regiones.remove(i);
            } else {
                regiones.set(i, recortada);
            }
        }
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
        if (this.instrumento != null && this.instrumento.getLimites() != null) {
            capa = new BufferedImage(this.ancho, this.alto,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = capa.createGraphics();
            aplicarCalidad(g);
            this.instrumento.dibujarEnReposo(g);
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
