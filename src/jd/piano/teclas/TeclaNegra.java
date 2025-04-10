package jd.piano.teclas;

import javax.swing.text.Position;
import java.awt.*;

//-----------------------------> REALIZAR TESTS!!! <-------------------------------

public class TeclaNegra extends Tecla{
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
    protected int[] getVerticesX() {
        int x = this.posicion.x;
        return new int[] {x, x, x+TeclaNegra.ANCHURA, x+TeclaNegra.ANCHURA};
    }

    @Override
    protected int[] getVerticesY() {
        int y = this.posicion.y;
        return new int[] {y, y+TeclaNegra.ALTURA, y+TeclaNegra.ALTURA, y};
    }
}
