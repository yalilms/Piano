package jd.piano.teclas;

import java.awt.*;

public abstract class Piano implements ElementoVisual{
    protected int teclaInicial;
    protected int teclaFinal;
    protected Point posicion;
    protected Graphics graphics;

    public Piano(){
        this.teclaInicial = 0;
        this.teclaFinal = 0;
        this.posicion = null;
        this.graphics = null;
    }
    
    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
    }
    
    @Override
    public void setGraphics(Graphics g) {
        this.graphics = g;
    }
    
    public abstract Tecla getTecla(int canal, int nota);

    public int getTeclaInicial() {
        return this.teclaInicial;
    }

    public int getTeclaFinal() {
        return this.teclaFinal;
    }
}
