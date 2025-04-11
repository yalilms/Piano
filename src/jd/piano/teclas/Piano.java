package jd.piano.teclas;

import java.awt.*;

public abstract class Piano implements ElementoVisual{
    private int teclaInicial;
    private int teclaFinal;
    private Point posicion;
    private Graphics graphics;

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

    protected void setTeclaInicial(int ti) {
        this.teclaInicial = ti;
    }

    protected void setTeclaFinal(int tf) {
        this.teclaFinal = tf;
    }

    protected Point getPosicion() {
        return this.posicion;
    }

    protected Graphics getGraphics() {
        return this.graphics;
    }
}
