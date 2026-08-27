package jd.piano.instrumentos;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Un instrumento que se puede ver y que responde al MIDI.
 *
 * Hasta ahora el programa hablaba directamente con Piano y con Tecla, asi que
 * lo unico que se podia dibujar era un teclado. Este es el contrato que
 * necesitan la Escena, el reproductor y la cascada, y lo cumplen tanto el
 * piano como el mastil de la guitarra o los pads de la bateria.
 */
public interface InstrumentoVisual {

    /** Nombre corto para los menus y el panel lateral. */
    String getNombre();

    void setPosicion(int x, int y);

    /** Rectangulo que ocupa, o null si aun no se le ha dado posicion. */
    Rectangle getLimites();

    int getAnchura();

    int getAltura();

    /**
     * Que se toca una nota. Devuelve true si el instrumento la representa: una
     * guitarra no llega a las notas mas graves del piano, y quien llame puede
     * querer saberlo.
     */
    boolean pulsar(int canal, int nota, int velocidad);

    void soltar(int canal, int nota);

    void soltarTodas();

    /** Suelta lo de un canal concreto, si el instrumento los distingue. */
    void soltarCanal(int canal);

    /**
     * Donde aterriza esa nota, para que las notas cayendo sepan sobre que
     * pintar. Null si el instrumento no la representa.
     */
    Rectangle donde(int canal, int nota);

    /**
     * La pieza que hay en ese punto, o null. Sirve para tocar con el raton.
     */
    PiezaPulsable piezaEn(int x, int y);

    void dibujar(Graphics2D g);

    /** Dibuja todo apagado, para la imagen que la Escena tiene cacheada. */
    void dibujarEnReposo(Graphics2D g);

    /** Los resplandores, en una pasada aparte y sin recortar. */
    void dibujarBrillos(Graphics2D g);

    /** Trozos que hay que repintar porque tienen piezas con color. */
    void anadirRegionesActivas(List<Rectangle> destino);

    /**
     * Reescala el instrumento para que ocupe como mucho ese hueco.
     * No debe reconstruir sus piezas: el reproductor guarda referencias.
     */
    void ajustarA(int ancho, int alto);
}
