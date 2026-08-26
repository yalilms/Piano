package jd.piano.teclas;

import java.awt.Color;

public interface Pulsable extends ElementoVisual {

    /** @param velocidad fuerza de la pulsacion, 0-127 como en MIDI. */
    void pulsar(int velocidad);
    void soltar();
    boolean estaPulsado();
    void setColorPulsado(Color c);
    Color getColorPulsado();
    Color getColorNoPulsado();

    default Color getColor() {
        Color c = null;
        if (estaPulsado()) {
            c = getColorPulsado();
        } else {
            c = getColorNoPulsado();
        }
        return c;
    }
}
