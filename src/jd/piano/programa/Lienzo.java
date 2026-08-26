package jd.piano.programa;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * La ventana. Solo se encarga de mostrar una Escena y de repintarla
 * periodicamente; todo el dibujado vive en Escena.
 */
public class Lienzo {

    private static final int REFRESCO_MS = 16;

    private final Escena escena;
    private final JPanel panel;
    private final JFrame ventana;

    /** Se ejecuta cuando cambia el tamano, para recolocar el piano. */
    private Runnable alRedimensionar;

    public Lienzo(String titulo, Escena escena) {
        this.escena = escena;
        this.alRedimensionar = null;

        this.panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Lienzo.this.escena.dibujar((Graphics2D) g);
            }
        };
        this.panel.setPreferredSize(new Dimension(escena.getAnchura(), escena.getAltura()));
        this.panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                atenderRedimension();
            }
        });

        this.ventana = new JFrame(titulo);
        this.ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.ventana.setContentPane(this.panel);
        this.ventana.pack();
        this.ventana.setLocationRelativeTo(null);
    }

    /**
     * Que hacer cuando la ventana cambia de tamano: normalmente recalcular el
     * layout del piano y volver a centrarlo.
     */
    public void setAlRedimensionar(Runnable accion) {
        this.alRedimensionar = accion;
    }

    private void atenderRedimension() {
        boolean cambio = this.escena.redimensionar(
                this.panel.getWidth(), this.panel.getHeight());
        if (cambio && this.alRedimensionar != null) {
            this.alRedimensionar.run();
        }
    }

    public void mostrar() {
        this.ventana.setVisible(true);
        new Timer(REFRESCO_MS, e -> this.panel.repaint()).start();
    }

    public void setTitulo(String titulo) {
        this.ventana.setTitle(titulo);
    }
}
