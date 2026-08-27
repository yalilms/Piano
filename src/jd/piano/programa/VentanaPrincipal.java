package jd.piano.programa;

import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

/**
 * La ventana del programa: lista de canciones, piano y barra de transporte.
 * Sustituye al menu por terminal que habia antes.
 */
public class VentanaPrincipal extends JFrame {

    private static final int NOTA_INICIAL = 24;
    private static final int NOTA_FINAL = 108;
    private static final int MARGEN = 48;

    private static final String MODO_SENCILLO = "Piano sencillo";
    private static final String MODO_MULTI = "Un teclado por canal";

    private final Reproductor reproductor;
    private final Escena escena;
    private final Lienzo lienzo;
    private final DefaultListModel<File> modeloCanciones;
    private final JList<File> listaCanciones;
    private final JComboBox<String> selectorModo;
    private final JLabel etiquetaInfo;
    private String rutaActual;

    public VentanaPrincipal(List<String> canciones) {
        super("Piano MIDI");

        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = (int) (pantalla.width * 0.88);
        int alto = (int) (pantalla.height * 0.78);

        this.reproductor = new Reproductor();
        this.rutaActual = null;
        this.escena = new Escena(Math.max(400, ancho - 240), Math.max(300, alto - 120));
        this.lienzo = new Lienzo(this.escena);
        this.lienzo.setAlRedimensionar(this::recolocar);

        Image fondo = cargarFondo();
        if (fondo != null) {
            this.escena.setFondo(fondo);
        }

        this.modeloCanciones = new DefaultListModel<>();
        for (String ruta : canciones) {
            this.modeloCanciones.addElement(new File(ruta));
        }
        this.listaCanciones = crearLista();
        this.selectorModo = new JComboBox<>(new String[]{MODO_SENCILLO, MODO_MULTI});
        this.selectorModo.addActionListener(e -> {
            if (this.rutaActual != null) {
                cargar(this.rutaActual);
            }
        });

        this.etiquetaInfo = new JLabel("Elige una cancion");
        this.etiquetaInfo.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel lateral = new JPanel(new BorderLayout(0, 6));
        lateral.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        lateral.setPreferredSize(new Dimension(230, 100));
        lateral.add(this.selectorModo, BorderLayout.NORTH);
        lateral.add(new JScrollPane(this.listaCanciones), BorderLayout.CENTER);
        lateral.add(this.etiquetaInfo, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(lateral, BorderLayout.WEST);
        add(this.lienzo, BorderLayout.CENTER);
        add(new PanelControles(this.reproductor), BorderLayout.SOUTH);

        habilitarArrastrarYSoltar();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(ancho, alto);
        setLocationRelativeTo(null);
    }

    private JList<File> crearLista() {
        JList<File> lista = new JList<>(this.modeloCanciones);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer((componente, valor, indice, seleccionada, foco) -> {
            JLabel etiqueta = new JLabel(" " + valor.getName());
            etiqueta.setOpaque(true);
            if (seleccionada) {
                etiqueta.setBackground(componente.getSelectionBackground());
                etiqueta.setForeground(componente.getSelectionForeground());
            }
            return etiqueta;
        });
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                File elegida = lista.getSelectedValue();
                if (elegida != null) {
                    cargar(elegida.getPath());
                }
            }
        });
        return lista;
    }

    /** Arrastrar un .mid sobre la ventana lo anade a la lista y lo reproduce. */
    private void habilitarArrastrarYSoltar() {
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport soporte) {
                return soporte.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport soporte) {
                boolean alguno = false;
                if (canImport(soporte)) {
                    try {
                        Transferable t = soporte.getTransferable();
                        Object datos = t.getTransferData(DataFlavor.javaFileListFlavor);
                        for (Object o : (List<?>) datos) {
                            File f = (File) o;
                            if (f.isFile() && f.getName().toLowerCase().endsWith(".mid")) {
                                if (!contiene(f)) {
                                    modeloCanciones.addElement(f);
                                }
                                listaCanciones.setSelectedValue(f, true);
                                alguno = true;
                            }
                        }
                    } catch (Exception error) {
                        alguno = false;
                    }
                }
                return alguno;
            }
        });
    }

    private boolean contiene(File fichero) {
        boolean encontrado = false;
        for (int i = 0; i < this.modeloCanciones.size() && !encontrado; i++) {
            encontrado = this.modeloCanciones.get(i).getAbsolutePath()
                    .equals(fichero.getAbsolutePath());
        }
        return encontrado;
    }

    /** Monta el piano para esa cancion y la pone a sonar. */
    private void cargar(String ruta) {
        try {
            this.reproductor.cerrar();
            this.rutaActual = ruta;

            AnalisisMidi analisis = AnalisisMidi.de(ruta);
            boolean multi = MODO_MULTI.equals(this.selectorModo.getSelectedItem());
            Piano piano;
            if (multi) {
                List<Integer> canales = analisis.getCanalesUsados();
                piano = new MultiPiano(canales, NOTA_INICIAL, NOTA_FINAL,
                        layoutPara(canales.size()));
            } else {
                piano = new PianoSencillo(NOTA_INICIAL, NOTA_FINAL, MedidasTeclado.NORMAL);
            }

            this.escena.setPiano(piano);
            recolocar();

            this.reproductor.conectar(piano);
            this.reproductor.cargar(ruta);
            this.reproductor.reproducir();

            setTitle("Piano MIDI - " + new File(ruta).getName());
            this.etiquetaInfo.setText("<html>"
                    + analisis.getCanalesUsados().size() + " canales<br>"
                    + analisis.getTotalNotas() + " notas<br>notas "
                    + analisis.getNotaMinima() + " a " + analisis.getNotaMaxima()
                    + "</html>");
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this,
                    "No se ha podido abrir la cancion:\n" + error.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private LayoutTeclados layoutPara(int cuantosCanales) {
        return LayoutTeclados.calcular(cuantosCanales,
                Piano.contarBlancas(NOTA_INICIAL, NOTA_FINAL),
                this.escena.getAnchura() - 2 * MARGEN,
                this.escena.getAltura() - 2 * MARGEN);
    }

    /** Recalcula escala y posicion para el tamano actual del lienzo. */
    private void recolocar() {
        Piano piano = this.escena.getPiano();
        if (piano == null) {
            return;
        }
        if (piano instanceof MultiPiano mp) {
            mp.setLayout(layoutPara(mp.getCanales().size()));
        } else {
            // El piano sencillo tambien tiene que caber: a escala fija 1,0 son
            // 1250 px y se salia por la derecha en ventanas mas estrechas.
            piano.setMedidas(layoutPara(1).getMedidas());
        }
        piano.setPosicion((this.escena.getAnchura() - piano.getAnchura()) / 2,
                (this.escena.getAltura() - piano.getAltura()) / 2);
        this.escena.prepararCaches();
    }

    private static Image cargarFondo() {
        File file = new File("madera.jpg");
        Image imagen = null;
        if (file.exists()) {
            try {
                imagen = ImageIO.read(file);
            } catch (Exception error) {
                imagen = null;
            }
        }
        return imagen;
    }

    /** Deja seleccionada la primera cancion, si hay alguna. */
    public void arrancar() {
        setVisible(true);
        if (!this.modeloCanciones.isEmpty()) {
            this.listaCanciones.setSelectedIndex(0);
        }
    }
}
