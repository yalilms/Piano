package jd.piano.teclas;

/** Mi y Si: solo tienen negra a la izquierda. */
public class TeclaBlanca3 extends TeclaBlanca {

    public TeclaBlanca3(int n, MedidasTeclado medidas) {
        super(n, medidas);
    }

    @Override
    protected boolean tieneMuescaIzquierda() {
        return true;
    }

    @Override
    protected boolean tieneMuescaDerecha() {
        return false;
    }
}
