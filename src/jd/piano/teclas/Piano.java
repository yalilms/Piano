package jd.piano.teclas;

import javax.swing.text.Position;
import java.awt.*;

public abstract class Piano implements ElementoVisual{
    private int teclaInicial;
    private int teclaFinal;
    private Position posicion;
    private Graphics graphics;

    public Piano(){
        this.teclaInicial = 0;
        this.teclaFinal = 0;
        this.posicion = null;
        this.graphics = null;
    }
    public abstract Tecla getTecla(int canal, int nota);

    public int getTeclaInicial() {
        return this.teclaInicial;
    }

    public int getTeclaFinal() {
        return this.teclaFinal;
    }
}
