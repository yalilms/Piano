package jd.piano.teclas;

import java.awt.*;

public interface Pulsable extends ElementoVisual{

    void pulsar();
    void soltar();
    boolean estaPulsado();
    void setColorPulsado(Color c);
    Color getColorPulsado();
    Color getColorNoPulsado();
    default Color getColor(){
        Color c = null;
        if (estaPulsado()){
            c = getColorPulsado();
        } else {
            c = getColorNoPulsado();
        }
        return c;
    }

}
