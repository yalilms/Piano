package jd.piano.teclas;

import jd.piano.instrumentos.PiezaPulsable;

/**
 * Una tecla del piano.
 *
 * El estado (si esta pulsada, con que fuerza, el desvanecido al soltar, el
 * resplandor) vive en PiezaPulsable, que es lo que comparte con un traste de la
 * guitarra o una barra del xilofono. Aqui queda solo lo propio de una tecla:
 * sus medidas y su forma.
 */
public abstract class Tecla extends PiezaPulsable {

    protected MedidasTeclado medidas;

    public Tecla(int n, MedidasTeclado medidas) {
        super(n);
        this.medidas = medidas;
    }

    public MedidasTeclado getMedidas() {
        return this.medidas;
    }

    /** Cambia el tamano de la tecla sin perder su estado ni su identidad. */
    public void setMedidas(MedidasTeclado medidas) {
        this.medidas = medidas;
        invalidarForma();
    }

    @Override
    protected int getHundimiento() {
        return this.medidas.getHundimiento();
    }

    public abstract int getAnchura();

    public abstract int getAltura();
}
