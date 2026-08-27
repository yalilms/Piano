package jd.piano.programa;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Encuentra la imagen de fondo y la carpeta de canciones, y decide donde se
 * pueden escribir las capturas.
 *
 * Los recursos se buscan primero en el directorio de trabajo, que es lo que
 * hacia el programa desde el principio, y si no estan se prueba junto a las
 * clases compiladas. Asi lanzarlo desde otro directorio ya no deja el programa
 * sin fondo y sin canciones, y funciona igual dentro de un jar empaquetado.
 */
public final class Recursos {

    private Recursos() {
    }

    /** @return el fichero o directorio encontrado, o null si no esta. */
    public static File buscar(String nombre) {
        File encontrado = null;
        for (File base : basesPosibles()) {
            if (base == null) {
                continue;
            }
            File candidato = new File(base, nombre);
            if (candidato.exists()) {
                encontrado = candidato;
                break;
            }
        }
        return encontrado;
    }

    /**
     * Directorios donde puede estar: el de trabajo, y subiendo desde donde
     * estan las clases (out/ o el jar) hasta tres niveles.
     */
    private static File[] basesPosibles() {
        File trabajo = new File(".").getAbsoluteFile();
        File[] bases = new File[]{trabajo, null, null, null};

        File desdeClases = directorioDeLasClases();
        for (int i = 1; i < bases.length && desdeClases != null; i++) {
            bases[i] = desdeClases;
            desdeClases = desdeClases.getParentFile();
        }
        return bases;
    }

    /**
     * Donde guardar las capturas.
     *
     * Ejecutandolo desde el proyecto van a capturas/ al lado, que es lo comodo
     * al desarrollar. Empaquetado van a la carpeta del usuario: el programa
     * puede estar en Program Files, donde no se puede escribir, y el directorio
     * de trabajo lo pone quien lo lance, asi que escribir ahi dejaria carpetas
     * sueltas por donde caiga.
     *
     * Para distinguir los dos casos basta con mirar si el fondo esta en el
     * directorio de trabajo: solo lo esta cuando se arranca desde el proyecto.
     */
    public static File carpetaDeCapturas() {
        File alLado = new File("capturas").getAbsoluteFile();
        if (new File("madera.jpg").isFile() && sePuedeEscribir(alLado)) {
            return alLado;
        }
        File delUsuario = new File(System.getProperty("user.home"),
                "Piano MIDI");
        if (sePuedeEscribir(delUsuario)) {
            return delUsuario;
        }
        // Ultimo recurso: el temporal del sistema, que siempre existe
        return new File(System.getProperty("java.io.tmpdir"), "Piano MIDI");
    }

    /**
     * Si se puede crear y escribir de verdad. No basta con canWrite: en Windows
     * dice que si sobre carpetas protegidas y el fallo salta luego, al guardar.
     */
    private static boolean sePuedeEscribir(File carpeta) {
        boolean vale = false;
        try {
            if (carpeta.isDirectory() || carpeta.mkdirs()) {
                File prueba = new File(carpeta, ".prueba-de-escritura");
                vale = prueba.createNewFile() && prueba.delete();
            }
        } catch (java.io.IOException | SecurityException error) {
            vale = false;
        }
        return vale;
    }

    private static File directorioDeLasClases() {
        File directorio = null;
        try {
            URL url = Recursos.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            File fichero = new File(url.toURI());
            directorio = fichero.isDirectory() ? fichero : fichero.getParentFile();
        } catch (URISyntaxException | RuntimeException error) {
            directorio = null;
        }
        return directorio;
    }
}
