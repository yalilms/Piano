package jd.piano.teclas;

import java.awt.*;
import java.awt.image.ConvolveOp;

public interface Pulsable {

    public abstract void pulsar();
    public abstract void soltar();
    public abstract boolean estaPulsado();
    public abstract void setColorPulsado(Color c);
    public abstract Color getColorPulsado();
    public abstract Color getColorNoPulsado();
    public default Color getColor(){
        throw new UnsupportedOperationException();
    }

}
