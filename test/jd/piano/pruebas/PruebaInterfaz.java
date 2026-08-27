package jd.piano.pruebas;

import jd.piano.programa.Iconos;
import jd.piano.programa.InstrumentosGM;
import jd.piano.programa.ListaCanciones;
import jd.piano.programa.PianoSencillo;
import jd.piano.programa.Recursos;
import jd.piano.programa.SintetizadorTeclado;
import jd.piano.teclas.Estilo;
import jd.piano.teclas.Piano;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/** Nombres de nota, instrumentos, icono, recursos y teclado del ordenador. */
public final class PruebaInterfaz {

    private PruebaInterfaz() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("interfaz y entrada");
        nombresDeNota(c);
        instrumentos(c);
        icono(c);
        recursos(c);
        tecladoDelOrdenador(c);
        return c;
    }

    private static void nombresDeNota(Comprobador c) {
        c.comprobar("el Do central es la nota 60 y se llama C4",
                Estilo.DO_CENTRAL == 60 && "C4".equals(Estilo.etiquetaDe(60)));
        c.comprobar("las octavas cuadran en los extremos",
                "C1".equals(Estilo.etiquetaDe(24)) && "C8".equals(Estilo.etiquetaDe(108)));
        c.comprobar("solo los Do llevan numero de octava",
                "D".equals(Estilo.etiquetaDe(62)) && "B".equals(Estilo.etiquetaDe(71)));

        boolean distintos = true;
        for (int a = 60; a < 72 && distintos; a++) {
            for (int b = a + 1; b < 72 && distintos; b++) {
                distintos = !Estilo.nombreDeNota(a).equals(Estilo.nombreDeNota(b));
            }
        }
        c.comprobar("las doce notas de la octava tienen nombres distintos", distintos);

        boolean todas = true;
        for (int n = 0; n <= 127 && todas; n++) {
            todas = Estilo.etiquetaDe(n) != null && !Estilo.etiquetaDe(n).isBlank();
        }
        c.comprobar("las 128 notas MIDI tienen etiqueta", todas);
    }

    private static void instrumentos(Comprobador c) {
        c.comprobar("hay 128 instrumentos General MIDI",
                InstrumentosGM.cuantos() == 128);
        boolean todos = true;
        for (int i = 0; i < 128 && todos; i++) {
            todos = InstrumentosGM.nombre(i) != null
                    && !InstrumentosGM.nombre(i).isBlank();
        }
        c.comprobar("ningun nombre de instrumento esta vacio", todos);
        c.comprobar("un programa fuera de rango da un nombre de reserva",
                InstrumentosGM.nombre(500) != null
                        && !InstrumentosGM.nombre(500).isBlank());
    }

    private static void icono(Comprobador c) {
        List<BufferedImage> iconos = Iconos.deLaAplicacion();
        c.comprobar("el icono viene en varios tamanos", iconos.size() >= 4);

        boolean pintados = true;
        for (BufferedImage img : iconos) {
            int opacos = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    if (((img.getRGB(x, y) >>> 24) & 0xFF) > 128) {
                        opacos++;
                    }
                }
            }
            pintados = pintados
                    && opacos > img.getWidth() * img.getHeight() / 4;
        }
        c.comprobar("ningun tamano del icono sale en blanco", pintados);
    }

    /**
     * Los recursos se tienen que encontrar aunque el programa se lance desde
     * otro directorio, que es lo que fallaba desde el principio.
     */
    private static void recursos(Comprobador c) {
        File fondo = Recursos.buscar("madera.jpg");
        File canciones = Recursos.buscar("canciones");
        c.dato("directorio de trabajo: " + new File(".").getAbsolutePath());
        c.comprobar("encuentra la imagen de fondo", fondo != null && fondo.isFile());
        c.comprobar("encuentra la carpeta de canciones",
                canciones != null && canciones.isDirectory());
        c.comprobar("las canciones se cargan",
                new ListaCanciones("canciones").getNumeroCanciones() > 0);
        c.comprobar("un recurso inexistente devuelve null",
                Recursos.buscar("no-existe-esto.xyz") == null);

        // Instalado en Program Files no se puede escribir al lado del programa,
        // asi que la carpeta de capturas tiene que salir siempre utilizable
        File capturas = Recursos.carpetaDeCapturas();
        c.dato("carpeta de capturas: " + capturas.getPath());
        boolean sirve = false;
        try {
            File prueba = new File(capturas, ".prueba-de-la-prueba");
            sirve = capturas.isDirectory() && prueba.createNewFile()
                    && prueba.delete();
        } catch (Exception error) {
            sirve = false;
        }
        c.comprobar("la carpeta de capturas existe y se puede escribir", sirve);
        c.comprobar("desde el proyecto las capturas van al lado del programa",
                capturas.getName().equals("capturas"));
    }

    private static void tecladoDelOrdenador(Comprobador c) {
        Piano piano = new PianoSencillo(24, 108);
        piano.setPosicion(0, 0);
        // Sin abrir el sintetizador a proposito: la parte visual debe funcionar
        // igual aunque no haya sonido
        SintetizadorTeclado s = new SintetizadorTeclado();
        s.conectar(piano);

        int[] fila = {KeyEvent.VK_Z, KeyEvent.VK_S, KeyEvent.VK_X, KeyEvent.VK_D,
                KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_G, KeyEvent.VK_B,
                KeyEvent.VK_H, KeyEvent.VK_N, KeyEvent.VK_J, KeyEvent.VK_M};
        boolean cromatica = true;
        for (int i = 0; i < fila.length; i++) {
            s.pulsarTecla(fila[i]);
            cromatica = cromatica && piano.getTecla(0, 60 + i).estaPulsado();
            s.soltarTecla(fila[i]);
        }
        c.comprobar("la fila de abajo da la escala cromatica desde el Do central",
                cromatica);

        s.pulsarTecla(KeyEvent.VK_Z);
        // El sistema repite el evento al mantener la tecla pulsada
        s.pulsarTecla(KeyEvent.VK_Z);
        s.soltarTecla(KeyEvent.VK_Z);
        c.comprobar("una sola suelta apaga, aunque se repita el pulsado",
                !piano.getTecla(0, 60).estaPulsado());

        s.pulsarTecla(KeyEvent.VK_RIGHT);
        s.pulsarTecla(KeyEvent.VK_Z);
        c.comprobar("la flecha derecha sube una octava",
                s.getOctava() == 1 && piano.getTecla(0, 72).estaPulsado());
        s.soltarTodo();

        for (int i = 0; i < 12; i++) {
            s.pulsarTecla(KeyEvent.VK_RIGHT);
        }
        boolean topeArriba = s.getOctava() <= 3;
        for (int i = 0; i < 24; i++) {
            s.pulsarTecla(KeyEvent.VK_LEFT);
        }
        c.comprobar("la octava tiene tope por los dos lados",
                topeArriba && s.getOctava() >= -3);
        c.comprobar("una tecla sin asignar se ignora",
                !s.pulsarTecla(KeyEvent.VK_F12));
    }
}
