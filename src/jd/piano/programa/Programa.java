package jd.piano.programa;
import bpc.daw.consola.*;
import jd.piano.teclas.Piano;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Programa {

    private static final int NOTA_INICIAL = 24;
    private static final int NOTA_FINAL = 108;

    // El multipiano dibuja 16 teclados a la vez, por eso usa un rango mas corto
    private static final int NOTA_INICIAL_MULTI = 48;
    private static final int NOTA_FINAL_MULTI = 71;

    public static void main(String[] args) {
        Consola consola = new Consola();
        CapaFondo capaFondo = consola.getCapaFondo();

        int coordx = Toolkit.getDefaultToolkit().getScreenSize().width;
        int coordy = Toolkit.getDefaultToolkit().getScreenSize().height;

        try{
            File file = new File("madera.jpg");
            Image image = ImageIO.read(file);
            capaFondo.setFondo(new FondoImagen(image));
        } catch (IOException error){
            System.out.println(error.getMessage());
        }
        CapaTexto texto = consola.getCapaTexto();
        Teclado teclado = consola.getTeclado();

        ListaCanciones lista = new ListaCanciones("canciones");
        List<String> canciones = lista.getCanciones();
        boolean ejecutar = !canciones.isEmpty();
        Piano piano = null;
        String rutaCancion = null;

        if (ejecutar) {
            texto.println("Canciones disponibles:");
            for (int i = 0; i < canciones.size(); i++) {
                texto.println((i + 1) + ") " + new File(canciones.get(i)).getName());
            }

            texto.println("\nSelecciona una canción (1-" + canciones.size() + "):");
            int seleccion = teclado.leerNumeroEntero();
            
            ejecutar = seleccion >= 1 && seleccion <= canciones.size();
            
            if (ejecutar) {
                rutaCancion = lista.getCancion(seleccion - 1);

                texto.println("""
                        1) Piano Sencillo
                        2) Multi Piano
                        """);
                int opcion = teclado.leerNumeroEntero();
                texto.cls();

                Graphics g = consola.getCapaCanvas().getGraphics();
                
                if (opcion == 1) {
                    piano = new PianoSencillo(NOTA_INICIAL, NOTA_FINAL);
                } else if (opcion == 2) {
                    piano = new MultiPiano(NOTA_INICIAL_MULTI, NOTA_FINAL_MULTI);
                } else {
                    ejecutar = false;
                    texto.println("Opción no válida");
                }

                if (ejecutar) {
                    piano.setPosicion((coordx - piano.getAnchura()) / 2,
                            (coordy - piano.getAltura()) / 2);
                    piano.setGraphics(g);
                    piano.dibujar();
                }
            } else {
                texto.println("Selección no válida");
            }
        } else {
            texto.println("No hay canciones disponibles");
        }

        if (ejecutar && piano != null && rutaCancion != null) {
            try {
                ReproductorMidi reproductor = new ReproductorMidi();
                reproductor.conectar(piano);
                reproductor.reproducir(rutaCancion);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
