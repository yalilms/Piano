package jd.piano.programa;
import bpc.daw.consola.*;
import jd.piano.teclas.Piano;

import java.io.File;

public class Programa {
    public static void main(String[] args) {
        Consola consola = new Consola();
        CapaFondo capaFondo = consola.getCapaFondo();
        capaFondo.setFondo(new FondoColorSolido(0, 0, 70));

        CapaTexto capaTexto = consola.getCapaTexto();
        Teclado teclado = consola.getTeclado();
        capaTexto.println("Introduce ruta de un archivo: ");
        String ruta = teclado.leerCadenaCaracteres();

        File file = new File(ruta);
        if (!file.exists()) {
            throw new IllegalArgumentException("ERROR, NO EXISTE UN ARCHIVO CON ESA RUTA");
        }

        capaTexto.println("""
                Donde desea reproducir la canción:
                1) Piano Sencillo
                2) Multi Piano
                """);
        int opcion = teclado.leerNumeroEntero();

        Piano piano = null;
         switch (opcion) {
            case 1 -> {
                piano = new PianoSencillo(24, 108);
                piano.setPosicion(0, 0);
                piano.setGraphics(consola.getCapaCanvas().getGraphics());
                piano.dibujar();
            }
             case 2 -> {
                 piano = new MultiPiano(24, 108);
                 piano.setPosicion(0, 0);
                 piano.setGraphics(consola.getCapaCanvas().getGraphics());
                 piano.dibujar();
             }
             default -> System.out.println("OPCION NO VALIDA");
         };

        try {
            ReproductorMidi reproductorMidi = new ReproductorMidi();
            reproductorMidi.conectar(piano);
            reproductorMidi.reproducir(ruta);
        } catch (Exception error) {
            System.out.println(error.getMessage());
        }

    }
}
