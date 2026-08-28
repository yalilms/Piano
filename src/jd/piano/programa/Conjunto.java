package jd.piano.programa;

import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.PiezaPulsable;
import jd.piano.teclas.Estilo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Varios instrumentos a la vez, uno encima de otro, y cada canal de la cancion
 * repartido al que le toca.
 *
 * Es un InstrumentoVisual que por dentro tiene otros, asi que la escena, el
 * reproductor y las notas cayendo no se enteran: le preguntan donde va una nota
 * y el conjunto pregunta al instrumento del canal que sea.
 *
 * El reparto se hace solo, mirando el PROGRAM_CHANGE de cada canal: las pistas
 * de guitarra van al mastil, las de bajo al bajo, y el resto al teclado. Y se
 * puede cambiar a mano, porque adivinar por el numero de programa acierta
 * bastante pero no siempre.
 *
 * Los canales del mismo tipo comparten instrumento en vez de tener uno cada
 * uno. Con trece canales harian falta trece paneles y no se veria ninguno; asi
 * salen tres o cuatro, y se distinguen por el color de la nota, que es el del
 * canal.
 */
public class Conjunto implements InstrumentoVisual {

    private static final int CANALES = 16;

    /** Hueco sobre cada instrumento para su rotulo, cuando hay sitio de sobra. */
    private static final int ALTO_ROTULO = 18;
    private static final int SEPARACION = 12;
    private static final int LADO_MUESTRA = 9;

    /** Familias de General MIDI. El programa dividido entre ocho da la familia. */
    private static final int FAMILIA_PERCUSION_CROMATICA = 1;
    private static final int FAMILIA_GUITARRA = 3;
    private static final int FAMILIA_BAJO = 4;
    private static final int FAMILIA_PERCUSIVA = 14;

    /**
     * Cuanto alto se lleva cada tipo al repartir. Un teclado necesita mas que
     * un mastil para que las teclas no salgan como alfileres.
     */
    private static final Map<TipoInstrumento, Double> PESOS =
            new EnumMap<>(TipoInstrumento.class);

    static {
        PESOS.put(TipoInstrumento.TECLADO, 1.45);
        PESOS.put(TipoInstrumento.GUITARRA, 1.0);
        PESOS.put(TipoInstrumento.BAJO, 0.8);
        PESOS.put(TipoInstrumento.XILOFONO, 1.05);
        PESOS.put(TipoInstrumento.CIRCULO, 1.3);
    }

    private final TipoInstrumento[] porCanal;
    private final List<Atril> atriles;

    private Point posicion;
    private int anchoDisponible;
    private int altoDisponible;

    /**
     * Lo que se reserva de verdad para los rotulos y los huecos. Se recorta
     * cuando el sitio aprieta: con cuatro instrumentos apilados, los rotulos y
     * las separaciones se llevan 120 px, y en un lienzo bajo eso era la
     * diferencia entre caber y salirse.
     */
    private int altoRotulo;
    private int separacion;

    public Conjunto() {
        this.porCanal = new TipoInstrumento[CANALES];
        this.atriles = new ArrayList<>();
        this.posicion = null;
        this.anchoDisponible = 900;
        this.altoDisponible = 460;
        this.altoRotulo = ALTO_ROTULO;
        this.separacion = SEPARACION;
    }

    /**
     * Reparte los canales de la cancion mirando que instrumento declara cada
     * uno. Si al canal no le cabe su instrumento por rango, se le da un
     * teclado: mas vale verlo en un piano que no verlo.
     */
    public static Conjunto para(AnalisisMidi analisis) {
        Conjunto conjunto = new Conjunto();
        for (int canal : analisis.getCanalesUsados()) {
            conjunto.porCanal[canal] = tipoPara(
                    analisis.getPrograma(canal),
                    analisis.getNotaMinimaDe(canal),
                    analisis.getNotaMaximaDe(canal));
        }
        conjunto.reconstruir();
        return conjunto;
    }

