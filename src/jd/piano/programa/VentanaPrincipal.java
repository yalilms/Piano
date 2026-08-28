package jd.piano.programa;

import jd.piano.instrumentos.InstrumentoVisual;
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
import java.awt.CardLayout;
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

    /** La0 a Do8: las 88 teclas de un piano de cola. */
    private static final int NOTA_INICIAL = 21;
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
    private final JComboBox<TipoInstrumento> selectorVerComo;
    private final JLabel etiquetaInfo;
    private final PantallaInicio pantallaInicio;
    private final JPanel tarjetas;
    private final CardLayout cambiador;

    private ModoDeUso modo;
    private String rutaActual;
    /** El conjunto en marcha, o null si el modo no reparte canales. */
    private Conjunto conjunto;
    private Cascada cascada;
    private ModoAprendizaje aprendizaje;
    private RastroLibre rastro;

    public VentanaPrincipal(List<String> canciones) {
        super("Piano MIDI");

        // El tamano de la ultima vez, si cabe en esta pantalla: quien lo dejo
        // grande en un monitor y abre en un portatil no quiere media ventana
        // fuera de la pantalla
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = Math.min(pantalla.width,
                Preferencias.getEntero(Preferencias.ANCHO,
                        (int) (pantalla.width * 0.88)));
        int alto = Math.min(pantalla.height,
                Preferencias.getEntero(Preferencias.ALTO,
                        (int) (pantalla.height * 0.78)));
        ancho = Math.max(900, ancho);
        alto = Math.max(560, alto);

        this.modo = Preferencias.getEnum(ModoDeUso.class, Preferencias.MODO,
                ModoDeUso.VER_CANCION);
        this.rutaActual = null;
        this.conjunto = null;
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
        this.selectorInstrumento.setSelectedIndex(Math.max(0, Math.min(
                this.selectorInstrumento.getItemCount() - 1,
                Preferencias.getEntero(Preferencias.TOCAS_CON, 0))));
        this.sintetizador.setPrograma(this.selectorInstrumento.getSelectedIndex());
        this.selectorInstrumento.addActionListener(e -> {
            int elegido = this.selectorInstrumento.getSelectedIndex();
            this.sintetizador.setPrograma(elegido);
            Preferencias.setEntero(Preferencias.TOCAS_CON, elegido);
            this.lienzo.requestFocusInWindow();
        });

        // Con que instrumento se ve la cancion. Cambiarlo recarga la actual.
        this.selectorVerComo = new JComboBox<>(TipoInstrumento.values());
        this.selectorVerComo.setToolTipText("Con que instrumento se ve la cancion");
        this.selectorVerComo.setSelectedItem(Preferencias.getEnum(
                TipoInstrumento.class, Preferencias.VER_COMO,
                TipoInstrumento.TECLADO));
        this.selectorVerComo.addActionListener(e -> {
            Preferencias.setEnum(Preferencias.VER_COMO, getTipoElegido());
            if (this.rutaActual != null && this.modo.eligeInstrumento()) {
                cargar(this.rutaActual);
            } else if (!this.modo.necesitaCancion()) {
                prepararLibre();
            }
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
        // El menu ya arranca con lo guardado; hay que pasarlo al estilo, que es
        // quien de verdad decide si se pintan los nombres
        Estilo.setMostrarNombres(this.menu.seVenLosNombres());
        setJMenuBar(this.menu);

        this.panelControles = new PanelControles(this.reproductor, this::capturar,
                this::cambioDeTono);

        JPanel vistaPiano = new JPanel(new BorderLayout());
        vistaPiano.add(crearLateral(), BorderLayout.WEST);
        vistaPiano.add(this.lienzo, BorderLayout.CENTER);
        vistaPiano.add(this.panelControles, BorderLayout.SOUTH);

        // Dos tarjetas: la pantalla de inicio a pantalla completa, o el piano.
        // Antes se cargaba y sonaba una cancion al abrir sin preguntar.
        this.pantallaInicio = new PantallaInicio(this::elegirDesdeElInicio);
        if (fondo != null) {
            this.pantallaInicio.setFondo(fondo);
        }
        this.cambiador = new CardLayout();
        this.tarjetas = new JPanel(this.cambiador);
        this.tarjetas.add(this.pantallaInicio, "inicio");
        this.tarjetas.add(vistaPiano, "piano");

        setLayout(new BorderLayout());
        add(this.tarjetas, BorderLayout.CENTER);

        habilitarArrastrarYSoltar();
        vigilarElFinal();
        cerrarBienAlSalir();

        setIconImages(Iconos.deLaAplicacion());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(ancho, alto);
        setLocationRelativeTo(null);
    }

    private JPanel crearLateral() {
        JPanel verComo = new JPanel(new BorderLayout(4, 0));
        verComo.add(new JLabel(" Ver como "), BorderLayout.WEST);
        verComo.add(this.selectorVerComo, BorderLayout.CENTER);

        JPanel tocasCon = new JPanel(new BorderLayout(4, 0));
        tocasCon.add(new JLabel(" Tocas con "), BorderLayout.WEST);
        tocasCon.add(this.selectorInstrumento, BorderLayout.CENTER);

        JPanel arriba = new JPanel(new BorderLayout(0, 4));
        arriba.add(verComo, BorderLayout.NORTH);
        arriba.add(tocasCon, BorderLayout.SOUTH);

        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.add(this.etiquetaInfo, BorderLayout.NORTH);
        abajo.add(new JScrollPane(this.panelCanales), BorderLayout.CENTER);
        abajo.setPreferredSize(new Dimension(270, 250));

        JPanel lateral = new JPanel(new BorderLayout(0, 6));
        lateral.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        lateral.setPreferredSize(new Dimension(270, 100));
        lateral.add(arriba, BorderLayout.NORTH);
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

    // ---------- pantalla de inicio ----------

    /** Vuelve a la pantalla de inicio y deja de sonar lo que hubiera. */
    @Override
    public void inicio() {
        this.reproductor.parar();
        this.cambiador.show(this.tarjetas, "inicio");
        setTitle("Piano MIDI");
        this.pantallaInicio.requestFocusInWindow();
    }

    /**
     * Lo que se elige en la pantalla de inicio. El modo libre entra directo; los
     * que necesitan cancion pasan al piano con la lista lista para elegir, sin
     * poner nada a sonar por su cuenta.
     */
    private void elegirDesdeElInicio(ModoDeUso elegido) {
        this.modo = elegido;
        this.menu.marcarModo(elegido);
        Preferencias.setEnum(Preferencias.MODO, elegido);
        if (!elegido.necesitaCancion()) {
            prepararLibre();
            this.lienzo.requestFocusInWindow();
        } else if (this.rutaActual != null) {
            cargar(this.rutaActual);
        } else {
            esperarQueElijanCancion();
        }
    }

    /** Piano vacio y la lista esperando: nada suena hasta que se elige. */
    private void esperarQueElijanCancion() {
        this.conjunto = null;
        this.selectorVerComo.setEnabled(this.modo.eligeInstrumento());
        this.escena.setInstrumento(null);
        this.escena.quitarSuperpuestas();
        this.escena.setMensaje("Elige una cancion de la lista");
        this.cascada = null;
        this.aprendizaje = null;
        this.rastro = null;
        this.panelControles.setVisible(false);
        this.panelCanales.setConjunto(null, null);
        this.panelCanales.mostrarAviso("Aqui saldran los canales de la cancion.");
        this.etiquetaInfo.setText("<html><b>" + this.modo.getNombre() + "</b><br>"
                + "<i>" + this.modo.getExplicacion() + "</i></html>");
        mostrarElPiano();
        setTitle("Piano MIDI - " + this.modo.getNombre());
        this.listaCanciones.requestFocusInWindow();
    }

    // ---------- modos de uso ----------

    /** Cambia de modo de uso y rehace lo que haga falta. */
    public void setModo(ModoDeUso nuevo) {
        if (nuevo == null) {
            return;
        }
        // Pedir el modo en el que ya se esta tambien tiene que llevar al piano:
        // si no, desde la pantalla de inicio Ctrl+1 no hacia nada.
        boolean cambia = nuevo != this.modo;
        this.modo = nuevo;
        this.menu.marcarModo(nuevo);
        Preferencias.setEnum(Preferencias.MODO, nuevo);

        if (!cambia && this.escena.getInstrumento() != null) {
            mostrarElPiano();
        } else if (!nuevo.necesitaCancion()) {
            prepararLibre();
        } else if (this.rutaActual != null) {
            cargar(this.rutaActual);
        } else {
            esperarQueElijanCancion();
        }
        this.lienzo.requestFocusInWindow();
    }

    private void mostrarElPiano() {
        this.cambiador.show(this.tarjetas, "piano");
    }

    /**
     * El tono ha cambiado. No hay que recargar nada: el reproductor transpone
     * al vuelo y la cascada pregunta por la nota ya transpuesta. Lo unico que
     * hay que rehacer es el informe, porque lo que cabe en el instrumento
     * cambia: una linea de bajo subida una octava si entra en una guitarra.
     */
    private void cambioDeTono() {
        if (this.rutaActual != null) {
            refrescarInforme();
        }
        this.lienzo.requestFocusInWindow();
    }

    /** Rehace el texto del panel para la cancion y el instrumento de ahora. */
    private void refrescarInforme() {
        InstrumentoVisual instrumento = this.escena.getInstrumento();
        if (this.rutaActual == null || instrumento == null) {
            return;
        }
        try {
            this.etiquetaInfo.setText(informe(
                    AnalisisMidi.de(this.rutaActual), instrumento, this.rutaActual));
        } catch (Exception error) {
            // se deja el informe que hubiera
        }
    }

    /**
     * Un canal se ha mandado a otro instrumento. No se recarga la cancion: los
     * paneles cambian de tamano, asi que se recoloca y se rehacen las imagenes
     * cacheadas, pero lo que suena sigue sonando.
     */
    private void reasignado() {
        recolocar();
        if (this.rutaActual != null && this.conjunto != null) {
            this.etiquetaInfo.setText(informeDelConjunto(this.rutaActual));
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
        this.conjunto = null;
        this.selectorVerComo.setEnabled(true);

        InstrumentoVisual instrumento = getTipoElegido().crear();
        this.escena.setInstrumento(instrumento);
        this.escena.quitarSuperpuestas();
        this.rastro = new RastroLibre();
        this.escena.anadirSuperpuesta(this.rastro);
        this.escena.anadirSuperpuesta(new Particulas());
        this.sintetizador.conectar(instrumento);
        this.reproductor.conectar(instrumento);

        this.bateria.construir(List.of());
        this.panelCanales.setConjunto(null, null);
        this.panelCanales.mostrarLibre();
        this.panelControles.setVisible(false);
        this.listaCanciones.clearSelection();
        mostrarElPiano();
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
            InstrumentoVisual instrumento = crearInstrumento(analisis);
            this.escena.setInstrumento(instrumento);
            this.escena.quitarSuperpuestas();
            montarCapas(ruta, analisis, instrumento);

            recolocar();
            this.sintetizador.conectar(instrumento);
            this.reproductor.conectar(instrumento);
            this.reproductor.cargar(ruta);
            this.panelCanales.setConjunto(this.conjunto, this::reasignado);
            this.panelCanales.mostrar(analisis);
            this.reproductor.reproducir();

            // Se activa despues de cargar, que es cuando puede adelantar la
            // intro hasta la primera nota que hay que tocar
            if (this.aprendizaje != null) {
                this.aprendizaje.setActivo(true);
            }

            mostrarElPiano();
            setTitle("Piano MIDI - " + new File(ruta).getName());
            this.etiquetaInfo.setText(informe(analisis, instrumento, ruta));
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this,
                    "No se ha podido abrir la cancion:\n" + error.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * El modo por canales siempre es una rejilla de teclados; en los demas
     * manda el instrumento elegido en "Ver como".
     */
    private InstrumentoVisual crearInstrumento(AnalisisMidi analisis) {
        this.conjunto = null;
        InstrumentoVisual instrumento;
        if (this.modo.esPorCanales()) {
            List<Integer> canales = analisis.getCanalesUsados();
            instrumento = new MultiPiano(canales, NOTA_INICIAL, NOTA_FINAL,
                    layoutPara(canales.size()));
        } else if (this.modo.esConjunto()) {
            this.conjunto = Conjunto.para(analisis);
            instrumento = this.conjunto;
        } else {
            instrumento = getTipoElegido().crear();
        }
        // El selector solo manda donde de verdad se le hace caso
        this.selectorVerComo.setEnabled(this.modo.eligeInstrumento());
        return instrumento;
    }

    private TipoInstrumento getTipoElegido() {
        Object elegido = this.selectorVerComo.getSelectedItem();
        return elegido instanceof TipoInstrumento tipo ? tipo : TipoInstrumento.TECLADO;
    }

    private void montarCapas(String ruta, AnalisisMidi analisis,
                             InstrumentoVisual instrumento) throws Exception {
        this.cascada = null;
        this.aprendizaje = null;

        // Las notas cayendo solo caben con un teclado: en el modo por canales
        // van pegados y no hay franja por la que caer. Y el propio instrumento
        // puede decir que no le pegan, como el circulo cromatico.
        if (this.modo.tieneCascada() && instrumento.admiteCascada()) {
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
        InstrumentoVisual instrumento = this.escena.getInstrumento();
        if (instrumento == null) {
            return;
        }
        // La bateria se queda con una franja arriba, y hay que apartarla antes
        // de colocar nada: un instrumento centrado que ocupe casi todo el alto,
        // como el circulo cromatico, se metia por debajo de los parches
        int arriba = MARGEN / 2;
        if (!this.bateria.estaVacia()) {
            this.bateria.setZona(new Rectangle(arriba, arriba,
                    this.escena.getAnchura() - 2 * arriba, 0));
            arriba += this.bateria.getAlturaNecesaria() + MARGEN / 3;
        }

        // Cada instrumento sabe reescalarse: el teclado cambia el tamano de
        // tecla, el mastil el largo de los trastes, el xilofono el de las barras
        int ancho = this.escena.getAnchura() - 2 * MARGEN;
        boolean hayFranja = this.cascada != null || this.rastro != null;
        int y;
        if (hayFranja) {
            // Pegado abajo, que las notas tienen que caer hasta el
            instrumento.ajustarA(ancho, this.escena.getAltura() - 2 * MARGEN);
            y = this.escena.getAltura() - instrumento.getAltura() - ALTO_TAPA_LIBRE;
        } else {
            // Centrado en lo que queda libre por debajo de la bateria
            int libre = this.escena.getAltura() - arriba - MARGEN / 2;
            instrumento.ajustarA(ancho, libre);
            y = arriba + (libre - instrumento.getAltura()) / 2;
        }
        instrumento.setPosicion(
                (this.escena.getAnchura() - instrumento.getAnchura()) / 2, y);

        if (this.cascada != null) {
            this.cascada.setZona(arriba, y - 4 - arriba);
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
            File carpeta = Recursos.carpetaDeCapturas();
            if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
                throw new IOException(
                        "no puedo crear la carpeta " + carpeta.getPath());
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

    /**
     * Lo que se cuenta en el panel: el modo, la cancion, y cuanto de ella cabe
     * en el instrumento elegido. Una guitarra no llega a las notas graves de un
     * piano, y mas vale decirlo que dejar media cancion sin pintar en silencio.
     */
    private String informe(AnalisisMidi analisis, InstrumentoVisual instrumento,
                           String ruta) {
        StringBuilder texto = new StringBuilder("<html><b>");
        texto.append(this.modo.getNombre()).append("</b><br>")
                .append(analisis.getTotalNotas()).append(" notas, ")
                .append(analisis.getCanalesUsados().size()).append(" canales<br>");

        if (this.modo.esConjunto() && this.conjunto != null) {
            texto.append(this.conjunto.getCuantosInstrumentos())
                    .append(" instrumentos: ")
                    .append(this.conjunto.getNombre()).append("<br>");
            try {
                double cobertura = this.conjunto.coberturaDe(Partitura.de(ruta),
                        this.reproductor.getTransposicion());
                texto.append("Se ven el ").append(Math.round(cobertura))
                        .append("% de las notas<br>");
            } catch (Exception error) {
                // sin cobertura que informar
            }
        } else if (!this.modo.esPorCanales()) {
            try {
                double cobertura = getTipoElegido().coberturaDe(Partitura.de(ruta),
                        this.reproductor.getTransposicion());
                // Sin articulo, que "en el Guitarra" no concuerda
                texto.append(instrumento.getNombre()).append(": se ven el ")
                        .append(Math.round(cobertura)).append("% de las notas");
                if (cobertura < 90) {
                    texto.append(" <b>(el resto se sale de su rango)</b>");
                }
                texto.append("<br>");
            } catch (Exception error) {
                // sin cobertura que informar
            }
        }
        texto.append(textoDelTono());
        texto.append("<i>").append(this.modo.getExplicacion()).append("</i></html>");
        return texto.toString();
    }

    /**
     * El informe cuando solo ha cambiado el reparto de canales. Se rehace
     * aparte porque el reparto se puede tocar sin recargar la cancion.
     */
    private String informeDelConjunto(String ruta) {
        StringBuilder texto = new StringBuilder("<html><b>");
        texto.append(this.modo.getNombre()).append("</b><br>")
                .append(this.conjunto.getCuantosInstrumentos())
                .append(" instrumentos: ")
                .append(this.conjunto.getNombre()).append("<br>");
        try {
            double cobertura = this.conjunto.coberturaDe(Partitura.de(ruta),
                    this.reproductor.getTransposicion());
            texto.append("Se ven el ").append(Math.round(cobertura))
                    .append("% de las notas<br>");
        } catch (Exception error) {
            // sin cobertura que informar
        }
        texto.append(textoDelTono());
        texto.append("<i>").append(this.modo.getExplicacion()).append("</i></html>");
        return texto.toString();
    }

    /** Aviso de que la cancion no esta en su tono original. */
    private String textoDelTono() {
        int semitonos = this.reproductor.getTransposicion();
        if (semitonos == 0) {
            return "";
        }
        return "Tono: " + (semitonos > 0 ? "+" : "") + semitonos
                + (Math.abs(semitonos) == 1 ? " semitono" : " semitonos") + "<br>";
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
                VentanaPrincipal.this.guardarElTamano();
                Preferencias.guardar();
                VentanaPrincipal.this.reproductor.cerrar();
                VentanaPrincipal.this.sintetizador.cerrar();
            }
        });
    }

    /**
     * Guarda el tamano de la ventana. Maximizada se guarda el tamano que tenia
     * antes, que es el que hay que restaurar al desmaximizar.
     */
    private void guardarElTamano() {
        boolean maximizada = (getExtendedState() & MAXIMIZED_BOTH) != 0;
        Preferencias.setBooleano(Preferencias.MAXIMIZADA, maximizada);
        if (!maximizada) {
            Preferencias.setEntero(Preferencias.ANCHO, getWidth());
            Preferencias.setEntero(Preferencias.ALTO, getHeight());
        }
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
     * Abre en la pantalla de inicio. No se carga ni suena nada hasta que se
     * elige que hacer.
     */
    public void arrancar() {
        if (Preferencias.getBooleano(Preferencias.MAXIMIZADA, false)) {
            setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        }
        setVisible(true);
        this.cambiador.show(this.tarjetas, "inicio");
    }
}
