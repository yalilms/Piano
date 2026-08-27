package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
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
 * La ventana del programa. Tiene cuatro modos de uso, que se eligen en el
 * menu: ver una cancion, verla por canales, aprender a tocarla, o tocar libre
 * sin ninguna cancion.
 */
public class VentanaPrincipal extends JFrame implements BarraDeMenu.Acciones {

    private static final int NOTA_INICIAL = 24;
    private static final int NOTA_FINAL = 108;
    private static final int MARGEN = 48;

    /** Hueco que se deja bajo el teclado para la tapa y su reflejo. */
    private static final int ALTO_TAPA_LIBRE = 46;

    private final Reproductor reproductor;
    private final SintetizadorTeclado sintetizador;
    private final Escena escena;
    private final Lienzo lienzo;
    private final PanelCanales panelCanales;
    private final PanelControles panelControles;
    private final Bateria bateria;
    private final BarraDeMenu menu;
    private final DefaultListModel<File> modeloCanciones;
    private final JList<File> listaCanciones;
    private final JComboBox<String> selectorInstrumento;
    private final JLabel etiquetaInfo;

    private ModoDeUso modo;
    private String rutaActual;
    private Cascada cascada;
    private ModoAprendizaje aprendizaje;
    private RastroLibre rastro;

    public VentanaPrincipal(List<String> canciones) {
        super("Piano MIDI");

        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = (int) (pantalla.width * 0.88);
        int alto = (int) (pantalla.height * 0.78);

        this.modo = ModoDeUso.VER_CANCION;
        this.rutaActual = null;
        this.cascada = null;
        this.aprendizaje = null;
        this.rastro = null;

        this.reproductor = new Reproductor();
        this.sintetizador = new SintetizadorTeclado();
        this.sintetizador.abrir();
        // Un solo dueno del sintetizador: el reproductor solo lo usa para
        // silenciar canales y para mandar los mensajes de la cancion.
        this.reproductor.setSintetizador(this.sintetizador.getSintetizador());
        this.panelCanales = new PanelCanales(this.reproductor);
        this.bateria = new Bateria();
        this.reproductor.setBateria(this.bateria);

        this.escena = new Escena(Math.max(400, ancho - 260), Math.max(300, alto - 140));
        this.lienzo = new Lienzo(this.escena);
        this.lienzo.setAlRedimensionar(this::recolocar);
        this.lienzo.setSintetizador(this.sintetizador);
        this.lienzo.setAlEspacio(this::atenderEspacio);

        Image fondo = cargarFondo();
        if (fondo != null) {
            this.escena.setFondo(fondo);
        }

        this.selectorInstrumento = new JComboBox<>(PanelCanales.nombresDeInstrumento());
        this.selectorInstrumento.setToolTipText("Con que instrumento tocas tu");
        this.selectorInstrumento.addActionListener(e -> {
            this.sintetizador.setPrograma(this.selectorInstrumento.getSelectedIndex());
            this.lienzo.requestFocusInWindow();
        });

        this.modeloCanciones = new DefaultListModel<>();
        for (String ruta : canciones) {
            this.modeloCanciones.addElement(new File(ruta));
        }
        this.listaCanciones = crearLista();
        this.etiquetaInfo = new JLabel(" ");
        this.etiquetaInfo.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        this.menu = new BarraDeMenu(this::setModo, this::mostrarNombres, this);
        this.menu.marcarModo(this.modo);
        setJMenuBar(this.menu);

        this.panelControles = new PanelControles(this.reproductor, this::capturar);

        setLayout(new BorderLayout());
        add(crearLateral(), BorderLayout.WEST);
        add(this.lienzo, BorderLayout.CENTER);
        add(this.panelControles, BorderLayout.SOUTH);

        habilitarArrastrarYSoltar();
        vigilarElFinal();
        cerrarBienAlSalir();

        setIconImages(Iconos.deLaAplicacion());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(ancho, alto);
        setLocationRelativeTo(null);
    }

