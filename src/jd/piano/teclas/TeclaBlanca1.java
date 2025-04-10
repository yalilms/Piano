package jd.piano.teclas;

import java.awt.*;

public class TeclaBlanca1 extends TeclaBlanca {

    TeclaBlanca1(int n) {
        super(n);
    }


    @Override
    protected int[] getVerticesX() {
        int x = this.posicion.x;
        final int N = TeclaNegra.ANCHURA;
        final int B = TeclaBlanca.ANCHURA;
        return new int[] {x, x, x+B, x+B, x+B-N/2, x+B-N/2};
    }

    @Override
    protected int[] getVerticesY() {
        int y = this.posicion.y;
        return new int[] {y, y+TeclaBlanca.ALTURA, y+TeclaBlanca.ALTURA, y+TeclaNegra.ALTURA, y+TeclaNegra.ALTURA, y};
    }
}
