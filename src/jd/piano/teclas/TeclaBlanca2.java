package jd.piano.teclas;

/** Re, Sol y La: tienen negra a los dos lados. */
public class TeclaBlanca2 extends TeclaBlanca {

    public TeclaBlanca2(int n) {
        super(n);
    }

    @Override
    protected boolean tieneMuescaIzquierda() {
        return true;
    }

    @Override
    protected boolean tieneMuescaDerecha() {
        return true;
    }
}
