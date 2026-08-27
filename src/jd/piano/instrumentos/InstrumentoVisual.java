package jd.piano.instrumentos;

import jd.piano.teclas.Estilo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Un instrumento que se puede ver y que responde al MIDI.
 *
 * Hasta ahora el programa hablaba directamente con Piano y con Tecla, asi que
 * lo unico que se podia dibujar era un teclado. Este es el contrato que
 * necesitan la Escena, el reproductor y la cascada, y lo cumplen tanto el
 * piano como el mastil de la guitarra.
 *
 * Lo unico que hay que resolver de verdad es piezaDe: donde cae cada nota. Lo
 * demas sale de ahi.
 */
public interface InstrumentoVisual extends ElementoVisual {

    /** Nombre corto para los menus y el panel lateral. */
    String getNombre();

    /**
     * La pieza donde cae esa nota, o null si el instrumento no la representa:
     * una guitarra no llega a las notas mas graves del piano.
     */
    PiezaPulsable piezaDe(int canal, int nota);

    /** Rectangulo que ocupa, o null si aun no se le ha dado posicion. */
    Rectangle getLimites();

    /**
     * Reescala el instrumento para que ocupe como mucho ese hueco.
     * No debe reconstruir sus piezas: el reproductor guarda referencias.
     */
    void ajustarA(int ancho, int alto);

    /** Dibuja todo apagado, para la imagen que la Escena tiene cacheada. */
    void dibujarEnReposo(Graphics2D g);

    /** Los resplandores, en una pasada aparte y sin recortar. */
    void dibujarBrillos(Graphics2D g);

    /** Trozos que hay que repintar porque tienen piezas con color. */
    void anadirRegionesActivas(List<Rectangle> destino);

    void soltarTodas();

    /** Suelta lo de un canal concreto, si el instrumento los distingue. */
    void soltarCanal(int canal);

    /**
     * Que suena una nota. Devuelve true si el instrumento la representa, para
     * que quien llame pueda saber si se ha perdido.
     */
    default boolean pulsar(int canal, int nota, int velocidad) {
        PiezaPulsable pieza = piezaDe(canal, nota);
        if (pieza == null) {
            return false;
        }
        pieza.setColorPulsado(Estilo.colorDeCanal(canal));
        pieza.pulsar(velocidad);
        return true;
    }

    default void soltar(int canal, int nota) {
        PiezaPulsable pieza = piezaDe(canal, nota);
        if (pieza != null) {
            pieza.soltar();
        }
    }

    /**
     * Donde aterriza esa nota, para que las notas cayendo sepan sobre que
     * pintar. Null si el instrumento no la representa o aun no tiene sitio.
     */
    default Rectangle donde(int canal, int nota) {
        PiezaPulsable pieza = piezaDe(canal, nota);
        Rectangle donde = null;
        if (pieza != null && getLimites() != null) {
            donde = pieza.getForma().getBounds();
        }
        return donde;
    }

    /** La pieza que hay en ese punto, o null. Sirve para tocar con el raton. */
    PiezaPulsable piezaEn(int x, int y);

    /**
     * Si tiene sentido que le caigan notas desde arriba. Casi todos dicen que
     * si: las piezas estan en una franja y las barras bajan hasta ella. El
     * circulo cromatico dice que no, porque sus notas estan repartidas por toda
     * una rueda y las barras la cruzarian por encima para llegar a las de abajo.
     */
    default boolean admiteCascada() {
        return true;
    }

    /**
     * Si debajo se pinta la tapa laqueada con su reflejo. Vale para lo que se
     * apoya en algo (un teclado, un xilofono, un mastil); no para lo que no,
     * como el circulo cromatico, donde la franja oscura salia cruzando la
     * madera por la mitad y cortando la rueda.
     */
    default boolean tieneReflejo() {
        return true;
    }
}
