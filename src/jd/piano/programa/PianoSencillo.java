package jd.piano.programa;

import jd.piano.teclas.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PianoSencillo extends Piano {

    private Map<Integer, Tecla> teclas;

    public PianoSencillo(int teclaInicial, int teclaFinal){
        super();
        this.teclaInicial = teclaInicial;
        this.teclaFinal = teclaFinal;
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
        
        // Primero posicionamos las teclas blancas
        for (int i = teclaInicial; i <= teclaFinal; i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaBlanca) {
                tecla.setPosicion(x1, y1);
                x1 += TeclaBlanca.ANCHURA;
            }
        }
        
        // Luego posicionamos las teclas negras
        for (int i = teclaInicial; i <= teclaFinal; i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaNegra) {
                int notaEquivalente = i % 12;
                int octava = (i - teclaInicial) / 12;
                int posX = x;
                
                switch (notaEquivalente) {
                    case 1: // C#
                        posX += octava * 7 * TeclaBlanca.ANCHURA + TeclaBlanca.ANCHURA - TeclaNegra.ANCHURA/2;
                        break;
                    case 3: // D#
                        posX += octava * 7 * TeclaBlanca.ANCHURA + 2 * TeclaBlanca.ANCHURA - TeclaNegra.ANCHURA/2;
                        break;
                    case 6: // F#
                        posX += octava * 7 * TeclaBlanca.ANCHURA + 4 * TeclaBlanca.ANCHURA - TeclaNegra.ANCHURA/2;
                        break;
                    case 8: // G#
                        posX += octava * 7 * TeclaBlanca.ANCHURA + 5 * TeclaBlanca.ANCHURA - TeclaNegra.ANCHURA/2;
                        break;
                    case 10: // A#
                        posX += octava * 7 * TeclaBlanca.ANCHURA + 6 * TeclaBlanca.ANCHURA - TeclaNegra.ANCHURA/2;
                        break;
                }
                
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
        for (int i = teclaInicial; i <= teclaFinal; i++) {
            Tecla tecla = this.teclas.get(i);
            if (tecla instanceof TeclaBlanca) {
                tecla.dibujar();
            }
        }
        
        // Luego dibujamos las teclas negras encima
        for (int i = teclaInicial; i <= teclaFinal; i++) {
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
