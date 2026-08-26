package jd.piano.teclas;

/** Do y Fa: solo tienen negra a la derecha. */
public class TeclaBlanca1 extends TeclaBlanca {

    public TeclaBlanca1(int n) {
        super(n);
    }

    @Override
    protected boolean tieneMuescaIzquierda() {
        return false;
    }

    @Override
    protected boolean tieneMuescaDerecha() {
        return true;
    }
}
