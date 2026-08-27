package jd.piano.programa;

import jd.piano.teclas.Estilo;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * El menu de la ventana. Reune lo que antes andaba suelto por el panel
 * lateral (el desplegable de modo, las casillas) y le pone atajos.
 */
public class BarraDeMenu extends JMenuBar {

    private final Map<ModoDeUso, JRadioButtonMenuItem> opcionesDeModo =
            new EnumMap<>(ModoDeUso.class);
    private final JCheckBoxMenuItem verNombres;
    private final JCheckBoxMenuItem encadenar;

    /**
     * @param alCambiarModo  que hacer cuando se elige otro modo de uso
     * @param alVerNombres   cuando se encienden o apagan los nombres de nota
     * @param acciones       lo que puede hacer la ventana desde el menu
     */
    public BarraDeMenu(Consumer<ModoDeUso> alCambiarModo,
                       Consumer<Boolean> alVerNombres,
                       Acciones acciones) {

        JMenu menuModo = new JMenu("Modo");
        menuModo.setMnemonic(KeyEvent.VK_M);
        menuModo.add(item("Volver al inicio", KeyEvent.VK_0, acciones::inicio));
        menuModo.addSeparator();
        ButtonGroup grupo = new ButtonGroup();
        int numero = 1;
        for (ModoDeUso modo : ModoDeUso.values()) {
            JRadioButtonMenuItem opcion = new JRadioButtonMenuItem(modo.getNombre());
            opcion.setToolTipText(modo.getExplicacion());
            // Ctrl+1 a Ctrl+4 para cambiar de modo sin soltar el teclado
            opcion.setAccelerator(KeyStroke.getKeyStroke(
                    KeyEvent.VK_0 + numero, InputEvent.CTRL_DOWN_MASK));
            opcion.addActionListener(e -> alCambiarModo.accept(modo));
            grupo.add(opcion);
            menuModo.add(opcion);
            this.opcionesDeModo.put(modo, opcion);
            numero++;
        }
        add(menuModo);

        JMenu menuCancion = new JMenu("Cancion");
        menuCancion.setMnemonic(KeyEvent.VK_C);
        menuCancion.add(item("Abrir un .mid...", KeyEvent.VK_O, acciones::abrir));
        menuCancion.addSeparator();
        menuCancion.add(item("Reproducir o pausar", KeyEvent.VK_P, acciones::alternarPausa));
        menuCancion.add(item("Parar", KeyEvent.VK_T, acciones::parar));
        menuCancion.add(item("Siguiente", KeyEvent.VK_N, acciones::siguiente));
        add(menuCancion);

        JMenu menuVer = new JMenu("Ver");
        menuVer.setMnemonic(KeyEvent.VK_V);
        this.verNombres = new JCheckBoxMenuItem("Nombres de nota", true);
        this.verNombres.setToolTipText("Ensena la nota en cada tecla blanca");
        this.verNombres.addActionListener(e ->
                alVerNombres.accept(this.verNombres.isSelected()));
        menuVer.add(this.verNombres);

        this.encadenar = new JCheckBoxMenuItem("Encadenar canciones", true);
        this.encadenar.setToolTipText("Al acabar, pasa a la siguiente de la lista");
        menuVer.add(this.encadenar);
        menuVer.addSeparator();
        menuVer.add(item("Guardar captura", KeyEvent.VK_G, acciones::capturar));
        add(menuVer);

        JMenu menuAyuda = new JMenu("Ayuda");
        menuAyuda.setMnemonic(KeyEvent.VK_A);
        menuAyuda.add(item("Controles", KeyEvent.VK_F1, acciones::ayuda));
        add(menuAyuda);
    }

    private static JMenuItem item(String texto, int tecla, Runnable accion) {
        JMenuItem item = new JMenuItem(texto);
        if (tecla == KeyEvent.VK_F1) {
            item.setAccelerator(KeyStroke.getKeyStroke(tecla, 0));
        } else {
            item.setAccelerator(KeyStroke.getKeyStroke(tecla,
                    InputEvent.CTRL_DOWN_MASK));
        }
        item.addActionListener(e -> accion.run());
        return item;
    }

    /** Marca un modo sin disparar su accion, para reflejar el estado real. */
    public void marcarModo(ModoDeUso modo) {
        JRadioButtonMenuItem opcion = this.opcionesDeModo.get(modo);
        if (opcion != null && !opcion.isSelected()) {
            opcion.setSelected(true);
        }
    }

    public boolean seEncadena() {
        return this.encadenar.isSelected();
    }

    public boolean seVenLosNombres() {
        return this.verNombres.isSelected();
    }

    /** Texto de ayuda de los controles, para el menu Ayuda. */
    public static String textoDeAyuda() {
        return "<html><h3>Controles</h3>"
                + "<p><b>Clic en el piano</b> le da el foco al teclado.</p>"
                + "<table>"
                + "<tr><td>Z S X D C V G B H N J M</td>"
                + "<td>una octava: abajo las blancas, encima las negras</td></tr>"
                + "<tr><td>Q 2 W 3 E R 5 T 6 Y 7 U I</td>"
                + "<td>la octava siguiente</td></tr>"
                + "<tr><td>Flechas izquierda y derecha</td>"
                + "<td>bajar o subir una octava</td></tr>"
                + "<tr><td>Espacio</td>"
                + "<td>pausa, o pedal de resonancia en el modo libre</td></tr>"
                + "<tr><td>Clic y arrastre en las teclas</td>"
                + "<td>tocar con el raton</td></tr>"
                + "<tr><td>Ctrl+1 a Ctrl+4</td><td>cambiar de modo</td></tr>"
                + "</table>"
                + "<p>El Do central sale marcado en rojo: es el <b>"
                + Estilo.etiquetaDe(Estilo.DO_CENTRAL) + "</b>.</p>"
                + "<p>Lo que tocas suena por el canal 16, el que menos usan los"
                + " ficheros MIDI, para no cambiarle el instrumento a ninguna"
                + " pista de la cancion.</p></html>";
    }

    /** Lo que el menu puede pedirle a la ventana. */
    public interface Acciones {
        void inicio();

        void abrir();

        void alternarPausa();

        void parar();

        void siguiente();

        void capturar();

        void ayuda();
    }
}