    /**
     * Que instrumento le pega a un canal. La familia dice la intencion y el
     * rango dice si se puede: una pista de guitarra que baja al Mi grave del
     * bajo no cabe en un mastil de guitarra, y ahi es mejor un teclado.
     */
    public static TipoInstrumento tipoPara(int programa, int notaMinima,
                                           int notaMaxima) {
        TipoInstrumento propuesta = switch (programa / 8) {
            case FAMILIA_GUITARRA -> TipoInstrumento.GUITARRA;
            case FAMILIA_BAJO -> TipoInstrumento.BAJO;
            case FAMILIA_PERCUSION_CROMATICA, FAMILIA_PERCUSIVA ->
                    TipoInstrumento.XILOFONO;
            default -> TipoInstrumento.TECLADO;
        };
        boolean cabe = notaMinima < 0
                || (notaMinima >= propuesta.getNotaMinima()
                        && notaMaxima <= propuesta.getNotaMaxima());
        return cabe ? propuesta : TipoInstrumento.TECLADO;
    }

    /** Con que instrumento se ve ese canal, o null si la cancion no lo usa. */
    public TipoInstrumento getTipoDe(int canal) {
        return canal >= 0 && canal < CANALES ? this.porCanal[canal] : null;
    }

    /** Los canales repartidos, en orden. */
    public List<Integer> getCanales() {
        List<Integer> lista = new ArrayList<>();
        for (int canal = 0; canal < CANALES; canal++) {
            if (this.porCanal[canal] != null) {
                lista.add(canal);
            }
        }
        return lista;
    }

    /**
     * Manda ese canal a otro instrumento. Hay que recolocar el conjunto
     * despues, porque los paneles cambian de tamano y de sitio.
     */
    public void asignar(int canal, TipoInstrumento tipo) {
        if (canal < 0 || canal >= CANALES || tipo == null
                || tipo == this.porCanal[canal]) {
            return;
        }
        soltarTodas();
        this.porCanal[canal] = tipo;
        reconstruir();
        ajustarA(this.anchoDisponible, this.altoDisponible);
        if (this.posicion != null) {
            setPosicion(this.posicion.x, this.posicion.y);
        }
    }

    /**
     * Monta un panel por cada tipo que se use, en el orden del enum para que la
     * pila no baile al cambiar una asignacion. Los que ya estaban se reutilizan,
     * asi lo que este sonando no se apaga al mover otro canal.
     */
    private void reconstruir() {
        List<Atril> anteriores = new ArrayList<>(this.atriles);
        this.atriles.clear();
        for (TipoInstrumento tipo : TipoInstrumento.values()) {
            List<Integer> suyos = new ArrayList<>();
            for (int canal = 0; canal < CANALES; canal++) {
                if (this.porCanal[canal] == tipo) {
                    suyos.add(canal);
                }
            }
            if (suyos.isEmpty()) {
                continue;
            }
            InstrumentoVisual instrumento = null;
            for (Atril viejo : anteriores) {
                if (viejo.tipo == tipo) {
                    instrumento = viejo.instrumento;
                }
            }
            if (instrumento == null) {
                instrumento = tipo.crear();
            }
            this.atriles.add(new Atril(tipo, instrumento, suyos));
        }
    }

    @Override
    public String getNombre() {
        StringBuilder nombre = new StringBuilder();
        for (Atril atril : this.atriles) {
            if (nombre.length() > 0) {
                nombre.append(" + ");
            }
            nombre.append(atril.tipo.getNombre());
        }
        return nombre.length() == 0 ? "Conjunto" : nombre.toString();
    }

    /** Cuantos instrumentos se estan viendo a la vez. */
    public int getCuantosInstrumentos() {
        return this.atriles.size();
    }

    /**
     * Que porcentaje de las notas de la cancion se ve de verdad. Aqui no vale
     * el rango de un solo instrumento: cada canal va al suyo, asi que se
     * pregunta nota a nota si hay sitio donde pintarla.
     */
    public double coberturaDe(Partitura partitura) {
        return coberturaDe(partitura, 0);
    }

    /** Lo mismo contando la transposicion. */
    public double coberturaDe(Partitura partitura, int semitonos) {
        long total = 0;
        long caben = 0;
        for (NotaMidi nota : partitura.getNotas()) {
            total++;
            if (piezaDe(nota.canal(), nota.nota() + semitonos) != null) {
                caben++;
            }
        }
        return total == 0 ? 100.0 : 100.0 * caben / total;
    }

