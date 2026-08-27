package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chispas que saltan de la tecla al golpearla, con el color de su canal.
 *
 * Detecta los golpes nuevos comparando el instante de pulsacion de cada tecla
 * con el que se vio la ultima vez, asi que no hace falta que nadie le avise.
 */
public class Particulas implements CapaSuperpuesta {

    private static final long VIDA_MS = 550;
    private static final int POR_GOLPE = 7;
    private static final int MAXIMO = 400;
    private static final double GRAVEDAD = 0.00022;

    /** Ultimo golpe visto de cada tecla, para no repetir el mismo. */
    private final Map<Tecla, Long> vistas = new HashMap<>();
    private final List<Chispa> chispas = new ArrayList<>();

    /** Semilla propia: el aspecto no depende del reloj y es reproducible. */
    private long semilla = 987654321L;

    @Override
    public void dibujar(Graphics2D g, Piano piano) {
        long ahora = System.currentTimeMillis();
        detectarGolpes(piano, ahora);
        pintar(g, ahora);
    }

    private void detectarGolpes(Piano piano, long ahora) {
        for (int nota = 0; nota <= 127; nota++) {
            Tecla tecla = buscar(piano, nota);
            if (tecla == null || !tecla.estaPulsado()) {
                continue;
            }
            long golpe = tecla.getInstantePulsacion();
            Long anterior = this.vistas.get(tecla);
            if (anterior == null || anterior != golpe) {
                this.vistas.put(tecla, golpe);
                lanzar(tecla, ahora);
            }
        }
    }

    /** Busca esa nota en cualquiera de los canales que tenga el piano. */
    private Tecla buscar(Piano piano, int nota) {
        Tecla tecla = null;
        if (piano instanceof MultiPiano mp) {
            for (int canal : mp.getCanales()) {
                Tecla candidata = mp.getTecla(canal, nota);
                if (candidata != null && candidata.estaPulsado()) {
                    tecla = candidata;
                    break;
                }
            }
        } else {
            tecla = piano.getTecla(0, nota);
        }
        return tecla;
    }

    private void lanzar(Tecla tecla, long ahora) {
        if (this.chispas.size() > MAXIMO) {
            return;
        }
        Rectangle caja = tecla.getForma().getBounds();
        double x = caja.getCenterX();
        double y = caja.getMaxY() - 2;
        // Las notas fuertes lanzan mas chispas y mas lejos
        double fuerza = 0.4 + 0.6 * (tecla.getVelocidad() / 127.0);
        int cuantas = Math.max(3, (int) Math.round(POR_GOLPE * fuerza));
        Color color = tecla.getColorPulsado();

        for (int i = 0; i < cuantas; i++) {
            double vx = (aleatorio() - 0.5) * 0.16 * (1 + fuerza);
            double vy = -(0.05 + aleatorio() * 0.16) * (0.6 + fuerza);
            double radio = 1.2 + aleatorio() * 2.2 * fuerza;
            this.chispas.add(new Chispa(x, y, vx, vy, radio, ahora, color));
        }
    }

    private void pintar(Graphics2D g, long ahora) {
        this.chispas.removeIf(c -> ahora - c.nacimiento >= VIDA_MS);
        for (Chispa c : this.chispas) {
            long edad = ahora - c.nacimiento;
            double t = edad / (double) VIDA_MS;
            double px = c.x + c.vx * edad;
            double py = c.y + c.vy * edad + GRAVEDAD * edad * edad;
            int alfa = (int) Math.round(210 * (1 - t) * (1 - t));
            double radio = c.radio * (1 - 0.4 * t);

            g.setColor(Estilo.conAlfa(c.color, alfa));
            g.fill(new Ellipse2D.Double(px - radio, py - radio, radio * 2, radio * 2));
        }
    }

    /**
     * Generador propio en vez de Math.random para que dos ejecuciones con los
     * mismos golpes den las mismas chispas y las capturas de prueba se puedan
     * comparar.
     */
    private double aleatorio() {
        this.semilla = this.semilla * 6364136223846793005L + 1442695040888963407L;
        return ((this.semilla >>> 11) & 0xFFFFFFFFFFFFFL) / (double) (1L << 52);
    }

    /** Una chispa: posicion y velocidad iniciales, y cuando nacio. */
    private record Chispa(double x, double y, double vx, double vy,
                          double radio, long nacimiento, Color color) {
    }
}
