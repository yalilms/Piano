package jd.piano.programa;

import jd.piano.teclas.Estilo;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

/**
 * Un renglon por canal de la cancion: su color, el instrumento que suena y un
 * interruptor para silenciarlo o aislarlo.
 *
 * Sirve de leyenda, que era lo que faltaba para que los colores del
 * multipiano significaran algo, y de mesa de mezclas.
 */
public class PanelCanales extends JPanel {

    private final Reproductor reproductor;
    private final List<JCheckBox> interruptores = new ArrayList<>();
    private final List<Integer> canales = new ArrayList<>();

    /** Puesto solo en el modo de varios instrumentos. Null en los demas. */
    private Conjunto conjunto;
    private Runnable alReasignar;

    public PanelCanales(Reproductor reproductor) {
        this.reproductor = reproductor;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
    }

    /**
     * En el modo de varios instrumentos, cada canal lleva ademas un desplegable
     * para mandarlo a otro instrumento. Hay que llamarlo antes de mostrar, y con
     * null para volver a las filas de una sola linea.
     */
    public void setConjunto(Conjunto conjunto, Runnable alReasignar) {
        this.conjunto = conjunto;
        this.alReasignar = alReasignar;
    }

    /** Rehace la lista para la cancion que se acaba de cargar. */
    public void mostrar(AnalisisMidi analisis) {
        removeAll();
        this.interruptores.clear();
        this.canales.clear();

        List<Integer> usados = analisis.getCanalesUsados();
        for (int canal : usados) {
            this.canales.add(canal);
            add(crearFila(canal, analisis.getInstrumento(canal)));
        }
        if (analisis.getTotalPercusion() > 0) {
            add(crearFilaPercusion(analisis));
        }
        if (usados.size() > 1) {
            add(crearBotonTodos());
        }
        revalidate();
        repaint();
    }

    /** Los 128 nombres de General MIDI, para los desplegables. */
    static String[] nombresDeInstrumento() {
        String[] nombres = new String[InstrumentosGM.cuantos()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = InstrumentosGM.nombre(i);
        }
        return nombres;
    }

    /** Un aviso en vez de la lista de canales. */
    public void mostrarAviso(String texto) {
        removeAll();
        this.interruptores.clear();
        this.canales.clear();
        JLabel aviso = new JLabel("<html><i>" + texto + "</i></html>");
        aviso.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        add(aviso);
        revalidate();
        repaint();
    }

    /** En el modo libre no hay canales que mezclar: se explica y ya. */
    public void mostrarLibre() {
        removeAll();
        this.interruptores.clear();
        this.canales.clear();
        JLabel aviso = new JLabel("<html><i>Sin cancion.<br>"
                + "El teclado es todo tuyo.</i></html>");
        aviso.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        add(aviso);
        revalidate();
        repaint();
    }

    private JPanel crearFila(int canal, String instrumento) {
        JCheckBox interruptor = new JCheckBox();
        interruptor.setSelected(true);
        interruptor.setToolTipText("Silenciar o reactivar este canal");
        interruptor.addActionListener(e ->
                this.reproductor.setSilenciado(canal, !interruptor.isSelected()));
        this.interruptores.add(interruptor);

        // Desplegable con los 128 instrumentos: se puede cambiar lo que suena
        // en esta pista sin tocar el fichero
        JComboBox<String> selector = new JComboBox<>(nombresDeInstrumento());
        selector.setSelectedItem(instrumento);
        selector.setToolTipText("Canal " + (canal + 1)
                + ": cambia el instrumento de esta pista");
        selector.addActionListener(e ->
                this.reproductor.setProgramaDeCanal(canal, selector.getSelectedIndex()));

        JButton solo = new JButton("solo");
        solo.setMargin(new java.awt.Insets(0, 4, 0, 4));
        solo.setToolTipText("Dejar sonando solo este canal");
        solo.addActionListener(e -> aislar(canal));

        JPanel fila = new JPanel(new BorderLayout(4, 0));
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        izquierda.add(interruptor);
        izquierda.add(new Muestra(Estilo.colorDeCanal(canal)));
        fila.add(izquierda, BorderLayout.WEST);
        fila.add(selector, BorderLayout.CENTER);
        fila.add(solo, BorderLayout.EAST);

        if (this.conjunto == null) {
            return fila;
        }
        // Dos lineas: arriba que suena, abajo donde se ve. No caben en una sola
        JPanel doble = new JPanel(new BorderLayout(0, 1));
        doble.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        doble.add(fila, BorderLayout.NORTH);
        doble.add(crearSelectorVisual(canal), BorderLayout.SOUTH);
        return doble;
    }

