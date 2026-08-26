package jd.piano.teclas;

import java.awt.Color;
import java.awt.Shape;

public class TeclaNegra extends Tecla {
    public static final int ANCHURA = 15;
    public static final int ALTURA = 50;

    TeclaNegra(int n) {
        super(n);
    }

    @Override
    public Color getColorNoPulsado() {
        return Color.BLACK;
    }

    @Override
    public int getAnchura() {
        return TeclaNegra.ANCHURA;
    }

    @Override
    public int getAltura() {
        return TeclaNegra.ALTURA;
    }

    @Override
    public Shape getForma() {
        return cuerpoRedondeadoAbajo(this.posicion.x, this.posicion.y,
                ANCHURA, ALTURA, RADIO);
    }
}
