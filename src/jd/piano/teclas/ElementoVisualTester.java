package jd.piano.teclas;

import bpc.daw.consola.CapaCanvas;
import bpc.daw.consola.Consola;
import bpc.daw.consola.FondoColorSolido;

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
        c1.getCapaFondo().setFondo(new FondoColorSolido(Color.GRAY));
        this.graphics = c1.getCapaCanvas().getGraphics();
    }

    public void setElementoVisual(ElementoVisual e) {
        this.elemento = e;
    }

    public boolean hacerPrueba() {
        boolean exitoPrueba = false;

        this.elemento.setPosicion(120, 90);
        this.elemento.setGraphics(this.graphics);
        this.elemento.dibujar();
        if (this.elemento instanceof Pulsable p) {
            p.setColorPulsado(Color.BLUE);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException error) {
                System.out.println(error.getMessage());
            }
            p.pulsar();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException error) {
                System.out.println(error.getMessage());
            }
            p.soltar();
        }
        Consola c1 = new Consola();
        c1.getCapaTexto().println("¿Ha salido el test bien?");
        char respuesta = c1.getTeclado().leerCaracter();
        if (respuesta == 's' || respuesta == 'S') {
            exitoPrueba = true;
        }
        return exitoPrueba;
    }
}