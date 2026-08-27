package jd.piano.pruebas;

import jd.piano.instrumentos.CirculoCromatico;
import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.PiezaPulsable;
import jd.piano.instrumentos.Xilofono;
import jd.piano.programa.AnalisisMidi;
import jd.piano.programa.Conjunto;
import jd.piano.programa.ModoDeUso;
import jd.piano.programa.NotaMidi;
import jd.piano.programa.Partitura;
import jd.piano.programa.TipoInstrumento;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * El xilofono, el circulo cromatico y el conjunto de varios instrumentos.
 *
 * Los tres tienen su propio riesgo. El xilofono, que las barras se pisen o que
 * dejen de estrecharse. El circulo, la cuenta de cuantas notas de la misma
 * clase estan sonando: sin ella, soltar un Do apagaria el nodo aunque siguiera
 * sonando el de otra octava. Y el conjunto, que una nota se pierda por caer en
 * un canal que no esta repartido.
 */
public final class PruebaConjunto {

    /** Programas de General MIDI de ejemplo, uno por familia que nos importa. */
    private static final int PIANO_DE_COLA = 0;
    private static final int MARIMBA = 12;
    private static final int GUITARRA_LIMPIA = 27;
    private static final int BAJO_ELECTRICO = 33;

    private PruebaConjunto() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("xilofono, circulo y conjunto");
        xilofono(c);
        circulo(c);
        reparto(c);
        pilaDeInstrumentos(c);
        canciones(c);
        return c;
    }

    // ---------- xilofono ----------

    private static void xilofono(Comprobador c) {
        Xilofono x = new Xilofono();
        x.ajustarA(1000, 200);
        x.setPosicion(20, 20);
        c.dato("xilofono: " + x.getAnchura() + " x " + x.getAltura() + " px, "
                + (Xilofono.NOTA_MAXIMA - Xilofono.NOTA_MINIMA + 1) + " barras");

        c.comprobar("acepta los extremos de su rango y nada mas alla",
                x.piezaDe(0, Xilofono.NOTA_MINIMA) != null
                        && x.piezaDe(0, Xilofono.NOTA_MAXIMA) != null
                        && x.piezaDe(0, Xilofono.NOTA_MINIMA - 1) == null
                        && x.piezaDe(0, Xilofono.NOTA_MAXIMA + 1) == null);

        // Cada nota su barra: ni dos notas en la misma ni una nota sin sitio
        Set<PiezaPulsable> vistas = new HashSet<>();
        boolean unaPorNota = true;
        for (int nota = Xilofono.NOTA_MINIMA; nota <= Xilofono.NOTA_MAXIMA; nota++) {
            PiezaPulsable barra = x.piezaDe(0, nota);
            unaPorNota = unaPorNota && barra != null && vistas.add(barra);
        }
        c.comprobar("cada nota tiene su propia barra", unaPorNota);

        // Las barras de la misma fila no se pisan
        boolean solapan = false;
        for (int a = Xilofono.NOTA_MINIMA; a <= Xilofono.NOTA_MAXIMA && !solapan; a++) {
            for (int b = a + 1; b <= Xilofono.NOTA_MAXIMA && !solapan; b++) {
                Rectangle ca = x.donde(0, a);
                Rectangle cb = x.donde(0, b);
                solapan = ca.intersects(cb);
            }
        }
        c.comprobar("ninguna barra pisa a otra", !solapan);

        // La fila de las alteradas va por encima de la de las naturales
        c.comprobar("las alteradas estan detras, mas arriba que las naturales",
                x.donde(0, 61).y + x.donde(0, 61).height <= x.donde(0, 60).y);

        // Y el largo baja al subir de nota, que es lo que le da la forma de cuna
        boolean estrecha = true;
        int anterior = Integer.MAX_VALUE;
        for (int nota = Xilofono.NOTA_MINIMA; nota <= Xilofono.NOTA_MAXIMA; nota++) {
            if (nota % 12 == 1 || nota % 12 == 3 || nota % 12 == 6
                    || nota % 12 == 8 || nota % 12 == 10) {
                continue;
            }
            int alto = x.donde(0, nota).height;
            estrecha = estrecha && alto <= anterior;
            anterior = alto;
        }
        c.dato("barra mas grave " + x.donde(0, Xilofono.NOTA_MINIMA).height
                + " px, mas aguda " + x.donde(0, Xilofono.NOTA_MAXIMA).height + " px");
        c.comprobar("las barras se acortan conforme sube la nota", estrecha);

        golpeVisible(c, x, 60, "xilofono");
    }

    // ---------- circulo cromatico ----------

    private static void circulo(Comprobador c) {
        CirculoCromatico r = new CirculoCromatico();
        r.ajustarA(400, 400);
        r.setPosicion(10, 10);
        c.dato("circulo: " + r.getAnchura() + " x " + r.getAltura() + " px");

        // Todas las octavas del mismo Do van al mismo nodo
        boolean mismoNodo = true;
        for (int octava = 0; octava < 10; octava++) {
            mismoNodo = mismoNodo
                    && r.piezaDe(0, 12 * octava) == r.piezaDe(0, 0);
        }
        c.comprobar("todas las octavas del Do caen en el mismo nodo", mismoNodo);

        Set<PiezaPulsable> nodos = new HashSet<>();
        for (int nota = 0; nota < 128; nota++) {
            nodos.add(r.piezaDe(0, nota));
        }
        c.comprobar("hay exactamente doce nodos", nodos.size() == 12);

        // Los doce tienen que caber dentro de la caja, etiquetas incluidas
        boolean dentro = true;
        for (int nota = 60; nota < 72; nota++) {
            dentro = dentro && r.getLimites().contains(r.donde(0, nota));
        }
        c.comprobar("los doce nodos caben en la caja del circulo", dentro);

        // La cuenta por clase, que es el fallo facil de este instrumento
        r.pulsar(0, 60, 100);
        r.pulsar(1, 72, 100);
        r.soltar(0, 60);
        c.comprobar("suelto un Do y el nodo sigue encendido por el de otra octava",
                r.piezaDe(0, 60).estaPulsado());
        r.soltar(1, 72);
        c.comprobar("al soltar el ultimo Do el nodo se apaga",
                !r.piezaDe(0, 60).estaPulsado());

        // Silenciar un canal no debe apagar lo que toca otro
        r.pulsar(2, 64, 100);
        r.pulsar(3, 76, 100);
        r.soltarCanal(2);
        c.comprobar("soltar un canal no apaga la misma nota de otro canal",
                r.piezaDe(0, 64).estaPulsado());
        r.soltarTodas();
        c.comprobar("soltarTodas apaga los doce",
                !r.piezaDe(0, 64).estaPulsado() && !r.piezaDe(0, 60).estaPulsado());

        // Volver a pulsar despues de soltarTodas tiene que funcionar: si la
        // cuenta se quedara descolgada, el nodo no se apagaria nunca mas
        r.pulsar(0, 67, 100);
        r.soltar(0, 67);
        c.comprobar("la cuenta no se descuelga tras soltarTodas",
                !r.piezaDe(0, 67).estaPulsado());

        c.comprobar("el circulo no admite notas cayendo", !r.admiteCascada());
        c.comprobar("representa el 100% de cualquier cancion",
                TipoInstrumento.CIRCULO.getNotaMinima() == 0
                        && TipoInstrumento.CIRCULO.getNotaMaxima() == 127);

        // Con dos notas sonando, la region a repintar es la rueda entera,
        // porque las lineas de intervalo van de un nodo a otro
        r.pulsar(0, 60, 100);
        r.pulsar(0, 67, 100);
        List<Rectangle> regiones = new ArrayList<>();
        r.anadirRegionesActivas(regiones);
        boolean cubreTodo = regiones.size() == 1
                && regiones.get(0).contains(r.getLimites());
        c.comprobar("con notas sonando se repinta la rueda entera", cubreTodo);

        golpeVisible(c, r, 60, "circulo");
    }

    /** Que la nota se vea de verdad: se compara el reposo con la pulsacion. */
    private static void golpeVisible(Comprobador c, InstrumentoVisual i,
                                     int nota, String nombre) {
        Rectangle limites = i.getLimites();
        int ancho = limites.x + limites.width + 20;
        int alto = limites.y + limites.height + 20;
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        i.soltarTodas();
        i.dibujarEnReposo(g);
        BufferedImage reposo = copiar(img);

        i.pulsar(2, nota, 120);
        i.dibujar(g);
        i.dibujarBrillos(g);
        g.dispose();

        long distintos = 0;
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                if (reposo.getRGB(x, y) != img.getRGB(x, y)) {
                    distintos++;
                }
            }
        }
        c.dato("pixeles que cambia una nota en el " + nombre + ": " + distintos);
        c.comprobar("una nota pulsada se ve en el " + nombre, distintos > 200);
        i.soltarTodas();
    }

    // ---------- reparto de canales ----------

    private static void reparto(Comprobador c) {
        // Sin rango declarado (-1) manda la familia sin mas
        c.comprobar("una pista de guitarra va al mastil",
                Conjunto.tipoPara(GUITARRA_LIMPIA, -1, -1) == TipoInstrumento.GUITARRA);
        c.comprobar("una pista de bajo va al bajo",
                Conjunto.tipoPara(BAJO_ELECTRICO, -1, -1) == TipoInstrumento.BAJO);
        c.comprobar("una marimba va al xilofono",
                Conjunto.tipoPara(MARIMBA, -1, -1) == TipoInstrumento.XILOFONO);
        c.comprobar("un piano va al teclado",
                Conjunto.tipoPara(PIANO_DE_COLA, -1, -1) == TipoInstrumento.TECLADO);

        // Y el rango tiene la ultima palabra: lo que no cabe, al teclado
        c.comprobar("una guitarra que baja del Mi grave se ve en un teclado",
                Conjunto.tipoPara(GUITARRA_LIMPIA, 28, 64) == TipoInstrumento.TECLADO);
        c.comprobar("una guitarra dentro de su rango se queda en el mastil",
                Conjunto.tipoPara(GUITARRA_LIMPIA, 40, 79) == TipoInstrumento.GUITARRA);
        c.comprobar("un bajo que sube demasiado se ve en un teclado",
                Conjunto.tipoPara(BAJO_ELECTRICO, 28, 96) == TipoInstrumento.TECLADO);
    }

    // ---------- la pila de instrumentos ----------

    private static void pilaDeInstrumentos(Comprobador c) {
        Conjunto conjunto = new Conjunto();
        conjunto.asignar(0, TipoInstrumento.TECLADO);
        conjunto.asignar(1, TipoInstrumento.GUITARRA);
        conjunto.asignar(2, TipoInstrumento.GUITARRA);
        conjunto.asignar(3, TipoInstrumento.BAJO);
        conjunto.ajustarA(1100, 600);
        conjunto.setPosicion(30, 30);

        c.dato("conjunto de prueba: " + conjunto.getNombre() + ", "
                + conjunto.getAnchura() + " x " + conjunto.getAltura() + " px");
        c.comprobar("cuatro canales de tres tipos dan tres instrumentos",
                conjunto.getCuantosInstrumentos() == 3);
        c.comprobar("los dos canales de guitarra comparten mastil",
                conjunto.piezaDe(1, 52) == conjunto.piezaDe(2, 52));
        c.comprobar("cada tipo tiene sus notas donde le toca",
                conjunto.piezaDe(0, 24) != null
                        && conjunto.piezaDe(3, 28) != null);

        // Los paneles no se pisan, y todos caben dentro del conjunto
        List<Rectangle> paneles = new ArrayList<>();
        for (int canal : new int[]{0, 1, 3}) {
            Rectangle union = null;
            for (int nota = 0; nota < 128; nota++) {
                Rectangle donde = conjunto.donde(canal, nota);
                if (donde != null) {
                    union = union == null ? new Rectangle(donde) : union.union(donde);
                }
            }
            paneles.add(union);
        }
        boolean sePisan = false;
        boolean caben = true;
        for (int i = 0; i < paneles.size(); i++) {
            caben = caben && conjunto.getLimites().contains(paneles.get(i));
            for (int j = i + 1; j < paneles.size(); j++) {
                sePisan = sePisan || paneles.get(i).intersects(paneles.get(j));
            }
        }
        c.comprobar("los instrumentos apilados no se pisan", !sePisan);
        c.comprobar("todos caben dentro del conjunto", caben);

        // Mover un canal reaprovecha el instrumento que ya estaba, para no
        // apagar lo que suena en los otros canales del mismo tipo
        PiezaPulsable antes = conjunto.piezaDe(1, 52);
        conjunto.asignar(2, TipoInstrumento.TECLADO);
        c.comprobar("mover un canal no rehace el instrumento de los demas",
                conjunto.piezaDe(1, 52) == antes);
        c.comprobar("el canal movido ya se ve en el teclado",
                conjunto.getTipoDe(2) == TipoInstrumento.TECLADO
                        && conjunto.piezaDe(2, 24) != null);
        c.comprobar("sigue habiendo tres instrumentos",
                conjunto.getCuantosInstrumentos() == 3);

        conjunto.asignar(1, TipoInstrumento.TECLADO);
        c.comprobar("al quedarse sin canales, el mastil desaparece",
                conjunto.getCuantosInstrumentos() == 2);

        c.comprobar("el conjunto no admite notas cayendo",
                !conjunto.admiteCascada()
                        && !ModoDeUso.CONJUNTO.tieneCascada());

        // Un canal que no estaba repartido pero suena igual: la nota no se pierde
        c.comprobar("una nota de un canal sin repartir no se pierde",
                conjunto.pulsar(11, 60, 100));
    }

    // ---------- las canciones de verdad ----------

    private static void canciones(Comprobador c) {
        File[] ficheros = PruebaMidi.canciones();
        if (ficheros.length == 0) {
            c.comprobar("hay canciones para probar el reparto", false);
            return;
        }
        double peor = 100;
        String laPeor = "";
        int masInstrumentos = 0;
        boolean sinExcepciones = true;

        for (File f : ficheros) {
            try {
                AnalisisMidi analisis = AnalisisMidi.de(f.getPath());
                Conjunto conjunto = Conjunto.para(analisis);
                conjunto.ajustarA(1100, 600);
                conjunto.setPosicion(30, 30);
                double cobertura = conjunto.coberturaDe(Partitura.de(f.getPath()));
                if (cobertura < peor) {
                    peor = cobertura;
                    laPeor = f.getName();
                }
                masInstrumentos = Math.max(masInstrumentos,
                        conjunto.getCuantosInstrumentos());

                // El reparto no puede perder nada que un teclado solo si
                // mostraria: si una pista de guitarra se sale del mastil, hay
                // que haberla mandado al teclado
                for (NotaMidi nota : Partitura.de(f.getPath()).getNotas()) {
                    boolean cabeEnTeclado =
                            nota.nota() >= TipoInstrumento.TECLADO.getNotaMinima()
                            && nota.nota() <= TipoInstrumento.TECLADO.getNotaMaxima();
                    if (cabeEnTeclado) {
                        sinExcepciones = sinExcepciones
                                && conjunto.donde(nota.canal(), nota.nota()) != null;
                    }
                }
            } catch (Exception error) {
                sinExcepciones = false;
            }
        }
        c.dato(String.format("peor cobertura del reparto: %.1f%% (%s)", peor, laPeor));
        c.dato("mas instrumentos a la vez en una cancion: " + masInstrumentos);
        c.comprobar("el reparto ve mas del 99% de las notas de cualquier cancion",
                peor > 99.0);
        c.comprobar("el reparto no pierde nada que un teclado si mostraria",
                sinExcepciones);
        c.comprobar("alguna cancion usa mas de un instrumento", masInstrumentos > 1);
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
