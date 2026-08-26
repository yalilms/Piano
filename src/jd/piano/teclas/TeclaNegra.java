package jd.piano.teclas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class TeclaNegra extends Tecla {

    /** Proporcion del alto que ocupa el reflejo pulido de arriba. */
    private static final double BRILLO = 0.28;

    TeclaNegra(int n, MedidasTeclado medidas) {
        super(n, medidas);
    }

    @Override
    public Color getColorNoPulsado() {
        return Estilo.GRAFITO_BAJO;
    }

    @Override
    public int getAnchura() {
        return this.medidas.getAnchuraNegra();
    }

    @Override
    public int getAltura() {
        return this.medidas.getAlturaNegra();
    }

    @Override
    protected void dibujar(Graphics2D g, double intensidad) {
        Shape forma = getFormaDibujo();
        Rectangle2D caja = forma.getBounds2D();
        double escala = this.medidas.getEscala();

        // Sombra sobre las blancas. Se pinta aqui, y no en la pasada de las
        // blancas, porque las negras se dibujan despues y asi cae encima.
        // Dos capas desplazadas hacen de difuminado barato.
        g.setColor(Estilo.conAlfa(Estilo.SOMBRA, 40));
        g.fill(desplazar(forma, 3 * escala, 4 * escala));
        g.setColor(Estilo.SOMBRA);
        g.fill(desplazar(forma, 1 * escala, 2 * escala));

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
                new float[]{0f, (float) BRILLO, 1f},
                new Color[]{reflejo, arriba, abajo}));
        g.fill(forma);

        g.setStroke(new BasicStroke(1f));
        g.setColor(Estilo.CONTORNO_NEGRA);
        g.draw(forma);
    }

    @Override
    protected Shape construirForma() {
        return cuerpoRedondeadoAbajo(this.posicion.x, this.posicion.y,
                this.medidas.getAnchuraNegra(), this.medidas.getAlturaNegra(),
                this.medidas.getRadioEsquina());
    }

    private static Shape desplazar(Shape forma, double dx, double dy) {
        return AffineTransform.getTranslateInstance(dx, dy)
                .createTransformedShape(forma);
    }
}
