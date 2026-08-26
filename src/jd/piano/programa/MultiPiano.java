package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class MultiPiano extends Piano {

    public static final int CANALES = 16;

    // FILAS * COLUMNAS debe valer CANALES
    private static final int FILAS = 4;
    private static final int COLUMNAS = 4;
    private static final int ESPACIO_X = 10;
    private static final int ESPACIO_Y = 20;

    private List<PianoSencillo> pianos;

    public MultiPiano(int ti, int tf) {
        setTeclaInicial(ti);
        setTeclaFinal(tf);
        this.pianos = new ArrayList<>();
        for (int i = 0; i < CANALES; i++) {
            this.pianos.add(new PianoSencillo(ti, tf));
        }
    }

    @Override
    public Tecla getTecla(int canal, int nota) {
        Tecla tecla = null;
        if (canal >= 0 && canal < this.pianos.size()) {
            tecla = this.pianos.get(canal).getTecla(0, nota);
        }
        return tecla;
    }

    @Override
    public void setPosicion(int x, int y) {
        super.setPosicion(x, y);

        int anchoUnPiano = this.pianos.getFirst().getAnchura();
        int altoUnPiano = this.pianos.getFirst().getAltura();

        for (int i = 0; i < this.pianos.size(); i++) {
            int fila = i / COLUMNAS;
            int columna = i % COLUMNAS;

            int posX = x + columna * (anchoUnPiano + ESPACIO_X);
            int posY = y + fila * (altoUnPiano + ESPACIO_Y);

            this.pianos.get(i).setPosicion(posX, posY);
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

    private static boolean tocaElRecorte(PianoSencillo piano, Rectangle recorte) {
        Rectangle limites = piano.getLimites();
        boolean toca = limites == null;
        if (limites != null) {
            Rectangle holgado = new Rectangle(limites);
            holgado.grow(Tecla.MARGEN_REPINTADO + Estilo.FIELTRO_ALTURA,
                    Tecla.MARGEN_REPINTADO + Estilo.FIELTRO_ALTURA);
            toca = holgado.intersects(recorte);
        }
        return toca;
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

    @Override
    public int getAnchura() {
        int anchoUnPiano = this.pianos.getFirst().getAnchura();
        return COLUMNAS * anchoUnPiano + (COLUMNAS - 1) * ESPACIO_X;
    }

    @Override
    public int getAltura() {
        int altoUnPiano = this.pianos.getFirst().getAltura();
        return FILAS * altoUnPiano + (FILAS - 1) * ESPACIO_Y;
    }
}
