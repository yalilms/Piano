package jd.piano.instrumentos;

import java.awt.Graphics2D;

public interface ElementoVisual extends Medible {
    void setPosicion(int x, int y);

    /**
     * Se dibuja en el Graphics2D que se le pasa, sin guardarlo.
     * Asi el mismo elemento sirve para pintar en pantalla frame a frame
     * o para volcarse a una imagen.
     */
    void dibujar(Graphics2D g);
}
