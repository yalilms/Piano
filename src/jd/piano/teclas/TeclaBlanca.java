package jd.piano.teclas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class TeclaBlanca extends Tecla {
    public static final int ANCHURA = 25;
    public static final int ALTURA = 100;

    /** Alto del canto frontal iluminado, en pixeles. */
    private static final int CANTO = 9;

    TeclaBlanca(int n) {
        super(n);
    }

    @Override
    public Color getColorNoPulsado() {
        return Estilo.MARFIL_ALTO;
    }

    @Override
    public int getAnchura() {
        return TeclaBlanca.ANCHURA;
    }

    @Override
    public int getAltura() {
        return TeclaBlanca.ALTURA;
    }

    @Override
    public void dibujar(Graphics2D g) {
        Shape forma = getFormaDibujo();
        Rectangle2D caja = forma.getBounds2D();
        double intensidad = getIntensidad();

        Color arriba = Estilo.MARFIL_ALTO;
        Color abajo = Estilo.MARFIL_BAJO;
        if (intensidad > 0) {
            Color tinte = getColorPulsado();
            // El tinte entra mas fuerte por abajo: la luz parece venir del frente
            arriba = Estilo.mezclar(arriba, tinte, intensidad * 0.55);
            abajo = Estilo.mezclar(abajo, tinte, intensidad * 0.95);
        }

        // Cuerpo y canto frontal en un solo relleno de tres paradas. Antes eran
        // dos fill mas un clip a la forma, y clipar contra una silueta con
        // muescas es de lo mas caro que hay en Java2D.
        float y0 = (float) caja.getY();
        float y1 = (float) caja.getMaxY();
        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, y0), new Point2D.Float(0, y1),
                new float[]{0f, 1f - (float) CANTO / (float) caja.getHeight(), 1f},
                new Color[]{arriba, abajo, Estilo.MARFIL_CANTO}));
        g.fill(forma);

        g.setStroke(new BasicStroke(1f));
        g.setColor(Estilo.SEPARADOR);
        g.draw(forma);
    }

    /**
     * El cuerpo de la tecla menos el hueco que ocupan las negras vecinas.
     * Restar areas en vez de enumerar vertices a mano hace imposible el fallo
     * de geometria: no hay coordenadas sueltas que equivocar.
     */
    @Override
    protected Shape construirForma() {
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
