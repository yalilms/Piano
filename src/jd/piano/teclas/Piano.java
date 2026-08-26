package jd.piano.teclas;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

public abstract class Piano implements ElementoVisual {
    private int teclaInicial;
    private int teclaFinal;
    private Point posicion;

    public Piano() {
        this.teclaInicial = 0;
        this.teclaFinal = 0;
        this.posicion = null;
    }

    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
    }

    public abstract Tecla getTecla(int canal, int nota);

    /**
     * Los resplandores de las teclas pulsadas, en una pasada aparte.
     * La Escena los pinta sin recortar, porque la luz se derrama mucho mas
     * alla de la tecla y no debe quedarse dentro de la region repintada.
     */
    public abstract void dibujarBrillos(Graphics2D g);

    /**
     * Trozos que hay que repintar porque tienen teclas con color. Permite a la
     * Escena tocar solo esas zonas en vez de las 88 teclas: medido, un blit
     * cuesta 0,5 ms y repintarlas todas 14 ms.
     *
     * Se devuelve una lista y no un solo rectangulo a proposito: en el
     * MultiPiano dos teclas en esquinas opuestas de la rejilla darian una
     * union del tamano de la pantalla y no se ahorraria nada.
     */
    public abstract void anadirRegionesActivas(List<Rectangle> destino);

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

    /**
     * Rectangulo que ocupa el piano, o null si aun no se le ha dado posicion.
     * Lo usa la Escena para saber donde acaba el teclado y colocar el reflejo.
     */
    public Rectangle getLimites() {
        Rectangle limites = null;
        if (this.posicion != null) {
            limites = new Rectangle(this.posicion.x, this.posicion.y,
                    getAnchura(), getAltura());
        }
        return limites;
    }
}
