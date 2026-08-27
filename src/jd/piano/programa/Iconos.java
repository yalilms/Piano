package jd.piano.programa;

import jd.piano.teclas.Estilo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * El icono de la ventana, dibujado al vuelo: unas teclas sobre madera.
 * Asi no hace falta arrastrar un fichero de imagen con el proyecto.
 */
public final class Iconos {

    private static final int[] TAMANOS = {16, 32, 48, 64, 128};

    private Iconos() {
    }

    /** Varios tamanos, que el sistema elige el que le convenga. */
    public static List<BufferedImage> deLaAplicacion() {
        List<BufferedImage> iconos = new ArrayList<>();
        for (int tamano : TAMANOS) {
            iconos.add(crear(tamano));
        }
        return iconos;
    }

    private static BufferedImage crear(int tamano) {
        BufferedImage img = new BufferedImage(tamano, tamano,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        double radio = tamano * 0.18;
        g.setColor(new Color(96, 62, 38));
        g.fill(new RoundRectangle2D.Double(0, 0, tamano, tamano, radio, radio));

        // Cinco blancas y las negras encima, en el hueco central
        double margen = tamano * 0.14;
        double ancho = (tamano - 2 * margen) / 5.0;
        double arriba = tamano * 0.24;
        double alto = tamano - arriba - margen;

        for (int i = 0; i < 5; i++) {
            g.setColor(Estilo.MARFIL_ALTO);
            g.fill(new RoundRectangle2D.Double(margen + i * ancho, arriba,
                    ancho - Math.max(1, tamano * 0.012), alto,
                    radio * 0.4, radio * 0.4));
        }
        // Negras entre la 1-2 y la 2-3, y entre la 4-5: el patron reconocible
        double anchoNegra = ancho * 0.58;
        double altoNegra = alto * 0.62;
        double[] centros = {1, 2, 4};
        g.setColor(Estilo.GRAFITO_BAJO);
        for (double c : centros) {
            g.fill(new RoundRectangle2D.Double(margen + c * ancho - anchoNegra / 2,
                    arriba, anchoNegra, altoNegra, radio * 0.3, radio * 0.3));
        }

        g.dispose();
        return img;
    }
}
