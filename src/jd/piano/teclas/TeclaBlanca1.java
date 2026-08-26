package jd.piano.teclas;

/** Do y Fa: solo tienen negra a la derecha. */
public class TeclaBlanca1 extends TeclaBlanca {

    public TeclaBlanca1(int n, MedidasTeclado medidas) {
        super(n, medidas);
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
