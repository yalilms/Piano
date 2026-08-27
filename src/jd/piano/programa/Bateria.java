package jd.piano.programa;

import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.teclas.Estilo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Los golpes de bateria como pads que se encienden, con el nombre de cada
 * pieza. Solo se montan los pads de las piezas que la cancion usa de verdad,
 * que suelen ser diez o doce de las sesenta posibles.
 *
 * Se dibuja como capa superpuesta porque cambia en cada frame y no puede
 * entrar en las imagenes que la Escena tiene cacheadas.
 */
public class Bateria implements CapaSuperpuesta {

    /** Cuanto tarda un pad en apagarse tras el golpe. */
    private static final long FUNDIDO_MS = 320;

    private static final int ALTO_PAD = 26;
    private static final int SEPARACION = 4;
    private static final int ANCHO_MINIMO = 54;

    private final List<Pad> pads = new ArrayList<>();
    private Rectangle zona;
    private int filas;

    public Bateria() {
        this.zona = null;
        this.filas = 0;
    }

    /** Monta un pad por cada pieza que use la cancion, en orden por familia. */
    public void construir(List<Integer> notasUsadas) {
        this.pads.clear();
        List<Integer> ordenadas = new ArrayList<>(notasUsadas);
        ordenadas.sort(Comparator.comparingInt(PiezasPercusion::ordenDe));
        for (int nota : ordenadas) {
            this.pads.add(new Pad(nota));
        }
        recolocar();
    }

    public boolean estaVacia() {
        return this.pads.isEmpty();
    }

    public int getCuantosPads() {
        return this.pads.size();
    }

    /** Franja de pantalla donde caben los pads. */
    public void setZona(Rectangle zona) {
        this.zona = zona;
        recolocar();
    }

    /**
     * Reparte los pads en tantas filas como hagan falta para que quepan con un
     * ancho legible.
     */
    private void recolocar() {
        if (this.zona == null || this.pads.isEmpty()) {
            return;
        }
        int porFila = Math.max(1, this.zona.width / (ANCHO_MINIMO + SEPARACION));
        porFila = Math.min(porFila, this.pads.size());
        int filas = (this.pads.size() + porFila - 1) / porFila;
        int ancho = (this.zona.width - (porFila - 1) * SEPARACION) / porFila;

        for (int i = 0; i < this.pads.size(); i++) {
            int fila = i / porFila;
            int columna = i % porFila;
            // La ultima fila puede ir incompleta: se centra para que no cojee
            int enEstaFila = Math.min(porFila, this.pads.size() - fila * porFila);
            int sobra = (this.zona.width - enEstaFila * ancho
                    - (enEstaFila - 1) * SEPARACION) / 2;
            this.pads.get(i).caja = new Rectangle(
                    this.zona.x + sobra + columna * (ancho + SEPARACION),
                    this.zona.y + fila * (ALTO_PAD + SEPARACION),
                    ancho, ALTO_PAD);
        }
        this.filas = filas;
    }

    /** Alto que necesita para todos sus pads. */
    public int getAlturaNecesaria() {
        return this.filas <= 0 ? 0
                : this.filas * ALTO_PAD + (this.filas - 1) * SEPARACION;
    }

    /** Un golpe de bateria: enciende su pad. */
    public void golpear(int nota, int velocidad) {
        for (Pad pad : this.pads) {
            if (pad.nota == nota) {
                pad.instanteGolpe = System.currentTimeMillis();
                pad.velocidad = velocidad;
            }
        }
    }

    public void apagarTodo() {
        for (Pad pad : this.pads) {
            pad.instanteGolpe = 0;
        }
    }

    @Override
    public void dibujar(Graphics2D g, InstrumentoVisual instrumento) {
        // Los pads no dependen del instrumento melodico que haya debajo
        if (this.zona == null) {
            return;
        }
        long ahora = System.currentTimeMillis();
        for (Pad pad : this.pads) {
            dibujarPad(g, pad, ahora);
        }
    }

    private void dibujarPad(Graphics2D g, Pad pad, long ahora) {
        if (pad.caja == null) {
            return;
        }
        double intensidad = pad.intensidad(ahora);
        Color color = Estilo.colorDeCanal(
                PiezasPercusion.familia(pad.nota).ordinal());

        Color fondoAlto = Estilo.mezclar(new Color(46, 38, 30), color,
                0.15 + 0.75 * intensidad);
        Color fondoBajo = Estilo.mezclar(new Color(24, 19, 14), color,
                0.10 + 0.55 * intensidad);

        RoundRectangle2D forma = new RoundRectangle2D.Double(
                pad.caja.x, pad.caja.y, pad.caja.width, pad.caja.height, 6, 6);
        g.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, pad.caja.y),
                new Point2D.Float(0, pad.caja.y + pad.caja.height),
                new float[]{0f, 1f}, new Color[]{fondoAlto, fondoBajo}));
        g.fill(forma);

        g.setStroke(new BasicStroke(1f));
        g.setColor(Estilo.conAlfa(color, (int) (90 + 165 * intensidad)));
        g.draw(forma);

        // El nombre, recortado si no cabe en el pad
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        FontMetrics metricas = g.getFontMetrics();
        String texto = recortar(PiezasPercusion.nombre(pad.nota), metricas,
                pad.caja.width - 8);
        g.setColor(Estilo.mezclar(new Color(186, 176, 162), Color.WHITE, intensidad));
        g.drawString(texto,
                pad.caja.x + (pad.caja.width - metricas.stringWidth(texto)) / 2,
                pad.caja.y + (pad.caja.height + metricas.getAscent()) / 2 - 2);
    }

    private static String recortar(String texto, FontMetrics metricas, int ancho) {
        String salida = texto;
        while (salida.length() > 1 && metricas.stringWidth(salida) > ancho) {
            salida = salida.substring(0, salida.length() - 1);
        }
        return salida;
    }

    /** Un pad: su pieza, donde esta y cuando se golpeo por ultima vez. */
    private static final class Pad {
        private final int nota;
        private Rectangle caja;
        private volatile long instanteGolpe;
        private volatile int velocidad;

        Pad(int nota) {
            this.nota = nota;
            this.caja = null;
            this.instanteGolpe = 0;
            this.velocidad = 0;
        }

        double intensidad(long ahora) {
            double valor = 0;
            if (this.instanteGolpe > 0) {
                long edad = ahora - this.instanteGolpe;
                if (edad < FUNDIDO_MS) {
                    double queda = 1.0 - edad / (double) FUNDIDO_MS;
                    valor = queda * queda * (0.35 + 0.65 * (this.velocidad / 127.0));
                }
            }
            return valor;
        }
    }
}
