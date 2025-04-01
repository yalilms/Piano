package jd.piano.teclas;

import java.awt.*;

public interface ElementoVisual extends Medible {
    public void setPosicion(int x, int y);
    public void setGraphics(Graphics g);
    public void dibujar();
    
}
