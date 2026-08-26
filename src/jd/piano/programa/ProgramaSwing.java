package jd.piano.programa;

import jd.piano.teclas.LayoutTeclados;
import jd.piano.teclas.MedidasTeclado;
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

    private static final int MARGEN = 56;

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
        System.out.println("  2) Multi Piano (un teclado por canal)");
        System.out.print("Modo: ");
        int opcion = leerEntero(teclado);

        if (opcion != 1 && opcion != 2) {
            System.out.println("Opcion no valida");
            return;
        }

        try {
            arrancar(rutaCancion, opcion == 2);
        } catch (Exception error) {
            System.out.println("Error: " + error.getMessage());
        }
    }

    private static void arrancar(String rutaCancion, boolean multi) throws Exception {
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        int anchoVentana = (int) (pantalla.width * 0.9);
        int altoVentana = (int) (pantalla.height * 0.75);

        AnalisisMidi analisis = AnalisisMidi.de(rutaCancion);
        Escena escena = new Escena(anchoVentana, altoVentana);
        Piano piano;

        if (multi) {
            List<Integer> canales = analisis.getCanalesUsados();
            System.out.println("La cancion usa " + canales.size() + " canales, notas "
                    + analisis.getNotaMinima() + " a " + analisis.getNotaMaxima());
            piano = new MultiPiano(canales, NOTA_INICIAL, NOTA_FINAL,
                    layoutPara(canales.size(), anchoVentana, altoVentana));
        } else {
            piano = new PianoSencillo(NOTA_INICIAL, NOTA_FINAL, MedidasTeclado.NORMAL);
        }

        escena.setPiano(piano);
        cargarFondo(escena);
        colocar(escena, piano, multi);

        Lienzo lienzo = new Lienzo("Piano MIDI - " + new File(rutaCancion).getName(), escena);
        lienzo.setAlRedimensionar(() -> colocar(escena, escena.getPiano(), multi));
        lienzo.mostrar();

        System.out.println("Sonando... cierra la ventana para salir.");
        ReproductorMidi reproductor = new ReproductorMidi();
        reproductor.conectar(piano);
        reproductor.reproducir(rutaCancion);
    }

    private static LayoutTeclados layoutPara(int cuantosCanales, int ancho, int alto) {
        return LayoutTeclados.calcular(cuantosCanales,
                Piano.contarBlancas(NOTA_INICIAL, NOTA_FINAL),
                ancho - 2 * MARGEN, alto - 2 * MARGEN);
    }

    /**
     * Recalcula escala y posicion para el tamano actual de la escena. Se llama
     * al arrancar y cada vez que se redimensiona la ventana.
     */
    private static void colocar(Escena escena, Piano piano, boolean multi) {
        if (piano == null) {
            return;
        }
        if (multi && piano instanceof MultiPiano mp) {
            mp.setLayout(layoutPara(mp.getCanales().size(),
                    escena.getAnchura(), escena.getAltura()));
        }
        piano.setPosicion((escena.getAnchura() - piano.getAnchura()) / 2,
                (escena.getAltura() - piano.getAltura()) / 2);
        escena.prepararCaches();
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
