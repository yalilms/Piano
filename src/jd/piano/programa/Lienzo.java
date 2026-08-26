package jd.piano.programa;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * La ventana. Solo se encarga de mostrar una Escena y de repintarla
 * periodicamente; todo el dibujado vive en Escena.
 */
public class Lienzo {

    private static final int REFRESCO_MS = 16;

    private final Escena escena;
    private final JPanel panel;
    private final JFrame ventana;

    public Lienzo(String titulo, Escena escena) {
        this.escena = escena;

        this.panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Lienzo.this.escena.dibujar((Graphics2D) g);
            }
        };
        this.panel.setPreferredSize(new Dimension(escena.getAnchura(), escena.getAltura()));

        this.ventana = new JFrame(titulo);
        this.ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.ventana.setContentPane(this.panel);
        this.ventana.pack();
        this.ventana.setLocationRelativeTo(null);
    }

    public void mostrar() {
        this.ventana.setVisible(true);
        new Timer(REFRESCO_MS, e -> this.panel.repaint()).start();
    }
}
