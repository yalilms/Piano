package jd.piano.teclas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class TeclaNegra extends Tecla {
    public static final int ANCHURA = 15;
    public static final int ALTURA = 50;

    /** Alto del reflejo especular de la parte de arriba. */
    private static final int BRILLO = 14;

    TeclaNegra(int n) {
        super(n);
    }

    @Override
    public Color getColorNoPulsado() {
        return Estilo.GRAFITO_BAJO;
    }

    @Override
    public int getAnchura() {
        return TeclaNegra.ANCHURA;
    }

    @Override
    public int getAltura() {
        return TeclaNegra.ALTURA;
    }

    @Override
    public void dibujar(Graphics2D g) {
        Shape forma = getFormaDibujo();
        Rectangle2D caja = forma.getBounds2D();
        double intensidad = getIntensidad();

        // Sombra sobre las blancas. Se pinta aqui, y no en la pasada de las
        // blancas, porque las negras se dibujan despues y asi cae encima.
        // Dos capas desplazadas hacen de difuminado barato.
        g.setColor(Estilo.conAlfa(Estilo.SOMBRA, 40));
        g.fill(desplazar(forma, 3, 4));
        g.setColor(Estilo.SOMBRA);
        g.fill(desplazar(forma, 1, 2));

        Color arriba = Estilo.GRAFITO_ALTO;
        Color abajo = Estilo.GRAFITO_BAJO;
        if (intensidad > 0) {
            Color tinte = getColorPulsado();
            arriba = Estilo.mezclar(arriba, tinte, intensidad * 0.85);
            abajo = Estilo.mezclar(abajo, tinte, intensidad * 0.6);
        }
        // El reflejo pulido va como primera parada del mismo degradado, en vez
        // de como un segundo relleno clipado a la forma.
        Color reflejo = Estilo.mezclar(arriba, Color.WHITE, 0.30);

        float y0 = (float) caja.getY();
        float y1 = (float) caja.getMaxY();
        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, y0), new Point2D.Float(0, y1),
                new float[]{0f, (float) BRILLO / (float) caja.getHeight(), 1f},
                new Color[]{reflejo, arriba, abajo}));
        g.fill(forma);

        g.setStroke(new BasicStroke(1f));
        g.setColor(Estilo.CONTORNO_NEGRA);
        g.draw(forma);
    }

    @Override
    protected Shape construirForma() {
        return cuerpoRedondeadoAbajo(this.posicion.x, this.posicion.y,
                ANCHURA, ALTURA, RADIO);
    }

    private static Shape desplazar(Shape forma, double dx, double dy) {
        return AffineTransform.getTranslateInstance(dx, dy)
                .createTransformedShape(forma);
    }
}
