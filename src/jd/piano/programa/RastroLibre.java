package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.PiezaPulsable;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * En el modo de tocar libre no hay notas cayendo, porque no hay cancion. Lo que
 * se dibuja es lo contrario: una barra que crece hacia arriba mientras
 * mantienes la tecla, y al soltarla se suelta y sube desvaneciendose.
 *
 * Asi la mitad de arriba de la ventana no se queda vacia y se ve la forma de lo
 * que acabas de tocar.
 */
public class RastroLibre implements CapaSuperpuesta {

    /** Pixeles que sube la barra por segundo mientras se mantiene la tecla. */
    private static final double CRECE_POR_SEGUNDO = 190;

    /** Cuanto tarda en desaparecer una barra ya soltada. */
    private static final long VIDA_MS = 1500;

    private static final double SUBE_POR_SEGUNDO = 120;
    private static final int INSET = 1;

    /** Barras que siguen creciendo, por nota. */
    private final Map<Integer, Barra> creciendo = new HashMap<>();

    /** Barras ya soltadas, subiendo y apagandose. */
    private final List<Barra> subiendo = new ArrayList<>();

    private int yTope;

    public RastroLibre() {
        this.yTope = 0;
    }

    public void setYTope(int yTope) {
        this.yTope = yTope;
    }

    @Override
    public void dibujar(Graphics2D g, InstrumentoVisual instrumento) {
        if (instrumento == null) {
            return;
        }
        long ahora = System.currentTimeMillis();
        actualizar(instrumento, ahora);
        pintar(g, ahora);
        dibujarUltima(g, instrumento);
    }

    private void actualizar(InstrumentoVisual instrumento, long ahora) {
        // Nuevas pulsaciones y barras que siguen creciendo
        for (int nota = 0; nota <= 127; nota++) {
            PiezaPulsable pieza = instrumento.piezaDe(SintetizadorTeclado.CANAL, nota);
            if (pieza == null) {
                pieza = instrumento.piezaDe(0, nota);
            }
            if (pieza == null || pieza.getInstantePulsacion() == 0) {
                continue;
            }
            Barra barra = this.creciendo.get(nota);
            if (pieza.estaPulsado()) {
                if (barra == null || barra.nacimiento != pieza.getInstantePulsacion()) {
                    Rectangle caja = pieza.getForma().getBounds();
                    this.creciendo.put(nota, new Barra(nota,
                            caja.x + INSET, Math.max(2, caja.width - 2 * INSET),
                            caja.y - 2, pieza.getInstantePulsacion(),
                            pieza.getVelocidad(), pieza.getColorPulsado()));
                }
            } else if (barra != null) {
                // Se ha soltado: la barra se suelta y empieza a subir
                barra.soltada = ahora;
                this.subiendo.add(barra);
                this.creciendo.remove(nota);
            }
        }
        this.subiendo.removeIf(b -> ahora - b.soltada >= VIDA_MS);
    }

    private void pintar(Graphics2D g, long ahora) {
        for (Barra barra : this.creciendo.values()) {
            pintarBarra(g, barra, alto(barra, ahora), barra.base, 1.0);
        }
        for (Barra barra : this.subiendo) {
            long edad = ahora - barra.soltada;
            double t = edad / (double) VIDA_MS;
            double alto = alto(barra, barra.soltada);
            double base = barra.base - SUBE_POR_SEGUNDO * edad / 1000.0;
            pintarBarra(g, barra, alto, base, (1 - t) * (1 - t));
        }
    }

    /** Lo que ha crecido desde que se pulso, sin pasarse del techo. */
    private double alto(Barra barra, long hasta) {
        double crecido = CRECE_POR_SEGUNDO * (hasta - barra.nacimiento) / 1000.0;
        return Math.max(4, Math.min(crecido, barra.base - this.yTope));
    }

    private void pintarBarra(Graphics2D g, Barra barra, double alto, double base,
                             double opacidad) {
        double arriba = Math.max(this.yTope, base - alto);
        double altura = base - arriba;
        if (altura < 1 || opacidad <= 0.02) {
            return;
        }
        double fuerza = 0.45 + 0.55 * (barra.velocidad / 127.0);
        int alfaAbajo = (int) Math.round(235 * fuerza * opacidad);
        int alfaArriba = (int) Math.round(60 * fuerza * opacidad);

        Color claro = Estilo.mezclar(barra.color, Color.WHITE, 0.30);
        g.setPaint(new GradientPaint(
                0f, (float) arriba, Estilo.conAlfa(barra.color, alfaArriba),
                0f, (float) base, Estilo.conAlfa(claro, alfaAbajo)));
        g.fill(new RoundRectangle2D.Double(barra.x, arriba, barra.ancho, altura, 6, 6));
    }

    /** El nombre de la ultima nota que se ha tocado, grande y centrado. */
    private void dibujarUltima(Graphics2D g, InstrumentoVisual instrumento) {
        Barra ultima = null;
        for (Barra barra : this.creciendo.values()) {
            if (ultima == null || barra.nacimiento > ultima.nacimiento) {
                ultima = barra;
            }
        }
        if (ultima == null) {
            return;
        }
        Rectangle limites = instrumento.getLimites();
        if (limites == null) {
            return;
        }
        String texto = Estilo.etiquetaDe(ultima.nota);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        FontMetrics metricas = g.getFontMetrics();
        int x = limites.x + (limites.width - metricas.stringWidth(texto)) / 2;
        int y = this.yTope + metricas.getAscent() + 6;

        g.setColor(Estilo.conAlfa(Color.BLACK, 120));
        g.drawString(texto, x + 2, y + 2);
        g.setColor(Estilo.conAlfa(ultima.color, 235));
        g.drawString(texto, x, y);
    }

    /** Una barra: donde esta, cuando nacio y con que fuerza. */
    private static final class Barra {
        private final int nota;
        private final double x;
        private final double ancho;
        private final double base;
        private final long nacimiento;
        private final int velocidad;
        private final Color color;
        private long soltada;

        Barra(int nota, double x, double ancho, double base, long nacimiento,
              int velocidad, Color color) {
            this.nota = nota;
            this.x = x;
            this.ancho = ancho;
            this.base = base;
            this.nacimiento = nacimiento;
            this.velocidad = velocidad;
            this.color = color;
            this.soltada = 0;
        }
    }
}