    // ---------- reparto de notas ----------

    private Atril atrilDe(int canal) {
        TipoInstrumento tipo = getTipoDe(canal);
        Atril encontrado = null;
        for (Atril atril : this.atriles) {
            if (atril.tipo == tipo) {
                encontrado = atril;
            }
        }
        // Un canal que no venia en el analisis pero suena igual: se le da el
        // primer panel antes que perder la nota
        if (encontrado == null && !this.atriles.isEmpty()) {
            encontrado = this.atriles.get(0);
        }
        return encontrado;
    }

    @Override
    public PiezaPulsable piezaDe(int canal, int nota) {
        Atril atril = atrilDe(canal);
        return atril == null ? null : atril.instrumento.piezaDe(canal, nota);
    }

    @Override
    public boolean pulsar(int canal, int nota, int velocidad) {
        Atril atril = atrilDe(canal);
        return atril != null && atril.instrumento.pulsar(canal, nota, velocidad);
    }

    @Override
    public void soltar(int canal, int nota) {
        Atril atril = atrilDe(canal);
        if (atril != null) {
            atril.instrumento.soltar(canal, nota);
        }
    }

    @Override
    public Rectangle donde(int canal, int nota) {
        Atril atril = atrilDe(canal);
        return atril == null ? null : atril.instrumento.donde(canal, nota);
    }

    @Override
    public void soltarTodas() {
        for (Atril atril : this.atriles) {
            atril.instrumento.soltarTodas();
        }
    }

    @Override
    public void soltarCanal(int canal) {
        Atril atril = atrilDe(canal);
        if (atril != null) {
            atril.instrumento.soltarCanal(canal);
        }
    }

    @Override
    public PiezaPulsable piezaEn(int x, int y) {
        PiezaPulsable encontrada = null;
        for (Atril atril : this.atriles) {
            if (encontrada == null) {
                encontrada = atril.instrumento.piezaEn(x, y);
            }
        }
        return encontrada;
    }

    /**
     * Nunca. Los instrumentos van uno encima de otro, asi que la barra de una
     * nota del de abajo tendria que cruzar por encima del de arriba para
     * aterrizar donde le toca. Aqui lo que se ve es el reparto, no la caida.
     */
    @Override
    public boolean admiteCascada() {
        return false;
    }

    /**
     * Tampoco. Una pila de instrumentos no se apoya en una sola tapa, y la
     * franja oscura quedaba como una estanteria colgada en medio de la madera.
     */
    @Override
    public boolean tieneReflejo() {
        return false;
    }

    // ---------- colocacion ----------

    @Override
    public void ajustarA(int ancho, int alto) {
        this.anchoDisponible = ancho;
        this.altoDisponible = alto;
        if (this.atriles.isEmpty()) {
            return;
        }
        int cuantos = this.atriles.size();
        // Los rotulos y los huecos no pueden comerse mas de un tercio del sitio
        this.altoRotulo = Math.min(ALTO_ROTULO, alto / Math.max(1, cuantos * 3));
        this.separacion = Math.min(SEPARACION, this.altoRotulo);
        int gastado = cuantos * this.altoRotulo + (cuantos - 1) * this.separacion;
        int util = Math.max(0, alto - gastado);

        double pesoTotal = 0;
        for (Atril atril : this.atriles) {
            pesoTotal += peso(atril.tipo);
        }
        // Se reparte con division entera y el ultimo se lleva lo que sobra: con
        // redondeo, cuatro paneles podian sumar un pixel mas que el hueco
        int repartido = 0;
        for (int i = 0; i < cuantos; i++) {
            Atril atril = this.atriles.get(i);
            int suyo = i == cuantos - 1 ? util - repartido
                    : (int) (util * peso(atril.tipo) / pesoTotal);
            repartido += suyo;
            atril.instrumento.ajustarA(ancho, Math.max(0, suyo));
        }
    }

