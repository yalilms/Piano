package jd.piano.programa;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * El panel donde se pinta el piano. Solo muestra una Escena y la repinta
 * periodicamente; todo el dibujado vive en Escena.
 */
public class Lienzo extends JPanel {

    private static final int REFRESCO_MS = 16;

    private final Escena escena;

    /** Se ejecuta cuando cambia el tamano, para recolocar el piano. */
    private Runnable alRedimensionar;

    public Lienzo(Escena escena) {
        this.escena = escena;
        this.alRedimensionar = null;
        setPreferredSize(new Dimension(escena.getAnchura(), escena.getAltura()));
        setDoubleBuffered(true);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                atenderRedimension();
            }
        });
        new Timer(REFRESCO_MS, e -> repaint()).start();
    }

    /**
     * Que hacer cuando el panel cambia de tamano: normalmente recalcular el
     * layout del piano y volver a centrarlo.
     */
    public void setAlRedimensionar(Runnable accion) {
        this.alRedimensionar = accion;
    }

    public Escena getEscena() {
        return this.escena;
    }

    private void atenderRedimension() {
        boolean cambio = this.escena.redimensionar(getWidth(), getHeight());
        if (cambio && this.alRedimensionar != null) {
            this.alRedimensionar.run();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.escena.dibujar((Graphics2D) g);
    }
}
