package jd.piano.teclas;

import java.awt.*;

public class TeclaBlanca1 extends TeclaBlanca {

    public TeclaBlanca1(int n) {
        super(n);
    }

    @Override
    protected int[] getVerticesX() {
        int x = this.posicion.x;
        final int N = TeclaNegra.ANCHURA;
        final int B = TeclaBlanca.ANCHURA;
        // Forma: rectángulo con corte en la parte derecha
        return new int[] {
            x,          // Esquina superior izquierda
            x,          // Esquina inferior izquierda
            x + B,      // Esquina inferior derecha
            x + B,      // Punto derecho hasta altura de tecla negra
            x + B - N/2,// Punto interior derecho
            x + B - N/2 // Punto superior derecho
        };
    }

    @Override
    protected int[] getVerticesY() {
        int y = this.posicion.y;
        // Los puntos Y corresponden con los X de arriba
        return new int[] {
            y,                      // Esquina superior izquierda
            y + TeclaBlanca.ALTURA, // Esquina inferior izquierda
            y + TeclaBlanca.ALTURA, // Esquina inferior derecha
            y + TeclaNegra.ALTURA,  // Punto derecho hasta altura de tecla negra
            y + TeclaNegra.ALTURA,  // Punto interior derecho
            y                       // Punto superior derecho
        };
    }
}
