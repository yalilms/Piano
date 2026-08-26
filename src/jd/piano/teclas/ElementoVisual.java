package jd.piano.teclas;

import java.awt.*;

public interface ElementoVisual extends Medible {
    void setPosicion(int x, int y);
    void setGraphics(Graphics g);
    void dibujar();
}