    private static double peso(TipoInstrumento tipo) {
        return PESOS.getOrDefault(tipo, 1.0);
    }

    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
        int ancho = getAnchura();
        int cursor = y;
        for (Atril atril : this.atriles) {
            cursor += this.altoRotulo;
            int suAncho = atril.instrumento.getAnchura();
            atril.instrumento.setPosicion(x + (ancho - suAncho) / 2, cursor);
            atril.yRotulo = cursor - 5;
            atril.xRotulo = x + (ancho - suAncho) / 2;
            cursor += atril.instrumento.getAltura() + this.separacion;
        }
    }

    @Override
    public Rectangle getLimites() {
        return this.posicion == null ? null
                : new Rectangle(this.posicion.x, this.posicion.y,
                        getAnchura(), getAltura());
    }

    @Override
    public int getAnchura() {
        int ancho = 0;
        for (Atril atril : this.atriles) {
            ancho = Math.max(ancho, atril.instrumento.getAnchura());
        }
        return ancho;
    }

    @Override
    public int getAltura() {
        int alto = 0;
        for (Atril atril : this.atriles) {
            alto += this.altoRotulo + atril.instrumento.getAltura()
                    + this.separacion;
        }
        return Math.max(0, alto - this.separacion);
    }

    // ---------- dibujado ----------

    @Override
    public void dibujar(Graphics2D g) {
        for (Atril atril : this.atriles) {
            atril.instrumento.dibujar(g);
            dibujarRotulo(g, atril);
        }
    }

    @Override
    public void dibujarEnReposo(Graphics2D g) {
        for (Atril atril : this.atriles) {
            atril.instrumento.dibujarEnReposo(g);
            dibujarRotulo(g, atril);
        }
    }

    @Override
    public void dibujarBrillos(Graphics2D g) {
        for (Atril atril : this.atriles) {
            atril.instrumento.dibujarBrillos(g);
        }
    }

    @Override
    public void anadirRegionesActivas(List<Rectangle> destino) {
        for (Atril atril : this.atriles) {
            atril.instrumento.anadirRegionesActivas(destino);
        }
    }

    /**
     * El nombre del instrumento y un cuadradito por cada canal que le toca, en
     * su color. Es la unica forma de saber que la mancha azul del mastil es la
     * pista de guitarra tercera y no otra.
     *
     * Se pinta tambien en dibujar y no solo en la imagen en reposo, porque cada
     * region sucia se repinta desde el escenario pelado y se lo llevaria.
     */
    private void dibujarRotulo(Graphics2D g, Atril atril) {
        if (this.posicion == null) {
            return;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        int x = atril.xRotulo;
        int y = atril.yRotulo;

        for (int canal : atril.canales) {
            g.setColor(Estilo.colorDeCanal(canal));
            g.fillRect(x, y - LADO_MUESTRA, LADO_MUESTRA, LADO_MUESTRA);
            g.setColor(new Color(0, 0, 0, 110));
            g.drawRect(x, y - LADO_MUESTRA, LADO_MUESTRA, LADO_MUESTRA);
            x += LADO_MUESTRA + 3;
        }
        x += 3;

        String texto = atril.tipo.getNombre() + "  -  " + textoCanales(atril.canales);
        // Sombra primero, que sobre madera clara el texto claro no se lee
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(texto, x + 1, y + 1);
        g.setColor(new Color(238, 228, 208, 225));
        g.drawString(texto, x, y);
    }

    private static String textoCanales(List<Integer> canales) {
        StringBuilder texto = new StringBuilder(
                canales.size() == 1 ? "canal " : "canales ");
        for (int i = 0; i < canales.size(); i++) {
            if (i > 0) {
                texto.append(i == canales.size() - 1 ? " y " : ", ");
            }
            // Los canales se cuentan del 1 al 16 para el que mira, no del 0
            texto.append(canales.get(i) + 1);
        }
        return texto.toString();
    }

    /** Un instrumento del conjunto y los canales que le tocan. */
    private static final class Atril {
        private final TipoInstrumento tipo;
        private final InstrumentoVisual instrumento;
        private final List<Integer> canales;
        private int xRotulo;
        private int yRotulo;

        Atril(TipoInstrumento tipo, InstrumentoVisual instrumento,
              List<Integer> canales) {
            this.tipo = tipo;
            this.instrumento = instrumento;
            this.canales = canales;
            this.xRotulo = 0;
            this.yRotulo = 0;
        }
    }
}
