package jd.piano.programa;

import jd.piano.teclas.Piano;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Arranque del programa: el menu va por la terminal y el piano se dibuja en
 * una ventana Swing.
 *
 * Los mensajes van sin acentos a proposito, porque la consola de Windows no
 * usa UTF-8 por defecto y saldrian caracteres raros.
 */
public class ProgramaSwing {

    private static final int NOTA_INICIAL = 24;
    private static final int NOTA_FINAL = 108;

    private static final int NOTA_INICIAL_MULTI = 48;
    private static final int NOTA_FINAL_MULTI = 71;

    private static final int MARGEN = 40;

    public static void main(String[] args) {
        ListaCanciones lista = new ListaCanciones("canciones");
        List<String> canciones = lista.getCanciones();

        if (canciones.isEmpty()) {
            System.out.println("No hay canciones en la carpeta 'canciones'.");
            System.out.println("Lanza el programa desde la raiz del proyecto.");
            return;
        }

        Scanner teclado = new Scanner(System.in);

        System.out.println("Canciones disponibles:");
        for (int i = 0; i < canciones.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + new File(canciones.get(i)).getName());
        }
        System.out.print("Selecciona una cancion (1-" + canciones.size() + "): ");
        int seleccion = leerEntero(teclado);

        if (seleccion < 1 || seleccion > canciones.size()) {
            System.out.println("Seleccion no valida");
            return;
        }
        String rutaCancion = lista.getCancion(seleccion - 1);

        System.out.println("  1) Piano Sencillo");
        System.out.println("  2) Multi Piano (16 canales)");
        System.out.print("Modo: ");
        int opcion = leerEntero(teclado);

        Piano piano = crearPiano(opcion);
        if (piano == null) {
            System.out.println("Opcion no valida");
            return;
        }

        Escena escena = crearEscena(piano);
        cargarFondo(escena);
        escena.setPiano(piano);
        piano.setPosicion((escena.getAnchura() - piano.getAnchura()) / 2,
                (escena.getAltura() - piano.getAltura()) / 2);
        // Con el piano ya colocado y antes de que suene nada: las teclas se
        // hornean en reposo dentro de la imagen cacheada.
        escena.prepararCaches();

        Lienzo lienzo = new Lienzo("Piano MIDI - " + new File(rutaCancion).getName(), escena);
        lienzo.mostrar();

        System.out.println("Sonando... cierra la ventana para salir.");
        try {
            ReproductorMidi reproductor = new ReproductorMidi();
            reproductor.conectar(piano);
            reproductor.reproducir(rutaCancion);
        } catch (Exception error) {
            System.out.println("Error reproduciendo el MIDI: " + error.getMessage());
        }
    }

    private static Piano crearPiano(int opcion) {
        Piano piano = null;
        if (opcion == 1) {
            piano = new PianoSencillo(NOTA_INICIAL, NOTA_FINAL);
        } else if (opcion == 2) {
            piano = new MultiPiano(NOTA_INICIAL_MULTI, NOTA_FINAL_MULTI);
        }
        return piano;
    }

    /** La escena se ajusta al piano, sin pasarse del tamano de la pantalla. */
    private static Escena crearEscena(Piano piano) {
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = Math.min(piano.getAnchura() + 2 * MARGEN, pantalla.width);
        int alto = Math.min(piano.getAltura() + 2 * MARGEN, pantalla.height - 80);
        return new Escena(ancho, alto);
    }

    private static void cargarFondo(Escena escena) {
        File file = new File("madera.jpg");
        if (!file.exists()) {
            System.out.println("Aviso: no encuentro madera.jpg, fondo liso.");
            return;
        }
        try {
            Image imagen = ImageIO.read(file);
            escena.setFondo(imagen);
        } catch (IOException error) {
            System.out.println("Aviso: no puedo leer madera.jpg (" + error.getMessage() + ")");
        }
    }

    private static int leerEntero(Scanner teclado) {
        int valor = -1;
        if (teclado.hasNextInt()) {
            valor = teclado.nextInt();
        } else if (teclado.hasNext()) {
            teclado.next();
        }
        return valor;
    }
}
