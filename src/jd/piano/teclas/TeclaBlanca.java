package jd.piano.teclas;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

public abstract class TeclaBlanca extends Tecla {
    public static final int ANCHURA = 25;
    public static final int ALTURA = 100;

    TeclaBlanca(int n) {
        super(n);
    }

    @Override
    public Color getColorNoPulsado() {
        return Color.WHITE;
    }

    @Override
    public int getAnchura() {
        return TeclaBlanca.ANCHURA;
    }

    @Override
    public int getAltura() {
        return TeclaBlanca.ALTURA;
    }

    /**
     * El cuerpo de la tecla menos el hueco que ocupan las negras vecinas.
     * Restar areas en vez de enumerar vertices a mano hace imposible el fallo
     * de geometria: no hay coordenadas sueltas que equivocar.
     */
    @Override
    public Shape getForma() {
        double x = this.posicion.x;
        double y = this.posicion.y;
        Area forma = cuerpoRedondeadoAbajo(x, y, ANCHURA, ALTURA, RADIO);

        // La negra se coloca en (x de la blanca siguiente - ANCHURA/2), asi que
        // con ANCHURA impar invade 8 px por la izquierda y 7 por la derecha.
        // Usando esos anchos el encaje es exacto y no queda costura.
        double invadeIzquierda = TeclaNegra.ANCHURA - TeclaNegra.ANCHURA / 2;
        double invadeDerecha = TeclaNegra.ANCHURA / 2;

        if (tieneMuescaIzquierda()) {
            forma.subtract(new Area(new Rectangle2D.Double(
                    x, y, invadeIzquierda, TeclaNegra.ALTURA)));
        }
        if (tieneMuescaDerecha()) {
            forma.subtract(new Area(new Rectangle2D.Double(
                    x + ANCHURA - invadeDerecha, y, invadeDerecha, TeclaNegra.ALTURA)));
        }
        return forma;
    }

    /** Si hay una tecla negra pegada por su lado izquierdo. */
    protected abstract boolean tieneMuescaIzquierda();

    /** Si hay una tecla negra pegada por su lado derecho. */
    protected abstract boolean tieneMuescaDerecha();
}
