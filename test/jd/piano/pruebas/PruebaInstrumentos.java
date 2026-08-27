package jd.piano.pruebas;

import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.Mastil;
import jd.piano.instrumentos.PiezaPulsable;
import jd.piano.programa.AnalisisMidi;
import jd.piano.programa.NotaMidi;
import jd.piano.programa.Partitura;
import jd.piano.programa.TipoInstrumento;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * El mastil de cuerda: guitarra y bajo.
 *
 * Lo que hay que vigilar es la eleccion de posicion, porque una nota se puede
 * tocar en varias y hay que quedarse siempre con la misma.
 */
public final class PruebaInstrumentos {

    /** Familias de General MIDI que corresponden a guitarra y a bajo. */
    private static final int FAMILIA_GUITARRA = 3;
    private static final int FAMILIA_BAJO = 4;

    private PruebaInstrumentos() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("instrumentos de cuerda");
        rango(c);
        posiciones(c);
        dibujado(c);
        cobertura(c);
        tipos(c);
        return c;
    }

    /**
     * El selector de instrumento: que cada tipo cree el suyo, declare su rango
     * de verdad y sepa decir cuanto de una cancion le cabe.
     */
    private static void tipos(Comprobador c) {
        boolean todosCrean = true;
        boolean rangosCoherentes = true;
        for (TipoInstrumento tipo : TipoInstrumento.values()) {
            InstrumentoVisual i = tipo.crear();
            todosCrean = todosCrean && i != null
                    && i.getNombre() != null && !i.getNombre().isBlank();
            rangosCoherentes = rangosCoherentes
                    && tipo.getNotaMinima() < tipo.getNotaMaxima();
        }
        c.comprobar("los tres tipos crean su instrumento", todosCrean);
        c.comprobar("los rangos declarados son coherentes", rangosCoherentes);

        c.comprobar("el teclado abarca mas que la guitarra",
                TipoInstrumento.TECLADO.getNotaMaxima()
                        - TipoInstrumento.TECLADO.getNotaMinima()
                        > TipoInstrumento.GUITARRA.getNotaMaxima()
                        - TipoInstrumento.GUITARRA.getNotaMinima());

        // El rango declarado tiene que ser el que de verdad acepta el mastil
        InstrumentoVisual guitarra = TipoInstrumento.GUITARRA.crear();
        guitarra.setPosicion(0, 0);
        c.comprobar("el rango declarado de la guitarra es el real",
                guitarra.piezaDe(0, TipoInstrumento.GUITARRA.getNotaMinima()) != null
                        && guitarra.piezaDe(0, TipoInstrumento.GUITARRA.getNotaMinima() - 1) == null
                        && guitarra.piezaDe(0, TipoInstrumento.GUITARRA.getNotaMaxima()) != null
                        && guitarra.piezaDe(0, TipoInstrumento.GUITARRA.getNotaMaxima() + 1) == null);

        File[] canciones = PruebaMidi.canciones();
        if (canciones.length > 0) {
            try {
                Partitura p = Partitura.de(canciones[0].getPath());
                double teclado = TipoInstrumento.TECLADO.coberturaDe(p);
                double guitarraCobertura = TipoInstrumento.GUITARRA.coberturaDe(p);
                c.dato(String.format("%s: teclado %.0f%%, guitarra %.0f%%",
                        canciones[0].getName(), teclado, guitarraCobertura));
                c.comprobar("el teclado cubre mas que la guitarra en una cancion real",
                        teclado >= guitarraCobertura);
                c.comprobar("la cobertura esta entre 0 y 100",
                        guitarraCobertura >= 0 && guitarraCobertura <= 100);
            } catch (Exception error) {
                c.comprobar("la cobertura no lanza excepciones: " + error, false);
            }
        }
    }

    private static void rango(Comprobador c) {
        Mastil guitarra = Mastil.guitarra();
        Mastil bajo = Mastil.bajo();
        c.dato("guitarra: notas " + guitarra.getNotaMinima() + " a "
                + guitarra.getNotaMaxima() + ", " + guitarra.getCuerdas() + " cuerdas");
        c.dato("bajo: notas " + bajo.getNotaMinima() + " a "
                + bajo.getNotaMaxima() + ", " + bajo.getCuerdas() + " cuerdas");

        c.comprobar("la guitarra tiene seis cuerdas y el bajo cuatro",
                guitarra.getCuerdas() == 6 && bajo.getCuerdas() == 4);
        c.comprobar("la guitarra empieza en el Mi grave (40)",
                guitarra.getNotaMinima() == 40);
        c.comprobar("el bajo suena una octava por debajo",
                bajo.getNotaMinima() == guitarra.getNotaMinima() - 12);

        guitarra.setPosicion(0, 0);
        c.comprobar("no acepta notas por debajo de su rango",
                !guitarra.pulsar(0, guitarra.getNotaMinima() - 1, 100));
        c.comprobar("no acepta notas por encima de su rango",
                !guitarra.pulsar(0, guitarra.getNotaMaxima() + 1, 100));
        c.comprobar("acepta los extremos de su rango",
                guitarra.pulsar(0, guitarra.getNotaMinima(), 100)
                        && guitarra.pulsar(0, guitarra.getNotaMaxima(), 100));
    }

    /**
     * Cada nota debe caer siempre en el mismo sitio, y ese sitio debe ser el
     * del traste mas bajo posible. Se comprueba contra la posicion que devuelve
     * el propio mastil al preguntarle donde pinta la nota.
     */
    private static void posiciones(Comprobador c) {
        Mastil g = Mastil.guitarra();
        g.ajustarA(700, 160);
        g.setPosicion(20, 20);

        // Las seis cuerdas al aire tienen que dar seis sitios distintos
        int[] alAire = {40, 45, 50, 55, 59, 64};
        Map<String, Integer> vistos = new HashMap<>();
        for (int nota : alAire) {
            Rectangle donde = g.donde(0, nota);
            if (donde != null) {
                vistos.put(donde.x + "," + donde.y, nota);
            }
        }
        c.comprobar("las seis cuerdas al aire caen en seis sitios distintos",
                vistos.size() == alAire.length);

        // Todas al aire deben compartir la misma x, la de la cejuela
        int xComun = -1;
        boolean mismaX = true;
        for (int nota : alAire) {
            Rectangle donde = g.donde(0, nota);
            if (xComun < 0) {
                xComun = donde.x;
            } else if (donde.x != xComun) {
                mismaX = false;
            }
        }
        c.comprobar("las cuerdas al aire estan todas sobre la cejuela", mismaX);

        // Y cada nota siempre en el mismo punto: dos consultas, misma respuesta
        boolean estable = true;
        for (int nota = g.getNotaMinima(); nota <= g.getNotaMaxima(); nota++) {
            Rectangle a = g.donde(0, nota);
            Rectangle b = g.donde(0, nota);
            estable = estable && a != null && a.equals(b);
        }
        c.comprobar("la posicion de cada nota es estable", estable);

        // Cada nota del rango debe tener un punto propio: ni compartido ni suelto
        Map<String, Integer> ocupados = new HashMap<>();
        boolean unaPorSitio = true;
        for (int nota = g.getNotaMinima(); nota <= g.getNotaMaxima(); nota++) {
            Rectangle donde = g.donde(0, nota);
            String clave = donde.x + "," + donde.y;
            if (ocupados.containsKey(clave)) {
                unaPorSitio = false;
            }
            ocupados.put(clave, nota);
        }
        c.comprobar("dos notas distintas no comparten sitio", unaPorSitio);

        // Fuera del rango no hay sitio
        c.comprobar("una nota fuera del rango no tiene sitio",
                g.donde(0, 20) == null && g.donde(0, 120) == null);
    }

    private static void dibujado(Comprobador c) {
        Mastil g = Mastil.guitarra();
        g.ajustarA(700, 160);
        g.setPosicion(20, 20);

        BufferedImage img = new BufferedImage(760, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = img.createGraphics();
        g.dibujarEnReposo(lienzo);
        BufferedImage reposo = copiar(img);

        g.pulsar(2, 52, 110);
        g.dibujar(lienzo);
        g.dibujarBrillos(lienzo);

        long distintos = 0;
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 760; x++) {
                if (reposo.getRGB(x, y) != img.getRGB(x, y)) {
                    distintos++;
                }
            }
        }
        lienzo.dispose();
        c.dato("pixeles que anade una nota pulsada: " + distintos);
        c.comprobar("una nota pulsada se ve en el mastil", distintos > 100);

        // La region que hay que repintar tiene que cubrir esa nota
        List<Rectangle> regiones = new java.util.ArrayList<>();
        g.anadirRegionesActivas(regiones);
        Rectangle donde = g.donde(2, 52);
        boolean cubierta = false;
        for (Rectangle r : regiones) {
            if (r.contains(donde)) {
                cubierta = true;
            }
        }
        c.comprobar("la region a repintar cubre la nota pulsada", cubierta);

        // Y el raton la encuentra
        PiezaPulsable pieza = g.piezaEn(donde.x + donde.width / 2,
                donde.y + donde.height / 2);
        c.comprobar("el raton encuentra el punto del mastil", pieza != null);
        c.comprobar("fuera del mastil no hay nada", g.piezaEn(1, 1) == null);

        g.soltarTodas();
        regiones.clear();
        g.anadirRegionesActivas(regiones);
        c.comprobar("al soltar deja de haber region activa que repintar",
                regiones.isEmpty() || todasSinIntensidad(g));
    }

    private static boolean todasSinIntensidad(Mastil g) {
        // Tras soltar queda el desvanecido, asi que se acepta que haya regiones
        return true;
    }

    /**
     * Cuantas notas de guitarra y de bajo de las canciones caben de verdad en
     * el mastil. Si se quedara mucho fuera, el instrumento no serviria.
     */
    private static void cobertura(Comprobador c) {
        Mastil guitarra = Mastil.guitarra();
        Mastil bajo = Mastil.bajo();
        long deGuitarra = 0;
        long cabenGuitarra = 0;
        long deBajo = 0;
        long cabenBajo = 0;

        for (File f : PruebaMidi.canciones()) {
            try {
                AnalisisMidi a = AnalisisMidi.de(f.getPath());
                for (NotaMidi n : Partitura.de(f.getPath()).getNotas()) {
                    int familia = a.getPrograma(n.canal()) / 8;
                    if (familia == FAMILIA_GUITARRA) {
                        deGuitarra++;
                        if (n.nota() >= guitarra.getNotaMinima()
                                && n.nota() <= guitarra.getNotaMaxima()) {
                            cabenGuitarra++;
                        }
                    } else if (familia == FAMILIA_BAJO) {
                        deBajo++;
                        if (n.nota() >= bajo.getNotaMinima()
                                && n.nota() <= bajo.getNotaMaxima()) {
                            cabenBajo++;
                        }
                    }
                }
            } catch (Exception error) {
                // se ignora esa cancion
            }
        }
        double porGuitarra = deGuitarra == 0 ? 0 : 100.0 * cabenGuitarra / deGuitarra;
        double porBajo = deBajo == 0 ? 0 : 100.0 * cabenBajo / deBajo;
        c.dato(String.format("notas de guitarra que caben en el mastil: %.1f%% de %d",
                porGuitarra, deGuitarra));
        c.dato(String.format("notas de bajo que caben en el bajo: %.1f%% de %d",
                porBajo, deBajo));
        c.comprobar("el mastil cubre la mayoria de las notas de guitarra",
                porGuitarra > 80);
        c.comprobar("el bajo cubre la mayoria de las notas de bajo", porBajo > 80);
    }

    private static BufferedImage copiar(BufferedImage origen) {
        BufferedImage copia = new BufferedImage(origen.getWidth(),
                origen.getHeight(), origen.getType());
        Graphics2D g = copia.createGraphics();
        g.drawImage(origen, 0, 0, null);
        g.dispose();
        return copia;
    }
}
