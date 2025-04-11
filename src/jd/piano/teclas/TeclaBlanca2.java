package jd.piano.teclas;

import java.awt.*;

public class TeclaBlanca2 extends TeclaBlanca {

    public TeclaBlanca2(int n) {
        super(n);
    }

    @Override
    protected int[] getVerticesX() {
        int x = this.posicion.x;
        final int N = TeclaNegra.ANCHURA;
        final int B = TeclaBlanca.ANCHURA;
        // Forma: rectángulo con cortes en ambos lados
        return new int[] {
            x,              // Punto superior izquierdo
            x,              // Punto inferior izquierdo
            x + B,          // Punto inferior derecho
            x + B,          // Punto derecho hasta altura tecla negra
            x + B - N/2,    // Punto interior derecho
            x + B - N/2,    // Punto superior derecho
            x + N/2,        // Punto superior izquierdo interior
            x + N/2         // Punto izquierdo hasta altura tecla negra
        };
    }

    @Override
    protected int[] getVerticesY() {
        int y = this.posicion.y;
        // Los puntos Y corresponden con los X de arriba
        return new int[] {
            y,                      // Punto superior izquierdo
            y + TeclaBlanca.ALTURA, // Punto inferior izquierdo
            y + TeclaBlanca.ALTURA, // Punto inferior derecho
            y + TeclaNegra.ALTURA,  // Punto derecho hasta altura tecla negra
            y + TeclaNegra.ALTURA,  // Punto interior derecho
            y,                      // Punto superior derecho
            y,                      // Punto superior izquierdo interior
            y + TeclaNegra.ALTURA   // Punto izquierdo hasta altura tecla negra
        };
    }
}
