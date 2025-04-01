package jd.piano.teclas;

import bpc.daw.consola.CapaCanvas;
import bpc.daw.consola.Consola;

import java.awt.*;

public class ElementoVisualTester {
    private Graphics graphics;
    private ElementoVisual elemento;

    public ElementoVisualTester(Graphics g) {
        this.graphics = g;
        this.elemento = null;
    }

    public ElementoVisualTester() {
        this.elemento = null;
        Consola c1 = new Consola();
        this.graphics = c1.getCapaCanvas().getGraphics();
    }

    public void setElementoVisual(ElementoVisual e) {
        this.elemento = e;
    }

    public boolean hacerPrueba() {
        throw new UnsupportedOperationException();
    }
}