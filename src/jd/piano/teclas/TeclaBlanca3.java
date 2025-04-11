package jd.piano.teclas;

import java.awt.*;

public class TeclaBlanca3 extends TeclaBlanca {

    public TeclaBlanca3(int n) {
        super(n);
    }

    @Override
    protected int[] getVerticesX() {
        int x = this.posicion.x;
        final int N = TeclaNegra.ANCHURA;
        final int B = TeclaBlanca.ANCHURA;
        return new int[] {x, x, x+B, x+B, x+N/2, x+N/2};
    }

    @Override
    protected int[] getVerticesY() {
        int y = this.posicion.y;
        return new int[] {y, y+TeclaBlanca.ALTURA, y+TeclaBlanca.ALTURA, y+TeclaNegra.ALTURA, y+TeclaNegra.ALTURA, y};
    }
}
