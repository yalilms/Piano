package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Las notas cayendo: barras de color que bajan y llegan a su tecla justo
 * cuando esa nota empieza a sonar. La altura de cada barra es su duracion.
 */
public class Cascada implements CapaSuperpuesta {

    /** Cuanto se ve por delante: una nota aparece arriba este tiempo antes. */
    private static final long ANTICIPACION_US = 3_000_000;

    private static final double INSET = 1.0;
    private static final double RADIO = 3;

    private final Partitura partitura;
    private final Reproductor reproductor;

    /** Alto de la zona por la que caen las notas, en pixeles. */
    private int alturaCaida;
    private int yTope;

    /**
     * Por donde empezar a recorrer la lista. Avanza sola con la cancion para no
     * repasar miles de notas ya pasadas en cada frame.
     */
    private int indiceDesde;
    private long ultimaPosicion;

    public Cascada(Partitura partitura, Reproductor reproductor) {
        this.partitura = partitura;
        this.reproductor = reproductor;
        this.alturaCaida = 0;
        this.yTope = 0;
        this.indiceDesde = 0;
        this.ultimaPosicion = 0;
    }

    /** Define la franja por la que caen las notas. */
    public void setZona(int yTope, int alturaCaida) {
        this.yTope = yTope;
        this.alturaCaida = Math.max(0, alturaCaida);
    }

    @Override
    public void dibujar(Graphics2D g, Piano piano) {
        if (piano == null || this.alturaCaida <= 0) {
            return;
        }
        long posicion = this.reproductor.getPosicionUs();
        // Si se ha buscado hacia atras hay que volver a empezar el recorrido
        if (posicion < this.ultimaPosicion) {
            this.indiceDesde = 0;
        }
        this.ultimaPosicion = posicion;

        List<NotaMidi> notas = this.partitura.getNotas();
        while (this.indiceDesde < notas.size()
                && notas.get(this.indiceDesde).finUs() < posicion) {
            this.indiceDesde++;
        }

        int fieltro = piano.getMedidas().getAlturaFieltro();
        long horizonte = posicion + ANTICIPACION_US;

        for (int i = this.indiceDesde; i < notas.size(); i++) {
            NotaMidi nota = notas.get(i);
            if (nota.inicioUs() > horizonte) {
                break;
            }
            dibujarNota(g, piano, nota, posicion, fieltro);
        }
    }

    private void dibujarNota(Graphics2D g, Piano piano, NotaMidi nota,
                             long posicion, int fieltro) {
        Tecla tecla = piano.getTecla(nota.canal(), nota.nota());
        if (tecla == null) {
            return;
        }
        Rectangle caja = tecla.getForma().getBounds();
        // Las barras mueren en el fieltro, no encima de las teclas
        double yTeclado = caja.y - fieltro;

        double fraccionInicio = (nota.inicioUs() - posicion) / (double) ANTICIPACION_US;
        double fraccionFin = (nota.finUs() - posicion) / (double) ANTICIPACION_US;

        double abajo = yTeclado - fraccionInicio * this.alturaCaida;
        double arriba = yTeclado - fraccionFin * this.alturaCaida;

        abajo = Math.min(abajo, yTeclado);
        arriba = Math.max(arriba, this.yTope);
        double alto = abajo - arriba;
        if (alto < 1) {
            return;
        }

        double ancho = Math.max(2, caja.width - 2 * INSET);
        double x = caja.x + INSET;

        Color color = Estilo.colorDeCanal(nota.canal());
        // La velocidad de la nota decide lo encendida que se ve la barra
        double fuerza = 0.45 + 0.55 * (nota.velocidad() / 127.0);
        Color claro = Estilo.mezclar(color, Color.WHITE, 0.35 * fuerza);
        Color oscuro = Estilo.mezclar(color, Color.BLACK, 0.30);

        g.setPaint(new GradientPaint(
                (float) x, (float) arriba, Estilo.conAlfa(oscuro, (int) (170 * fuerza)),
                (float) x, (float) abajo, Estilo.conAlfa(claro, (int) (240 * fuerza))));
        g.fill(new RoundRectangle2D.Double(x, arriba, ancho, alto, RADIO * 2, RADIO * 2));

        // Filo brillante en el borde de abajo: marca donde va a golpear
        if (abajo < yTeclado) {
            g.setColor(Estilo.conAlfa(Color.WHITE, (int) (200 * fuerza)));
            g.fill(new RoundRectangle2D.Double(x, abajo - 2, ancho, 2, 2, 2));
        }
    }
}
