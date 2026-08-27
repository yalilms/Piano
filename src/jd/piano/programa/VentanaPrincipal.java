package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * La ventana del programa: lista de canciones, piano y barra de transporte.
 * Sustituye al menu por terminal que habia antes.
 */
public class VentanaPrincipal extends JFrame {

    private static final int NOTA_INICIAL = 24;
    private static final int NOTA_FINAL = 108;
    private static final int MARGEN = 48;

    /** Hueco que se deja bajo el teclado para la tapa y su reflejo. */
    private static final int ALTO_TAPA_LIBRE = 46;

    private static final String MODO_SENCILLO = "Piano sencillo";
    private static final String MODO_MULTI = "Un teclado por canal";

    private final Reproductor reproductor;
    private final Escena escena;
    private final Lienzo lienzo;
    private final DefaultListModel<File> modeloCanciones;
    private final JList<File> listaCanciones;
    private final JComboBox<String> selectorModo;
    private final JLabel etiquetaInfo;
    private final SintetizadorTeclado sintetizador;
    private final PanelCanales panelCanales;
    private final JCheckBox encadenar;
    private final Bateria bateria;
    private final JComboBox<String> selectorInstrumento;
    private String rutaActual;
    private Cascada cascada;

    public VentanaPrincipal(List<String> canciones) {
        super("Piano MIDI");

        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = (int) (pantalla.width * 0.88);
        int alto = (int) (pantalla.height * 0.78);

        this.reproductor = new Reproductor();
        this.sintetizador = new SintetizadorTeclado();
        this.sintetizador.abrir();
        // Un solo dueno del sintetizador: el reproductor solo lo usa para
        // silenciar canales.
        this.reproductor.setSintetizador(this.sintetizador.getSintetizador());
        this.panelCanales = new PanelCanales(this.reproductor);
        this.bateria = new Bateria();
        this.reproductor.setBateria(this.bateria);

        this.rutaActual = null;
        this.cascada = null;
        this.escena = new Escena(Math.max(400, ancho - 240), Math.max(300, alto - 120));
        this.lienzo = new Lienzo(this.escena);
        this.lienzo.setAlRedimensionar(this::recolocar);
        this.lienzo.setSintetizador(this.sintetizador);
        this.lienzo.setAlPulsarEspacio(this.reproductor::alternarPausa);

        this.selectorInstrumento = new JComboBox<>(PanelCanales.nombresDeInstrumento());
        this.selectorInstrumento.setToolTipText("Con que instrumento tocas tu");
        this.selectorInstrumento.addActionListener(e -> {
            this.sintetizador.setPrograma(this.selectorInstrumento.getSelectedIndex());
            // Devolver el foco al piano, que si no hay que volver a hacer clic
            this.lienzo.requestFocusInWindow();
        });

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

        this.encadenar = new JCheckBox("Encadenar canciones", true);
        this.encadenar.setToolTipText("Al acabar, pasa a la siguiente de la lista");

        JCheckBox verNombres = new JCheckBox("Nombres de nota", true);
        verNombres.setToolTipText("Ensena la nota en cada tecla blanca");
        verNombres.addActionListener(e -> {
            Estilo.setMostrarNombres(verNombres.isSelected());
            // Las teclas en reposo estan horneadas en la imagen cacheada
            this.escena.invalidarCaches();
            this.escena.prepararCaches();
        });

        JPanel tocasCon = new JPanel(new BorderLayout(4, 0));
        tocasCon.add(new JLabel(" Tocas con "), BorderLayout.WEST);
        tocasCon.add(this.selectorInstrumento, BorderLayout.CENTER);

        JPanel casillas = new JPanel(new BorderLayout());
        casillas.add(verNombres, BorderLayout.NORTH);
        casillas.add(this.encadenar, BorderLayout.CENTER);
        casillas.add(tocasCon, BorderLayout.SOUTH);

        JPanel arriba = new JPanel(new BorderLayout(0, 4));
        arriba.add(this.selectorModo, BorderLayout.NORTH);
        arriba.add(casillas, BorderLayout.SOUTH);

        JPanel lateral = new JPanel(new BorderLayout(0, 6));
        lateral.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        lateral.setPreferredSize(new Dimension(270, 100));
        lateral.add(arriba, BorderLayout.NORTH);
        lateral.add(new JScrollPane(this.listaCanciones), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.add(this.etiquetaInfo, BorderLayout.NORTH);
        abajo.add(new JScrollPane(this.panelCanales), BorderLayout.CENTER);
        abajo.setPreferredSize(new Dimension(270, 260));
        lateral.add(abajo, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(lateral, BorderLayout.WEST);
        add(this.lienzo, BorderLayout.CENTER);
        add(new PanelControles(this.reproductor, this::guardarCaptura),
                BorderLayout.SOUTH);

        habilitarArrastrarYSoltar();
        vigilarElFinal();
        cerrarBienAlSalir();

        setIconImages(Iconos.deLaAplicacion());
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

    /**
     * Cuando la cancion se acaba, la ventana se quedaba en un callejon sin
     * salida: la barra al final y nada mas. Ahora pasa a la siguiente, o se
     * queda parada limpiamente si esta desmarcado el encadenado.
     */
    private void vigilarElFinal() {
        new Timer(400, e -> {
            if (this.rutaActual != null && this.reproductor.haTerminado()) {
                if (this.encadenar.isSelected()) {
                    siguienteCancion();
                } else {
                    this.reproductor.parar();
                }
            }
        }).start();
    }

    private void siguienteCancion() {
        int cuantas = this.modeloCanciones.size();
        if (cuantas == 0) {
            return;
        }
        int siguiente = (this.listaCanciones.getSelectedIndex() + 1) % cuantas;
        // setSelectedIndex dispara el listener, que ya carga y reproduce
        this.listaCanciones.setSelectedIndex(siguiente);
        this.listaCanciones.ensureIndexIsVisible(siguiente);
    }

    /** Suelta el secuenciador y el sintetizador al cerrar la ventana. */
    private void cerrarBienAlSalir() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                VentanaPrincipal.this.reproductor.cerrar();
                VentanaPrincipal.this.sintetizador.cerrar();
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

            // Las notas cayendo solo tienen sentido en el modo sencillo: en el
            // multi los teclados van pegados y no hay franja por la que caer.
            this.escena.quitarSuperpuestas();
            if (multi) {
                this.cascada = null;
            } else {
                this.cascada = new Cascada(Partitura.de(ruta), this.reproductor);
                this.escena.anadirSuperpuesta(this.cascada);
            }
            this.bateria.construir(analisis.getPercusionUsada());
            if (!this.bateria.estaVacia()) {
                this.escena.anadirSuperpuesta(this.bateria);
            }
            this.escena.anadirSuperpuesta(new Particulas());

            recolocar();

            this.sintetizador.conectar(piano);
            this.reproductor.conectar(piano);
            this.reproductor.cargar(ruta);
            this.panelCanales.mostrar(analisis);
            this.reproductor.reproducir();

            setTitle("Piano MIDI - " + new File(ruta).getName());
            this.etiquetaInfo.setText("<html>"
                    + analisis.getTotalNotas() + " notas, "
                    + analisis.getCanalesUsados().size() + " canales<br>"
                    + "<i>Clic en el piano y toca con Z-M y Q-U.<br>"
                    + "Flechas para cambiar de octava, espacio para pausar.</i>"
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
        // En modo sencillo el teclado se baja para dejar sitio a las notas
        int y = this.cascada != null
                ? this.escena.getAltura() - piano.getAltura() - ALTO_TAPA_LIBRE
                : (this.escena.getAltura() - piano.getAltura()) / 2;
        piano.setPosicion((this.escena.getAnchura() - piano.getAnchura()) / 2, y);

        // Los pads de bateria ocupan la franja de arriba y la cascada empieza
        // por debajo de ellos, para que no se pisen
        int arriba = MARGEN / 2;
        if (!this.bateria.estaVacia()) {
            this.bateria.setZona(new Rectangle(arriba, arriba,
                    this.escena.getAnchura() - 2 * arriba, 0));
            arriba += this.bateria.getAlturaNecesaria() + MARGEN / 3;
        }
        if (this.cascada != null) {
            this.cascada.setZona(arriba,
                    y - piano.getMedidas().getAlturaFieltro() - arriba);
        }
        this.escena.prepararCaches();
    }

    private static Image cargarFondo() {
        File file = Recursos.buscar("madera.jpg");
        Image imagen = null;
        if (file != null) {
            try {
                imagen = ImageIO.read(file);
            } catch (Exception error) {
                imagen = null;
            }
        }
        return imagen;
    }

    /** Guarda lo que se ve ahora mismo como PNG. */
    private void guardarCaptura() {
        try {
            File carpeta = new File("capturas");
            if (!carpeta.exists() && !carpeta.mkdirs()) {
                throw new IOException("no puedo crear la carpeta capturas");
            }
            String sello = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .format(LocalDateTime.now());
            File destino = new File(carpeta, "piano-" + sello + ".png");
            ImageIO.write(this.escena.aImagen(), "png", destino);
            this.etiquetaInfo.setText("Guardado en " + destino.getPath());
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this,
                    "No se ha podido guardar la captura:\n" + error.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Deja seleccionada la primera cancion, si hay alguna. */
    public void arrancar() {
        setVisible(true);
        this.lienzo.requestFocusInWindow();
        if (!this.modeloCanciones.isEmpty()) {
            this.listaCanciones.setSelectedIndex(0);
        }
    }
}
