package jd.piano.programa;

import jd.piano.teclas.Piano;

import java.awt.Graphics2D;

/**
 * Algo que se pinta encima del piano en cada frame, como las notas cayendo.
 * La Escena no necesita saber que es.
 */
public interface CapaSuperpuesta {
    void dibujar(Graphics2D g, Piano piano);
}
