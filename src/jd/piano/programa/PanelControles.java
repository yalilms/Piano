package jd.piano.programa;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Barra de transporte: reproducir y pausar, parar, barra de progreso con salto
 * al pinchar, y control de tempo.
 */
public class PanelControles extends JPanel {

    private static final int PASOS_BARRA = 1000;
    private static final int REFRESCO_MS = 100;

    private final Reproductor reproductor;
    private final JButton botonPausa;
    private final JSlider barra;
    private final JSlider tempo;
    private final JLabel tiempo;
    private final JLabel etiquetaTempo;
    private final JLabel contador;

    /** Muestra anterior, para calcular las notas por segundo. */
    private int notasAntes;
    private long instanteAntes;
    private double notasPorSegundo;

    /**
     * Distingue mover la barra por codigo de moverla el usuario. Sin esto, el
     * temporizador que la actualiza dispararia una busqueda en cada tick y la
     * cancion no avanzaria nunca.
     */
    private boolean moviendoPorCodigo;

    public PanelControles(Reproductor reproductor, Runnable alCapturar) {
        this.reproductor = reproductor;
        this.moviendoPorCodigo = false;
        this.notasAntes = 0;
        this.instanteAntes = System.currentTimeMillis();
        this.notasPorSegundo = 0;

        this.botonPausa = new JButton("Pausa");
        this.botonPausa.addActionListener(e -> {
            reproductor.alternarPausa();
            actualizarBoton();
        });
        JButton botonParar = new JButton("Parar");
        botonParar.addActionListener(e -> {
            reproductor.parar();
            actualizarBoton();
        });

        this.barra = new JSlider(0, PASOS_BARRA, 0);
        this.barra.addChangeListener(e -> {
            if (!this.moviendoPorCodigo && this.reproductor.hayCancion()) {
                long destino = (long) (this.reproductor.getDuracionUs()
                        * (this.barra.getValue() / (double) PASOS_BARRA));
                this.reproductor.buscar(destino);
            }
        });

        this.tiempo = new JLabel("0:00 / 0:00");
        this.tiempo.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        this.contador = new JLabel("0 notas");
        this.contador.setToolTipText("Notas que han sonado y notas por segundo");

        // 25 a 300 en centesimas: de un cuarto de velocidad al triple
        this.tempo = new JSlider(25, 300, 100);
        this.tempo.setPreferredSize(new Dimension(140, this.tempo.getPreferredSize().height));
        this.etiquetaTempo = new JLabel("x1,00");
        this.tempo.addChangeListener(e -> {
            float factor = this.tempo.getValue() / 100f;
            this.reproductor.setFactorTempo(factor);
            this.etiquetaTempo.setText(String.format("x%.2f", factor));
        });

        JButton botonCaptura = new JButton("Captura");
        botonCaptura.setToolTipText("Guarda lo que se ve como PNG en capturas/");
        botonCaptura.addActionListener(e -> alCapturar.run());

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        izquierda.add(this.botonPausa);
        izquierda.add(botonParar);
        izquierda.add(botonCaptura);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        derecha.add(this.contador);
        derecha.add(this.tiempo);
        derecha.add(new JLabel("Tempo"));
        derecha.add(this.tempo);
        derecha.add(this.etiquetaTempo);
        derecha.add(Box.createHorizontalStrut(4));

        setLayout(new BorderLayout(6, 0));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        add(izquierda, BorderLayout.WEST);
        add(this.barra, BorderLayout.CENTER);
        add(derecha, BorderLayout.EAST);

        new Timer(REFRESCO_MS, e -> refrescar()).start();
    }

    private void refrescar() {
        actualizarBoton();
        long duracion = this.reproductor.getDuracionUs();
        long posicion = this.reproductor.getPosicionUs();

        if (duracion > 0 && !this.barra.getValueIsAdjusting()) {
            this.moviendoPorCodigo = true;
            this.barra.setValue((int) (PASOS_BARRA * posicion / (double) duracion));
            this.moviendoPorCodigo = false;
        }
        this.tiempo.setText(comoReloj(posicion) + " / " + comoReloj(duracion));
        actualizarContador();
    }

    /**
     * Notas por segundo suavizadas. Sin suavizar, con el refresco de 100 ms el
     * numero salta demasiado para poder leerlo.
     */
    private void actualizarContador() {
        int notas = this.reproductor.getNotasTocadas();
        long ahora = System.currentTimeMillis();
        long transcurrido = ahora - this.instanteAntes;
        if (transcurrido >= 250) {
            double instantaneas = 1000.0 * (notas - this.notasAntes) / transcurrido;
            this.notasPorSegundo = this.notasPorSegundo * 0.6 + instantaneas * 0.4;
            this.notasAntes = notas;
            this.instanteAntes = ahora;
        }
        this.contador.setText(String.format("%d notas - %.0f/s", notas,
                Math.max(0, this.notasPorSegundo)));
    }

    private void actualizarBoton() {
        this.botonPausa.setText(this.reproductor.estaSonando() ? "Pausa" : "Reproducir");
    }

    private static String comoReloj(long microsegundos) {
        long segundos = microsegundos / 1_000_000;
        return String.format("%d:%02d", segundos / 60, segundos % 60);
    }
}
