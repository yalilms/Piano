package jd.piano.teclas;

import javax.swing.text.Position;
import java.awt.*;

public abstract class Piano {
    private int teclaInicial;
    private int teclaFinal;
    private Position posicion;
    private Graphics graphics;

    public Piano(){

    }
    public abstract Tecla getTecla(int canal, int nota);

    public int getTeclaInicial() {
        return this.teclaInicial;
    }

    public int getTeclaFinal() {
        return this.teclaFinal;
    }
}
