package jd.piano.programa;

import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Un teclado por canal MIDI. Solo se montan los canales que la cancion usa de
 * verdad: medido sobre las canciones del proyecto, van de 2 a 14 y nunca 16,
 * asi que dibujar siempre los dieciseis desperdiciaba espacio y obligaba a
 * recortar el rango de notas.
 */
public class MultiPiano extends Piano {

    public static final int CANALES_MIDI = 16;

    /** Canal MIDI de cada teclado, en orden de dibujado. */
    private final List<Integer> canales;
    /** Indice del teclado de cada canal, o -1 si ese canal no se dibuja. */
    private final int[] indicePorCanal;
    private final List<PianoSencillo> pianos;

    private LayoutTeclados layout;

    public MultiPiano(List<Integer> canales, int ti, int tf, LayoutTeclados layout) {
        setTeclaInicial(ti);
        setTeclaFinal(tf);
        this.canales = new ArrayList<>(canales);
        this.layout = layout;
        this.indicePorCanal = new int[CANALES_MIDI];
        for (int c = 0; c < CANALES_MIDI; c++) {
            this.indicePorCanal[c] = -1;
        }
        this.pianos = new ArrayList<>();
        for (int i = 0; i < this.canales.size(); i++) {
            int canal = this.canales.get(i);
            if (canal >= 0 && canal < CANALES_MIDI) {
                this.indicePorCanal[canal] = i;
            }
            this.pianos.add(new PianoSencillo(ti, tf, layout.getMedidas()));
        }
    }

    public List<Integer> getCanales() {
        return new ArrayList<>(this.canales);
    }

    @Override
    public Tecla getTecla(int canal, int nota) {
        Tecla tecla = null;
        if (canal >= 0 && canal < CANALES_MIDI) {
            int indice = this.indicePorCanal[canal];
            if (indice >= 0) {
                tecla = this.pianos.get(indice).getTecla(0, nota);
            }
        }
        return tecla;
    }

    @Override
    public MedidasTeclado getMedidas() {
        return this.layout.getMedidas();
    }

    @Override
    public void setMedidas(MedidasTeclado medidas) {
        for (PianoSencillo piano : this.pianos) {
            piano.setMedidas(medidas);
        }
        if (getPosicion() != null) {
            setPosicion(getPosicion().x, getPosicion().y);
        }
    }

    /** Cambia la rejilla y la escala de golpe, por ejemplo al redimensionar. */
    public void setLayout(LayoutTeclados layout) {
        this.layout = layout;
        setMedidas(layout.getMedidas());
    }

    @Override
    public void setPosicion(int x, int y) {
        super.setPosicion(x, y);

        int anchoUno = anchuraDeUnTeclado();
        int altoUno = this.layout.getMedidas().getAlturaBlanca();
        int columnas = this.layout.getColumnas();

        for (int i = 0; i < this.pianos.size(); i++) {
            int fila = i / columnas;
            int columna = i % columnas;
            this.pianos.get(i).setPosicion(
                    x + columna * (anchoUno + this.layout.getEspacioX()),
                    y + fila * (altoUno + this.layout.getEspacioY()));
        }
    }

    /**
     * Se salta de golpe los teclados que no tocan el recorte, para no ir
     * preguntando tecla a tecla en los otros quince.
     */
    @Override
    public void dibujar(Graphics2D g) {
        Rectangle recorte = g.getClipBounds();
        for (PianoSencillo piano : this.pianos) {
            if (recorte == null || tocaElRecorte(piano, recorte)) {
                piano.dibujar(g);
            }
        }
    }

    @Override
    public void dibujarEnReposo(Graphics2D g) {
        Rectangle recorte = g.getClipBounds();
        for (PianoSencillo piano : this.pianos) {
            if (recorte == null || tocaElRecorte(piano, recorte)) {
                piano.dibujarEnReposo(g);
            }
        }
    }

    @Override
    public void soltarTodas() {
        for (PianoSencillo piano : this.pianos) {
            piano.soltarTodas();
        }
    }

    @Override
    public Tecla teclaEn(int x, int y) {
        Tecla encontrada = null;
        for (int i = 0; i < this.pianos.size() && encontrada == null; i++) {
            encontrada = this.pianos.get(i).teclaEn(x, y);
        }
        return encontrada;
    }

    /** Canal del teclado que hay en ese punto, o -1 si no hay ninguno. */
    public int canalEn(int x, int y) {
        int canal = -1;
        for (int i = 0; i < this.pianos.size() && canal < 0; i++) {
            Rectangle limites = this.pianos.get(i).getLimites();
            if (limites != null && limites.contains(x, y)) {
                canal = this.canales.get(i);
            }
        }
        return canal;
    }

    @Override
    public void dibujarBrillos(Graphics2D g) {
        for (PianoSencillo piano : this.pianos) {
            piano.dibujarBrillos(g);
        }
    }

    @Override
    public void anadirRegionesActivas(List<Rectangle> destino) {
        for (PianoSencillo piano : this.pianos) {
            piano.anadirRegionesActivas(destino);
        }
    }

    private boolean tocaElRecorte(PianoSencillo piano, Rectangle recorte) {
        Rectangle limites = piano.getLimites();
        boolean toca = true;
        if (limites != null) {
            Rectangle holgado = new Rectangle(limites);
            int holgura = Tecla.MARGEN_REPINTADO
                    + this.layout.getMedidas().getAlturaFieltro();
            holgado.grow(holgura, holgura);
            toca = holgado.intersects(recorte);
        }
        return toca;
    }

    private int anchuraDeUnTeclado() {
        return this.layout.getMedidas().anchuraDe(
                contarBlancas(getTeclaInicial(), getTeclaFinal()));
    }

    @Override
    public int getAnchura() {
        int columnas = this.layout.getColumnas();
        return columnas * anchuraDeUnTeclado()
                + (columnas - 1) * this.layout.getEspacioX();
    }

    @Override
    public int getAltura() {
        int filas = this.layout.getFilas();
        return filas * this.layout.getMedidas().getAlturaBlanca()
                + (filas - 1) * this.layout.getEspacioY();
    }
}
