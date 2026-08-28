package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;
import jd.piano.teclas.TeclaBlanca;
import jd.piano.teclas.TeclaFactory;

import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PianoSencillo extends Piano {

    private final Map<Integer, Tecla> teclas;
    private MedidasTeclado medidas;

    public PianoSencillo(int teclaInicial, int teclaFinal) {
        this(teclaInicial, teclaFinal, MedidasTeclado.NORMAL);
    }

    public PianoSencillo(int teclaInicial, int teclaFinal, MedidasTeclado medidas) {
        this.medidas = medidas;
        setTeclaInicial(teclaInicial);
        setTeclaFinal(teclaFinal);
        this.teclas = new HashMap<>();
        for (int i = teclaInicial; i <= teclaFinal; i++) {
            this.teclas.put(i, TeclaFactory.crearTecla(i, medidas,
                    teclaInicial, teclaFinal));
        }
    }

    @Override
    public Tecla getTecla(int canal, int nota) {
        return this.teclas.get(nota);
    }

    @Override
    public MedidasTeclado getMedidas() {
        return this.medidas;
    }

    @Override
    public void setMedidas(MedidasTeclado medidas) {
        this.medidas = medidas;
        for (Tecla tecla : this.teclas.values()) {
            tecla.setMedidas(medidas);
        }
        // Las posiciones dependen del ancho de tecla, hay que recolocarlas
        if (getPosicion() != null) {
            setPosicion(getPosicion().x, getPosicion().y);
        }
    }

    /**
     * Escala el teclado para que quepa en ese hueco. A escala fija son 1250 px
     * de ancho y se salia en ventanas mas estrechas.
     */
    @Override
    public void ajustarA(int ancho, int alto) {
        setMedidas(LayoutTeclados.calcular(1,
                contarBlancas(getTeclaInicial(), getTeclaFinal()),
                ancho, alto).getMedidas());
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
                tecla.setPosicion(x1 - this.medidas.getDesplazamientoNegra(), y);
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
        recorrerTeclas(g, false);
    }

    @Override
    public void dibujarEnReposo(Graphics2D g) {
        recorrerTeclas(g, true);
    }

    private void recorrerTeclas(Graphics2D g, boolean enReposo) {
        Rectangle recorte = g.getClipBounds();
        dibujarFieltro(g);

        for (int pasada = 0; pasada < 2; pasada++) {
            boolean tocanBlancas = (pasada == 0);
            for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
                Tecla tecla = this.teclas.get(i);
                boolean esBlanca = (tecla instanceof TeclaBlanca);
                if (esBlanca == tocanBlancas && visible(tecla, recorte)) {
                    if (enReposo) {
                        tecla.dibujarEnReposo(g);
                    } else {
                        tecla.dibujar(g);
                    }
                }
            }
        }
    }

    @Override
    public void soltarTodas() {
        for (Tecla tecla : this.teclas.values()) {
            if (tecla.estaPulsado()) {
                tecla.soltar();
            }
        }
    }

    /**
     * Aqui todos los canales comparten las mismas teclas, asi que no se puede
     * soltar uno solo: se sueltan todas.
     */
    @Override
    public void soltarCanal(int canal) {
        soltarTodas();
    }

    @Override
    public Tecla teclaEn(int x, int y) {
        Tecla encontrada = buscar(x, y, false);
        if (encontrada == null) {
            encontrada = buscar(x, y, true);
        }
        return encontrada;
    }

    /** Las negras primero, que estan dibujadas encima de las blancas. */
    private Tecla buscar(int x, int y, boolean blancas) {
        for (Tecla tecla : this.teclas.values()) {
            boolean esBlanca = tecla instanceof TeclaBlanca;
            if (esBlanca == blancas && tecla.getForma().contains(x, y)) {
                return tecla;
            }
        }
        return null;
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
        int altoFieltro = this.medidas.getAlturaFieltro();
        double x = getPosicion().x;
        double y = getPosicion().y - altoFieltro;
        g.setPaint(new GradientPaint(
                0f, (float) y, Estilo.FIELTRO_BAJO,
                0f, (float) (y + altoFieltro), Estilo.FIELTRO_ALTO));
        g.fill(new Rectangle2D.Double(x, y, getAnchura(), altoFieltro));
    }

    @Override
    public int getAnchura() {
        return this.medidas.anchuraDe(
                contarBlancas(getTeclaInicial(), getTeclaFinal()));
    }

    @Override
    public int getAltura() {
        return this.medidas.getAlturaBlanca();
    }
}
