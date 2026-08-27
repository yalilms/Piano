package jd.piano.programa;

import jd.piano.instrumentos.InstrumentoVisual;

import java.awt.Graphics2D;

/**
 * Algo que se pinta encima del instrumento en cada frame, como las notas
 * cayendo o las chispas. La Escena no necesita saber que es.
 */
public interface CapaSuperpuesta {
    void dibujar(Graphics2D g, InstrumentoVisual instrumento);
}
