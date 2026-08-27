package jd.piano.programa;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.List;

/**
 * Arranque del programa. Ya no hay menu por terminal: todo se elige en la
 * ventana.
 *
 * Los mensajes de consola van sin acentos a proposito, porque la consola de
 * Windows no usa UTF-8 por defecto y saldrian caracteres raros.
 */
public class ProgramaSwing {

    public static void main(String[] args) {
        ponerAspecto();

        ListaCanciones lista = new ListaCanciones("canciones");
        List<String> canciones = lista.getCanciones();

        if (canciones.isEmpty()) {
            System.out.println("No hay canciones en la carpeta 'canciones'.");
            System.out.println("Lanza el programa desde la raiz del proyecto,");
            System.out.println("o arrastra un .mid sobre la ventana.");
        }

        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal(canciones);
            ventana.arrancar();
            if (canciones.isEmpty()) {
                JOptionPane.showMessageDialog(ventana,
                        "No hay canciones en la carpeta 'canciones'.\n"
                                + "Arrastra un fichero .mid sobre la ventana.",
                        "Sin canciones", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    /**
     * FlatLaf da a los botones y sliders un aspecto actual; el de serie de
     * Swing canta bastante al lado de un piano bien pintado. Si el JAR no esta
     * el programa sigue funcionando con el aspecto por defecto.
     */
    private static void ponerAspecto() {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception error) {
            System.out.println("Aviso: FlatLaf no disponible, aspecto por defecto.");
        }
    }
}
