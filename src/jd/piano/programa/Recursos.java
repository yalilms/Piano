package jd.piano.programa;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Encuentra la imagen de fondo y la carpeta de canciones.
 *
 * Se buscan primero en el directorio de trabajo, que es lo que hacia el
 * programa desde el principio, y si no estan se prueba junto a las clases
 * compiladas. Asi lanzarlo desde otro directorio ya no deja el programa sin
 * fondo y sin canciones.
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
