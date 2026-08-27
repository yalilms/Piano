package jd.piano.programa;

import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * El panel donde se pinta el piano. Muestra una Escena, la repinta
 * periodicamente y deja tocar las teclas con el raton.
 */
public class Lienzo extends JPanel {

    private static final int REFRESCO_MS = 16;

    private final Escena escena;
    private SintetizadorTeclado sintetizador;

    /** Se ejecuta cuando cambia el tamano, para recolocar el piano. */
    private Runnable alRedimensionar;

    /** Se ejecuta al pulsar espacio. */
    private Runnable alPulsarEspacio;

    /** Nota que esta sonando por el raton, o -1 si ninguna. */
    private int notaDelRaton;

    public Lienzo(Escena escena) {
        this.escena = escena;
        this.alRedimensionar = null;
        this.alPulsarEspacio = null;
        this.sintetizador = null;
        this.notaDelRaton = -1;

        setPreferredSize(new Dimension(escena.getAnchura(), escena.getAltura()));
        setDoubleBuffered(true);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                atenderRedimension();
            }
        });
        anadirRaton();
        // Hay que poder recibir el foco para escuchar el teclado. Se pide al
        // hacer clic en el piano, asi las flechas y el espacio no le pelean el
        // teclado a la lista de canciones ni a los sliders.
        setFocusable(true);
        anadirTeclado();
        new Timer(REFRESCO_MS, e -> repaint()).start();
    }

    /** Que hacer al pulsar espacio: normalmente pausar o reanudar. */
    public void setAlPulsarEspacio(Runnable accion) {
        this.alPulsarEspacio = accion;
    }

    private void anadirTeclado() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (Lienzo.this.alPulsarEspacio != null) {
                        Lienzo.this.alPulsarEspacio.run();
                    }
                } else if (Lienzo.this.sintetizador != null) {
                    Lienzo.this.sintetizador.pulsarTecla(e.getKeyCode());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (Lienzo.this.sintetizador != null) {
                    Lienzo.this.sintetizador.soltarTecla(e.getKeyCode());
                }
            }
        });
    }

    public void setSintetizador(SintetizadorTeclado sintetizador) {
        this.sintetizador = sintetizador;
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

    /**
     * Pulsar y arrastrar sobre las teclas. Saber en que tecla se ha hecho clic
     * sale gratis de tenerlas como formas: forma.contains(punto).
     */
    private void anadirRaton() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                tocar(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                soltarRaton();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                soltarRaton();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                tocar(e.getX(), e.getY());
            }
        });
    }

    private void tocar(int x, int y) {
        Piano piano = this.escena.getPiano();
        if (piano == null || this.sintetizador == null) {
            return;
        }
        Tecla tecla = piano.teclaEn(x, y);
        int nota = tecla == null ? -1 : tecla.getNumeroNota();
        // Al arrastrar solo se cambia de nota cuando se entra en otra tecla
        if (nota != this.notaDelRaton) {
            soltarRaton();
            if (nota >= 0) {
                this.sintetizador.encender(nota);
                this.notaDelRaton = nota;
            }
        }
    }

    private void soltarRaton() {
        if (this.notaDelRaton >= 0) {
            this.sintetizador.apagar(this.notaDelRaton);
            this.notaDelRaton = -1;
        }
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
