package jd.piano.programa;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Ventana de dibujo hecha con Swing, equivalente a la CapaCanvas de ConsolaDAW.
 * Las teclas pintan sobre una imagen en memoria y un temporizador la va
 * volcando a la pantalla, asi que el piano puede dibujarse desde el hilo MIDI
 * sin pelearse con el hilo de Swing.
 */
public class Lienzo {

    private static final int REFRESCO_MS = 33;
    private static final Color FONDO_LISO = new Color(90, 60, 40);

    private final BufferedImage imagen;
    private final Graphics graphics;
    private final JPanel panel;
    private final JFrame ventana;

    public Lienzo(String titulo, int ancho, int alto) {
        this.imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        this.graphics = this.imagen.getGraphics();
        this.graphics.setColor(FONDO_LISO);
        this.graphics.fillRect(0, 0, ancho, alto);

        this.panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(imagen, 0, 0, null);
            }
        };
        this.panel.setPreferredSize(new Dimension(ancho, alto));

        this.ventana = new JFrame(titulo);
        this.ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.ventana.setContentPane(this.panel);
        this.ventana.pack();
        this.ventana.setLocationRelativeTo(null);
    }

    /** El Graphics que hay que pasarle al piano con setGraphics. */
    public Graphics getGraphics() {
        return this.graphics;
    }

    public void setFondo(Image fondo) {
        this.graphics.drawImage(fondo, 0, 0, this.imagen.getWidth(),
                this.imagen.getHeight(), null);
    }

    public void mostrar() {
        this.ventana.setVisible(true);
        new Timer(REFRESCO_MS, e -> this.panel.repaint()).start();
    }

    public int getAnchura() {
        return this.imagen.getWidth();
    }

    public int getAltura() {
        return this.imagen.getHeight();
    }
}