    private JPanel crearLateral() {
        JPanel tocasCon = new JPanel(new BorderLayout(4, 0));
        tocasCon.add(new JLabel(" Tocas con "), BorderLayout.WEST);
        tocasCon.add(this.selectorInstrumento, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.add(this.etiquetaInfo, BorderLayout.NORTH);
        abajo.add(new JScrollPane(this.panelCanales), BorderLayout.CENTER);
        abajo.setPreferredSize(new Dimension(270, 250));

        JPanel lateral = new JPanel(new BorderLayout(0, 6));
        lateral.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        lateral.setPreferredSize(new Dimension(270, 100));
        lateral.add(tocasCon, BorderLayout.NORTH);
        lateral.add(new JScrollPane(this.listaCanciones), BorderLayout.CENTER);
        lateral.add(abajo, BorderLayout.SOUTH);
        return lateral;
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
                    // Elegir cancion estando en el modo libre saca de el, que es
                    // lo que uno espera al pinchar en la lista
                    if (!this.modo.necesitaCancion()) {
                        this.modo = ModoDeUso.VER_CANCION;
                        this.menu.marcarModo(this.modo);
                    }
                    cargar(elegida.getPath());
                }
            }
        });
        return lista;
    }

    // ---------- modos de uso ----------

    /** Cambia de modo de uso y rehace lo que haga falta. */
    public void setModo(ModoDeUso nuevo) {
        if (nuevo == null || nuevo == this.modo) {
            return;
        }
        this.modo = nuevo;
        this.menu.marcarModo(nuevo);
        if (!nuevo.necesitaCancion()) {
            prepararLibre();
        } else if (this.rutaActual != null) {
            cargar(this.rutaActual);
        } else if (!this.modeloCanciones.isEmpty()) {
            this.listaCanciones.setSelectedIndex(0);
        } else {
            prepararLibre();
        }
        this.lienzo.requestFocusInWindow();
    }

    public ModoDeUso getModo() {
        return this.modo;
    }

    /** Modo de tocar libre: sin cancion, el teclado entero para el usuario. */
    private void prepararLibre() {
        this.reproductor.cerrar();
        this.rutaActual = null;
        this.cascada = null;
        this.aprendizaje = null;

        Piano piano = new PianoSencillo(NOTA_INICIAL, NOTA_FINAL, MedidasTeclado.NORMAL);
        this.escena.setPiano(piano);
        this.escena.quitarSuperpuestas();
        this.rastro = new RastroLibre();
        this.escena.anadirSuperpuesta(this.rastro);
        this.escena.anadirSuperpuesta(new Particulas());
        this.sintetizador.conectar(piano);
        this.reproductor.conectar(piano);

        this.bateria.construir(List.of());
        this.panelCanales.mostrarLibre();
        this.panelControles.setVisible(false);
        this.listaCanciones.clearSelection();
        setTitle("Piano MIDI - tocar libre");
        this.etiquetaInfo.setText("<html><b>Tocar libre</b><br>"
                + "<i>Clic en el piano y toca con Z-M y Q-U.<br>"
                + "El espacio hace de pedal.</i></html>");
        recolocar();
    }

    /** Monta el piano para esa cancion segun el modo y la pone a sonar. */
    private void cargar(String ruta) {
        try {
            this.reproductor.cerrar();
            this.rutaActual = ruta;
            this.rastro = null;
            this.panelControles.setVisible(true);

            AnalisisMidi analisis = AnalisisMidi.de(ruta);
            Piano piano = crearPiano(analisis);
            this.escena.setPiano(piano);
            this.escena.quitarSuperpuestas();
            montarCapas(ruta, analisis);

            recolocar();
            this.sintetizador.conectar(piano);
            this.reproductor.conectar(piano);
            this.reproductor.cargar(ruta);
            this.panelCanales.mostrar(analisis);
            this.reproductor.reproducir();

            // Se activa despues de cargar, que es cuando puede adelantar la
            // intro hasta la primera nota que hay que tocar
            if (this.aprendizaje != null) {
                this.aprendizaje.setActivo(true);
            }

            setTitle("Piano MIDI - " + new File(ruta).getName());
            this.etiquetaInfo.setText("<html><b>" + this.modo.getNombre() + "</b><br>"
                    + analisis.getTotalNotas() + " notas, "
                    + analisis.getCanalesUsados().size() + " canales<br>"
                    + "<i>" + this.modo.getExplicacion() + "</i></html>");
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this,
                    "No se ha podido abrir la cancion:\n" + error.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Piano crearPiano(AnalisisMidi analisis) {
        Piano piano;
        if (this.modo.esPorCanales()) {
            List<Integer> canales = analisis.getCanalesUsados();
            piano = new MultiPiano(canales, NOTA_INICIAL, NOTA_FINAL,
                    layoutPara(canales.size()));
        } else {
            piano = new PianoSencillo(NOTA_INICIAL, NOTA_FINAL, MedidasTeclado.NORMAL);
        }
        return piano;
    }

    private void montarCapas(String ruta, AnalisisMidi analisis) throws Exception {
        this.cascada = null;
        this.aprendizaje = null;

        // Las notas cayendo solo caben con un teclado: en el modo por canales
        // van pegados y no hay franja por la que caer.
        if (this.modo.tieneCascada()) {
            Partitura partitura = Partitura.de(ruta);
            this.cascada = new Cascada(partitura, this.reproductor);
            this.escena.anadirSuperpuesta(this.cascada);

            if (this.modo == ModoDeUso.APRENDER) {
                // Vigila un solo canal: exigir los trece a la vez no habria
                // manera de tocarlo
                int canal = ModoAprendizaje.canalDeMelodia(partitura);
                this.aprendizaje = new ModoAprendizaje(partitura, this.reproductor,
                        canal);
                this.escena.anadirSuperpuesta(this.aprendizaje);
            }
        }
        this.bateria.construir(analisis.getPercusionUsada());
        if (!this.bateria.estaVacia()) {
            this.escena.anadirSuperpuesta(this.bateria);
        }
        this.escena.anadirSuperpuesta(new Particulas());
    }

    // ---------- colocacion ----------

    private LayoutTeclados layoutPara(int cuantosTeclados) {
        return LayoutTeclados.calcular(cuantosTeclados,
                Piano.contarBlancas(NOTA_INICIAL, NOTA_FINAL),
                this.escena.getAnchura() - 2 * MARGEN,
                this.escena.getAltura() - 2 * MARGEN);
    }

    /** Recalcula escala y posicion para el tamano y el modo actuales. */
    private void recolocar() {
        Piano piano = this.escena.getPiano();
        if (piano == null) {
            return;
        }
        if (piano instanceof MultiPiano mp) {
            mp.setLayout(layoutPara(mp.getCanales().size()));
        } else {
            // El teclado tambien tiene que caber: a escala fija son 1250 px
            piano.setMedidas(layoutPara(1).getMedidas());
        }

        // Con cascada o con rastro el teclado se baja, para dejar sitio arriba
        boolean hayFranja = this.cascada != null || this.rastro != null;
        int y = hayFranja
                ? this.escena.getAltura() - piano.getAltura() - ALTO_TAPA_LIBRE
                : (this.escena.getAltura() - piano.getAltura()) / 2;
        piano.setPosicion((this.escena.getAnchura() - piano.getAnchura()) / 2, y);

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
        if (this.rastro != null) {
            this.rastro.setYTope(arriba);
        }
        this.escena.prepararCaches();
    }

    // ---------- acciones del menu ----------

    @Override
    public void abrir() {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(
                new FileNameExtensionFilter("Ficheros MIDI", "mid", "midi"));
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            anadirYSeleccionar(selector.getSelectedFile());
        }
    }

    @Override
    public void alternarPausa() {
        if (this.modo.necesitaCancion()) {
            this.reproductor.alternarPausa();
        }
    }

    @Override
    public void parar() {
        this.reproductor.parar();
    }

    @Override
    public void siguiente() {
        int cuantas = this.modeloCanciones.size();
        if (cuantas == 0) {
            return;
        }
        int siguiente = (this.listaCanciones.getSelectedIndex() + 1) % cuantas;
        this.listaCanciones.setSelectedIndex(siguiente);
        this.listaCanciones.ensureIndexIsVisible(siguiente);
    }

    @Override
    public void capturar() {
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

    @Override
    public void ayuda() {
        JOptionPane.showMessageDialog(this, BarraDeMenu.textoDeAyuda(),
                "Controles", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarNombres(boolean ver) {
        Estilo.setMostrarNombres(ver);
        // Las teclas en reposo estan horneadas en la imagen cacheada
        this.escena.invalidarCaches();
        this.escena.prepararCaches();
    }

    /**
     * La barra espaciadora: pausa cuando hay cancion, y pedal de resonancia en
     * el modo libre, donde no hay nada que pausar.
     */
    private void atenderEspacio(boolean pulsada) {
        if (this.modo.necesitaCancion()) {
            if (pulsada) {
                this.reproductor.alternarPausa();
            }
        } else {
            this.sintetizador.setSostenido(pulsada);
        }
    }

    // ---------- canciones ----------

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
                            if (f.isFile() && esMidi(f)) {
                                anadirYSeleccionar(f);
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

    private static boolean esMidi(File f) {
        String nombre = f.getName().toLowerCase();
        return nombre.endsWith(".mid") || nombre.endsWith(".midi");
    }

    private void anadirYSeleccionar(File fichero) {
        if (!contiene(fichero)) {
            this.modeloCanciones.addElement(fichero);
        }
        this.listaCanciones.setSelectedValue(fichero, true);
    }

    private boolean contiene(File fichero) {
        boolean encontrado = false;
        for (int i = 0; i < this.modeloCanciones.size() && !encontrado; i++) {
            encontrado = this.modeloCanciones.get(i).getAbsolutePath()
                    .equals(fichero.getAbsolutePath());
        }
        return encontrado;
    }

    /**
     * Cuando la cancion se acaba, pasa a la siguiente, o se para limpiamente si
     * el encadenado esta desmarcado. En el modo libre no hay nada que vigilar.
     */
    private void vigilarElFinal() {
        new Timer(400, e -> {
            if (this.modo.necesitaCancion() && this.rutaActual != null
                    && this.reproductor.haTerminado()) {
                if (this.menu.seEncadena()) {
                    siguiente();
                } else {
                    this.reproductor.parar();
                }
            }
        }).start();
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

    /**
     * Arranca con la primera cancion, o directamente en el modo libre si no hay
     * ninguna: asi el programa sirve para algo aunque falte la carpeta.
     */
    public void arrancar() {
        setVisible(true);
        if (this.modeloCanciones.isEmpty()) {
            this.modo = ModoDeUso.TOCAR_LIBRE;
            this.menu.marcarModo(this.modo);
            prepararLibre();
        } else {
            this.listaCanciones.setSelectedIndex(0);
        }
        this.lienzo.requestFocusInWindow();
    }
}
