package jd.piano.pruebas;

import jd.piano.programa.Escena;
import jd.piano.programa.ModoDeUso;
import jd.piano.programa.PianoSencillo;
import jd.piano.programa.RastroLibre;
import jd.piano.programa.SintetizadorTeclado;
import jd.piano.teclas.Estilo;
import jd.piano.teclas.Piano;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/** Los modos de uso y el modo de tocar libre. */
public final class PruebaModos {

    private PruebaModos() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("modos de uso");
        modos(c);
        rastro(c);
        sostenido(c);
        return c;
    }

    private static void modos(Comprobador c) {
        c.comprobar("hay cuatro modos de uso", ModoDeUso.values().length == 4);

        boolean conNombre = true;
        for (ModoDeUso m : ModoDeUso.values()) {
            conNombre = conNombre && m.getNombre() != null && !m.getNombre().isBlank()
                    && m.getExplicacion() != null && !m.getExplicacion().isBlank();
        }
        c.comprobar("todos tienen nombre y explicacion", conNombre);

        c.comprobar("solo el modo libre no necesita cancion",
                !ModoDeUso.TOCAR_LIBRE.necesitaCancion()
                        && ModoDeUso.VER_CANCION.necesitaCancion()
                        && ModoDeUso.VER_CANALES.necesitaCancion()
                        && ModoDeUso.APRENDER.necesitaCancion());

        c.comprobar("solo ver por canales dibuja un teclado por canal",
                ModoDeUso.VER_CANALES.esPorCanales()
                        && !ModoDeUso.VER_CANCION.esPorCanales()
                        && !ModoDeUso.APRENDER.esPorCanales()
                        && !ModoDeUso.TOCAR_LIBRE.esPorCanales());

        // La cascada necesita un solo teclado y una cancion: ni el modo por
        // canales ni el libre la pueden tener
        c.comprobar("la cascada solo esta donde cabe",
                ModoDeUso.VER_CANCION.tieneCascada()
                        && ModoDeUso.APRENDER.tieneCascada()
                        && !ModoDeUso.VER_CANALES.tieneCascada()
                        && !ModoDeUso.TOCAR_LIBRE.tieneCascada());

        boolean coherente = true;
        for (ModoDeUso m : ModoDeUso.values()) {
            // No puede haber cascada sin cancion, ni con varios teclados
            if (m.tieneCascada() && (!m.necesitaCancion() || m.esPorCanales())) {
                coherente = false;
            }
        }
        c.comprobar("no hay combinaciones imposibles", coherente);
    }

    /** El rastro del modo libre: barras que crecen y suben. */
    private static void rastro(Comprobador c) {
        Piano piano = new PianoSencillo(24, 108);
        piano.setPosicion(40, 300);
        SintetizadorTeclado s = new SintetizadorTeclado();
        s.conectar(piano);

        Escena escena = new Escena(600, 500);
        escena.setPiano(piano);
        RastroLibre rastro = new RastroLibre();
        rastro.setYTope(20);
        escena.anadirSuperpuesta(rastro);
        escena.prepararCaches();

        Graphics2D g = new BufferedImage(600, 500, BufferedImage.TYPE_INT_RGB)
                .createGraphics();
        rastro.dibujar(g, piano);
        BufferedImage vacio = escena.aImagen();

        s.encender(60);
        try {
            Thread.sleep(140);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
        BufferedImage conNota = escena.aImagen();

        long distintos = 0;
        for (int y = 0; y < 500; y++) {
            for (int x = 0; x < 600; x++) {
                if (vacio.getRGB(x, y) != conNota.getRGB(x, y)) {
                    distintos++;
                }
            }
        }
        c.dato("pixeles que anade el rastro: " + distintos);
        c.comprobar("al tocar aparece el rastro sobre el teclado", distintos > 500);

        s.apagar(60);
        rastro.dibujar(g, piano);
        c.comprobar("soltar la nota no revienta el rastro", true);

        // Un piano sin colocar no debe hacerlo caer
        rastro.dibujar(g, null);
        c.comprobar("sin piano no hace nada", true);
        g.dispose();
    }

    private static void sostenido(Comprobador c) {
        SintetizadorTeclado s = new SintetizadorTeclado();
        c.comprobar("empieza sin pedal", !s.isSostenido());
        s.setSostenido(true);
        c.comprobar("se puede pisar el pedal", s.isSostenido());
        s.setSostenido(false);
        c.comprobar("y levantarlo", !s.isSostenido());

        // Sin sintetizador abierto tampoco debe fallar
        s.pulsarTecla(KeyEvent.VK_Z);
        s.soltarTecla(KeyEvent.VK_Z);
        c.comprobar("el pedal no depende de que haya sonido", true);

        c.comprobar("lo que toca el usuario tiene color propio",
                !Estilo.COLOR_USUARIO.equals(Estilo.colorDeCanal(
                        SintetizadorTeclado.CANAL)));
    }
}
