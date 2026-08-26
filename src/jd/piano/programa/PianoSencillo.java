package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;
import jd.piano.teclas.TeclaBlanca;
import jd.piano.teclas.TeclaFactory;
import jd.piano.teclas.TeclaNegra;

import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PianoSencillo extends Piano {

    private Map<Integer, Tecla> teclas;

    public PianoSencillo(int teclaInicial, int teclaFinal) {
        setTeclaInicial(teclaInicial);
        setTeclaFinal(teclaFinal);
        this.teclas = new HashMap<>();
        for (int i = teclaInicial; i <= teclaFinal; i++) {
            this.teclas.put(i, TeclaFactory.crearTecla(i));
        }
    }

    @Override
    public Tecla getTecla(int canal, int nota) {
        return this.teclas.get(nota);
    }

    @Override
    public void setPosicion(int x, int y) {
        super.setPosicion(x, y);
        int x1 = x;

        for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaBlanca) {
                tecla.setPosicion(x1, y);
                x1 += tecla.getAnchura();
            } else {
                tecla.setPosicion(x1 - TeclaNegra.ANCHURA / 2, y);
            }
        }
    }

    /**
     * Fieltro, blancas y negras. Los brillos van aparte, en dibujarBrillos.
     *
     * Las negras se pintan despues de todas las blancas para que ellas y sus
     * sombras queden siempre encima, sin depender del orden de las notas.
     *
     * Si el Graphics viene recortado solo se dibuja lo que cae dentro: asi la
     * Escena puede repintar una franja pequena en vez del teclado entero.
     */
    @Override
    public void dibujar(Graphics2D g) {
        Rectangle recorte = g.getClipBounds();
        dibujarFieltro(g);

        for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaBlanca && visible(tecla, recorte)) {
                tecla.dibujar(g);
            }
        }
        for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaNegra && visible(tecla, recorte)) {
                tecla.dibujar(g);
            }
        }
    }

    @Override
    public void dibujarBrillos(Graphics2D g) {
        for (Tecla tecla : this.teclas.values()) {
            tecla.dibujarBrillo(g);
        }
    }

    @Override
    public void anadirRegionesActivas(List<Rectangle> destino) {
        for (Tecla tecla : this.teclas.values()) {
            if (tecla.getIntensidad() > 0) {
                destino.add(new Rectangle(tecla.getCajaRepintado()));
            }
        }
    }

    /** Una tecla cuya sombra puede caer dentro del recorte cuenta como visible. */
    private static boolean visible(Tecla tecla, Rectangle recorte) {
        return recorte == null || tecla.getCajaRepintado().intersects(recorte);
    }

    /** La franja de fieltro que tapa el nacimiento de las teclas. */
    private void dibujarFieltro(Graphics2D g) {
        if (getPosicion() == null) {
            return;
        }
        double x = getPosicion().x;
        double y = getPosicion().y - Estilo.FIELTRO_ALTURA;
        g.setPaint(new GradientPaint(
                0f, (float) y, Estilo.FIELTRO_BAJO,
                0f, (float) (y + Estilo.FIELTRO_ALTURA), Estilo.FIELTRO_ALTO));
        g.fill(new Rectangle2D.Double(x, y, getAnchura(), Estilo.FIELTRO_ALTURA));
    }

    @Override
    public int getAnchura() {
        int contadorBlancas = 0;
        for (Tecla tecla : this.teclas.values()) {
            if (tecla instanceof TeclaBlanca) {
                contadorBlancas++;
            }
        }
        return contadorBlancas * TeclaBlanca.ANCHURA;
    }

    @Override
    public int getAltura() {
        return TeclaBlanca.ALTURA;
    }
}
