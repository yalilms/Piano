package jd.piano.programa;

import jd.piano.teclas.Piano;
import jd.piano.teclas.Tecla;
import jd.piano.teclas.TeclaBlanca;
import jd.piano.teclas.TeclaFactory;
import jd.piano.teclas.TeclaNegra;

import java.awt.Graphics2D;
import java.util.HashMap;
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
     * Primero todas las blancas y despues todas las negras, en dos pasadas.
     * Asi las negras y sus sombras quedan siempre por encima, sin depender del
     * orden de las notas.
     */
    @Override
    public void dibujar(Graphics2D g) {
        for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaBlanca) {
                tecla.dibujar(g);
            }
        }
        for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaNegra) {
                tecla.dibujar(g);
            }
        }
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
