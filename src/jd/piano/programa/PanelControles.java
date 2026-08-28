package jd.piano.programa;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Barra de transporte: reproducir y pausar, parar, barra de progreso con salto
 * al pinchar, tempo, tono y bucle para practicar un trozo.
 *
 * El texto del boton de bucle y el del tono se sacan del reproductor en cada
 * refresco en vez de guardarse aqui. Asi no puede pasar lo que ya paso con los
 * silencios de canal: que el control ensene una cosa y el estado sea otra.
 */
public class PanelControles extends JPanel {

    private static final int PASOS_BARRA = 1000;

    /**
     * A este ritmo se refresca la barra y se vigila el bucle. Con 100 ms el
     * bucle se pasaba de largo un decima cada vuelta, que se nota al practicar.
     */
    private static final int REFRESCO_MS = 50;

    private final Reproductor reproductor;
    private final JButton botonPausa;
    private final JSlider barra;
    private final JSlider tempo;
    private final JLabel tiempo;
    private final JLabel etiquetaTempo;
    private final JLabel contador;
    private final JSpinner tono;
    private final JButton botonBucle;
    private final JLabel etiquetaBucle;

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

    public PanelControles(Reproductor reproductor, Runnable alCapturar,
                          Runnable alCambiarTono) {
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

        // Tono: la cancion entera subida o bajada. Sirve para pasarla a algo
        // mas facil de tocar, y para meterla en el rango de un instrumento que
        // no le llega.
        this.tono = new JSpinner(new SpinnerNumberModel(0,
                -Reproductor.TONO_MAXIMO, Reproductor.TONO_MAXIMO, 1));
        this.tono.setPreferredSize(new Dimension(52,
                this.tono.getPreferredSize().height));
        this.tono.setToolTipText("Subir o bajar la cancion, en semitonos");
        this.tono.addChangeListener(e -> {
            this.reproductor.setTransposicion((Integer) this.tono.getValue());
            if (alCambiarTono != null) {
                alCambiarTono.run();
            }
        });

        // Bucle A-B con un solo boton: A, luego B, luego quitar
        this.botonBucle = new JButton("Bucle A-B");
        this.botonBucle.setToolTipText("Repetir un trozo: una vez para el "
                + "principio, otra para el final, otra para quitarlo");
        this.botonBucle.addActionListener(e -> {
            this.reproductor.marcarBucle();
            actualizarBucle();
        });
        this.etiquetaBucle = new JLabel(" ");
        this.etiquetaBucle.setToolTipText("El trozo que se esta repitiendo");

        JButton botonCaptura = new JButton("Captura");
        botonCaptura.setToolTipText("Guarda lo que se ve como PNG en capturas/");
        botonCaptura.addActionListener(e -> alCapturar.run());

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        izquierda.add(this.botonPausa);
        izquierda.add(botonParar);
        izquierda.add(this.botonBucle);
        izquierda.add(this.etiquetaBucle);
        izquierda.add(botonCaptura);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        derecha.add(this.contador);
        derecha.add(this.tiempo);
        derecha.add(new JLabel("Tono"));
        derecha.add(this.tono);
        derecha.add(new JLabel("Tempo"));
        derecha.add(this.tempo);
        derecha.add(this.etiquetaTempo);
        derecha.add(Box.createHorizontalStrut(4));

        setLayout(new BorderLayout(6, 0));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        add(izquierda, BorderLayout.WEST);
        add(this.barra, BorderLayout.CENTER);
        add(derecha, BorderLayout.EAST);

        actualizarBucle();
        new Timer(REFRESCO_MS, e -> refrescar()).start();
    }

    private void refrescar() {
        actualizarBoton();
        // Volver al principio del trozo si ya se ha pasado del final
        this.reproductor.atenderBucle();
        actualizarBucle();
        // El tono se puede cambiar por otro sitio que no sea este control (al
        // recuperar las preferencias, por ejemplo). Se sincroniza aqui para que
        // no pueda pasar lo de los silencios: el control diciendo una cosa y el
        // reproductor haciendo otra.
        sincronizarTono();
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

    /** El boton y el rotulo del bucle, sacados del estado del reproductor. */
    private void actualizarBucle() {
        if (this.reproductor.tieneBucle()) {
            this.botonBucle.setText("Quitar bucle");
            this.etiquetaBucle.setText(
                    comoReloj(this.reproductor.getBucleInicioUs()) + " - "
                            + comoReloj(this.reproductor.getBucleFinUs()));
        } else if (this.reproductor.esperaFinDeBucle()) {
            this.botonBucle.setText("Marcar el fin");
            this.etiquetaBucle.setText("desde "
                    + comoReloj(this.reproductor.getBucleInicioUs()));
        } else {
            this.botonBucle.setText("Bucle A-B");
            this.etiquetaBucle.setText(" ");
        }
    }

    /** Deja el control del tono donde diga el reproductor, sin dar el aviso. */
    public void sincronizarTono() {
        int actual = this.reproductor.getTransposicion();
        if (!this.tono.getValue().equals(actual)) {
            this.tono.setValue(actual);
        }
    }

    private static String comoReloj(long microsegundos) {
        long segundos = microsegundos / 1_000_000;
        return String.format("%d:%02d", segundos / 60, segundos % 60);
    }
}
