package jd.piano.teclas;

import java.awt.*;

public class TeclaBlanca1 extends TeclaBlanca {

    TeclaBlanca1(int n) {
        super(n);
    }


    @Override
    protected int[] getVerticesX() {
        return new int[0];
    }

    @Override
    protected int[] getVerticesY() {
        return new int[0];
    }
}
