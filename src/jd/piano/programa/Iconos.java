package jd.piano.programa;

import jd.piano.teclas.Estilo;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    /**
     * Guarda el icono como .ico, que es lo que necesita un acceso directo de
     * Windows.
     *
     * Un .ico no es mas que una cabecera, una entrada por imagen y los datos
     * seguidos. Desde Windows Vista se admite PNG dentro, asi que se reutiliza
     * el codificador que ya trae Java en vez de escribir bitmaps a mano.
     */
    public static void guardarIco(File destino) throws IOException {
        List<byte[]> pngs = new ArrayList<>();
        for (int tamano : TAMANOS) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(crear(tamano), "png", buffer);
            pngs.add(buffer.toByteArray());
        }

        int cabecera = 6 + 16 * pngs.size();
        try (DataOutputStream salida = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(destino)))) {
            escribirCorto(salida, 0);               // reservado
            escribirCorto(salida, 1);               // tipo: icono
            escribirCorto(salida, pngs.size());

            int desplazamiento = cabecera;
            for (int i = 0; i < pngs.size(); i++) {
                int tamano = TAMANOS[i];
                // 0 significa 256: el byte no llega a mas
                salida.writeByte(tamano >= 256 ? 0 : tamano);
                salida.writeByte(tamano >= 256 ? 0 : tamano);
                salida.writeByte(0);                // paleta
                salida.writeByte(0);                // reservado
                escribirCorto(salida, 1);           // planos
                escribirCorto(salida, 32);          // bits por pixel
                escribirEntero(salida, pngs.get(i).length);
                escribirEntero(salida, desplazamiento);
                desplazamiento += pngs.get(i).length;
            }
            for (byte[] png : pngs) {
                salida.write(png);
            }
        }
    }

    /** El .ico va en little endian, al contrario que DataOutputStream. */
    private static void escribirCorto(DataOutputStream salida, int valor)
            throws IOException {
        salida.writeByte(valor & 0xFF);
        salida.writeByte((valor >> 8) & 0xFF);
    }

    private static void escribirEntero(DataOutputStream salida, int valor)
            throws IOException {
        salida.writeByte(valor & 0xFF);
        salida.writeByte((valor >> 8) & 0xFF);
        salida.writeByte((valor >> 16) & 0xFF);
        salida.writeByte((valor >> 24) & 0xFF);
    }

    /** Para poder generar el .ico desde un script sin abrir la ventana. */
    public static void main(String[] args) throws IOException {
        File destino = new File(args.length > 0 ? args[0] : "piano.ico");
        guardarIco(destino);
        System.out.println("icono escrito en " + destino.getAbsolutePath()
                + " (" + destino.length() + " bytes)");
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