    /** Desplegable de "se ve en", para mover un canal a otro instrumento. */
    private JPanel crearSelectorVisual(int canal) {
        JComboBox<TipoInstrumento> visual =
                new JComboBox<>(TipoInstrumento.values());
        TipoInstrumento actual = this.conjunto.getTipoDe(canal);
        if (actual != null) {
            visual.setSelectedItem(actual);
        }
        visual.setToolTipText("Canal " + (canal + 1)
                + ": en que instrumento se dibuja esta pista");
        visual.addActionListener(e -> {
            Object elegido = visual.getSelectedItem();
            if (elegido instanceof TipoInstrumento tipo) {
                this.conjunto.asignar(canal, tipo);
                if (this.alReasignar != null) {
                    this.alReasignar.run();
                }
            }
        });

        JLabel etiqueta = new JLabel("  se ve en ");
        etiqueta.setFont(etiqueta.getFont().deriveFont(11f));
        JPanel fila = new JPanel(new BorderLayout(2, 0));
        fila.add(etiqueta, BorderLayout.WEST);
        fila.add(visual, BorderLayout.CENTER);
        return fila;
    }

    /**
     * La bateria va aparte: en el canal 10 la nota es una pieza, no una altura,
     * asi que no tiene instrumento que elegir, solo se puede silenciar.
     */
    private JPanel crearFilaPercusion(AnalisisMidi analisis) {
        int canal = 9;
        JCheckBox interruptor = new JCheckBox();
        interruptor.setSelected(true);
        interruptor.setToolTipText("Silenciar o reactivar la bateria");
        interruptor.addActionListener(e ->
                this.reproductor.setSilenciado(canal, !interruptor.isSelected()));
        this.interruptores.add(interruptor);
        this.canales.add(canal);

        JLabel nombre = new JLabel("Bateria ("
                + analisis.getPercusionUsada().size() + " piezas)");
        nombre.setToolTipText(analisis.getTotalPercusion() + " golpes en la cancion");

        JPanel fila = new JPanel(new BorderLayout(4, 0));
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        izquierda.add(interruptor);
        izquierda.add(new Muestra(Estilo.colorDeCanal(canal)));
        fila.add(izquierda, BorderLayout.WEST);
        fila.add(nombre, BorderLayout.CENTER);
        return fila;
    }

    private JButton crearBotonTodos() {
        JButton todos = new JButton("Oir todos");
        todos.addActionListener(e -> {
            for (int i = 0; i < this.canales.size(); i++) {
                this.reproductor.setSilenciado(this.canales.get(i), false);
                this.interruptores.get(i).setSelected(true);
            }
        });
        return todos;
    }

    private void aislar(int canal) {
        for (int i = 0; i < this.canales.size(); i++) {
            boolean silenciar = this.canales.get(i) != canal;
            this.reproductor.setSilenciado(this.canales.get(i), silenciar);
            this.interruptores.get(i).setSelected(!silenciar);
        }
    }

    /** Cuadradito de color, para saber que canal es cada uno en el piano. */
    private static class Muestra extends JPanel {
        private final Color color;

        Muestra(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(12, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(this.color);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(this.color.darker());
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }
}
