package jd.piano.programa;

import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import java.awt.Graphics2D;
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

    @Override
    public void dibujar(Graphics2D g) {
        for (PianoSencillo piano : this.pianos) {
            piano.dibujar(g);
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
