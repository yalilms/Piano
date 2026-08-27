package jd.piano.teclas;

import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.PiezaPulsable;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

public abstract class Piano implements InstrumentoVisual {
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

    @Override
    public String getNombre() {
        return "Piano";
    }

    /** Para el contrato de instrumento: en un piano la pieza es la tecla. */
    @Override
    public PiezaPulsable piezaDe(int canal, int nota) {
        return getTecla(canal, nota);
    }

    @Override
    public PiezaPulsable piezaEn(int x, int y) {
        return teclaEn(x, y);
    }

    /**
     * Cambia el tamano de las teclas conservando su estado y su identidad.
     * Importante que no se reconstruyan: el reproductor MIDI guarda referencias
     * a las teclas y recrearlas las dejaria colgadas.
     */
    public abstract void setMedidas(MedidasTeclado medidas);

    public abstract MedidasTeclado getMedidas();

    /**
     * Suelta todas las teclas. Hace falta al pausar, parar o saltar de
     * posicion: el secuenciador no manda los NOTE_OFF de las notas que estaban
     * sonando y se quedarian encendidas para siempre.
     */
    public abstract void soltarTodas();

    /**
     * Suelta las teclas de un canal concreto. En el piano sencillo todos los
     * canales comparten teclado y no se puede distinguir, asi que suelta
     * todas; en el multipiano solo toca el teclado de ese canal.
     */
    public abstract void soltarCanal(int canal);

    /**
     * La tecla que hay en ese punto, o null si no hay ninguna. Sale gratis de
     * tener las teclas como formas: basta preguntarle si contiene el punto.
     * Las negras se comprueban antes porque estan encima.
     */
    public abstract Tecla teclaEn(int x, int y);

    /**
     * Dibuja el teclado apagado, ignorando que teclas estan pulsadas.
     * La Escena lo usa para hornear su imagen cacheada, y necesita que salga
     * en reposo aunque la cancion ya este sonando.
     */
    public abstract void dibujarEnReposo(Graphics2D g);

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

    /** Cuantas teclas blancas hay en un rango de notas MIDI. */
    public static int contarBlancas(int notaInicial, int notaFinal) {
        int blancas = 0;
        for (int n = notaInicial; n <= notaFinal; n++) {
            int resto = ((n % 12) + 12) % 12;
            if (resto != 1 && resto != 3 && resto != 6 && resto != 8 && resto != 10) {
                blancas++;
            }
        }
        return blancas;
    }
}
