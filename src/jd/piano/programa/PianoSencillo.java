package jd.piano.programa;

import jd.piano.teclas.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PianoSencillo extends Piano {

    private Map<Integer, Tecla> teclas;

    public PianoSencillo(int teclaInicial, int teclaFinal){
        setTeclaInicial(teclaInicial);
        setTeclaFinal(teclaFinal);
        this.teclas = new HashMap<>();
        for (int i = teclaInicial; i<=teclaFinal; i++) {
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
        int y1 = y;

        for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaBlanca) {
                tecla.setPosicion(x1, y1);
                x1 += TeclaBlanca.ANCHURA;
            }
            if (tecla instanceof TeclaNegra) {
                int posX = x;
                tecla.setPosicion(posX, y1);
            }
        }
    }

    @Override
    public void setGraphics(Graphics g) {
        super.setGraphics(g);
        for (Tecla tecla : this.teclas.values()) {
            tecla.setGraphics(g);
        }
    }

    @Override
    public void dibujar() {
        // Primero dibujamos las teclas blancas
        for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaBlanca) {
                tecla.dibujar();
            }
        }
        
        // Luego dibujamos las teclas negras encima
        for (int i = getTeclaInicial(); i <= getTeclaFinal(); i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaNegra) {
                tecla.dibujar();
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
