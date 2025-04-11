package jd.piano.programa;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ListaCanciones {
    private List<String> canciones;
    private String directorioCanciones;

    public ListaCanciones(String directorio) {
        this.directorioCanciones = directorio;
        this.canciones = new ArrayList<>();
        cargarCanciones();
    }

    private void cargarCanciones() {
        File directorio = new File(directorioCanciones);
        if (directorio.exists() && directorio.isDirectory()) {
            File[] archivos = directorio.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".mid")) {
                        canciones.add(archivo.getAbsolutePath());
                    }
                }
            }
        }
    }

    public List<String> getCanciones() {
        return new ArrayList<>(canciones);
    }

    public String getCancion(int indice) {
        if (indice >= 0 && indice < canciones.size()) {
            return canciones.get(indice);
        }
        return null;
    }

    public int getNumeroCanciones() {
        return canciones.size();
    }
} 