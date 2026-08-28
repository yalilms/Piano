package jd.piano.programa;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Lo que el programa recuerda de una vez para la siguiente.
 *
 * Hasta ahora se abria siempre igual: teclado, tamano por defecto y el
 * instrumento del principio, da igual como lo hubieras dejado. Aqui se guarda
 * lo que cuesta volver a poner cada vez.
 *
 * Lo que no se guarda, a proposito: la cancion, el tempo y el tono. Los tres
 * son decisiones de un rato concreto, y recuperarlos en silencio sorprende mas
 * que ayuda: abrir el programa y encontrarse la cancion sonando a media
 * velocidad y en otro tono no se entiende. Y la cancion, ademas, es justo lo
 * que la pantalla de inicio esta para preguntar.
 *
 * Nada de esto puede tumbar el programa. Preferences lee y escribe en el
 * registro de Windows, y hay maquinas donde eso esta cerrado; si falla, se
 * sigue con los valores por defecto y no se dice nada.
 */
public final class Preferencias {

    private static final String RUTA = "jd/piano/midi";

    public static final String MODO = "modo";
    public static final String VER_COMO = "verComo";
    public static final String TOCAS_CON = "tocasCon";
    public static final String NOMBRES_DE_NOTA = "nombresDeNota";
    public static final String ENCADENAR = "encadenar";
    public static final String ANCHO = "ventanaAncho";
    public static final String ALTO = "ventanaAlto";
    public static final String MAXIMIZADA = "ventanaMaximizada";

    private Preferencias() {
    }

    private static Preferences nodo() {
        try {
            return Preferences.userRoot().node(RUTA);
        } catch (RuntimeException error) {
            return null;
        }
    }

    public static int getEntero(String clave, int porDefecto) {
        Preferences p = nodo();
        if (p == null) {
            return porDefecto;
        }
        try {
            return p.getInt(clave, porDefecto);
        } catch (RuntimeException error) {
            return porDefecto;
        }
    }

    public static void setEntero(String clave, int valor) {
        Preferences p = nodo();
        if (p == null) {
            return;
        }
        try {
            p.putInt(clave, valor);
        } catch (RuntimeException error) {
            // se queda sin guardar y no pasa nada
        }
    }

    public static boolean getBooleano(String clave, boolean porDefecto) {
        Preferences p = nodo();
        if (p == null) {
            return porDefecto;
        }
        try {
            return p.getBoolean(clave, porDefecto);
        } catch (RuntimeException error) {
            return porDefecto;
        }
    }

    public static void setBooleano(String clave, boolean valor) {
        Preferences p = nodo();
        if (p == null) {
            return;
        }
        try {
            p.putBoolean(clave, valor);
        } catch (RuntimeException error) {
            // idem
        }
    }

    /**
     * Un valor de enumeracion, guardado por su nombre y no por su posicion:
     * asi anadir o reordenar modos no convierte lo guardado en otra cosa.
     */
    public static <T extends Enum<T>> T getEnum(Class<T> tipo, String clave,
                                                T porDefecto) {
        Preferences p = nodo();
        if (p == null) {
            return porDefecto;
        }
        try {
            String nombre = p.get(clave, null);
            return nombre == null ? porDefecto : Enum.valueOf(tipo, nombre);
        } catch (RuntimeException error) {
            // Puede ser un valor guardado por una version anterior que ya no
            // existe: IllegalArgumentException de valueOf entra por aqui
            return porDefecto;
        }
    }

    public static void setEnum(String clave, Enum<?> valor) {
        Preferences p = nodo();
        if (p == null || valor == null) {
            return;
        }
        try {
            p.put(clave, valor.name());
        } catch (RuntimeException error) {
            // idem
        }
    }

    /** Fuerza la escritura al disco. Se llama al cerrar la ventana. */
    public static void guardar() {
        Preferences p = nodo();
        if (p == null) {
            return;
        }
        try {
            p.flush();
        } catch (BackingStoreException | RuntimeException error) {
            // idem
        }
    }

    /** Borra una clave concreta. Lo usan las comprobaciones para no dejar rastro. */
    public static void olvidar(String clave) {
        Preferences p = nodo();
        if (p == null) {
            return;
        }
        try {
            p.remove(clave);
        } catch (RuntimeException error) {
            // idem
        }
    }
}
