package jd.piano.programa;

import jd.piano.teclas.Piano;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Todo el dibujado del programa, sin depender de una ventana.
 * Al no tocar Swing se puede volcar a una imagen sin pantalla, asi que lo que
 * se ve en un PNG de prueba es exactamente lo que se ve en la ventana.
 */
public class Escena {

    private static final Color FONDO_LISO = new Color(90, 60, 40);

    private final int ancho;
    private final int alto;
    private BufferedImage fondo;
    private Piano piano;

    public Escena(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        this.fondo = null;
        this.piano = null;
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
    }

    public void setPiano(Piano piano) {
        this.piano = piano;
    }

    public Piano getPiano() {
        return this.piano;
    }

    public void dibujar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        if (this.fondo != null) {
            g.drawImage(this.fondo, 0, 0, null);
        } else {
            g.setColor(FONDO_LISO);
            g.fillRect(0, 0, this.ancho, this.alto);
        }

        if (this.piano != null) {
            this.piano.dibujar(g);
        }
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
