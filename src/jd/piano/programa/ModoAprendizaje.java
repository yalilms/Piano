package jd.piano.programa;

import jd.piano.teclas.Estilo;
import jd.piano.instrumentos.InstrumentoVisual;
import jd.piano.instrumentos.PiezaPulsable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Modo aprendizaje: la cancion se para y espera a que toques las notas que
 * vienen, como en Synthesia.
 *
 * Solo se vigila un canal. Exigir todos a la vez seria imposible de tocar: en
 * estas canciones hay hasta trece sonando al mismo tiempo.
 *
 * Las notas que caen a la vez se agrupan como un acorde, y vale tocarlas una
 * detras de otra: se recuerda cual se ha acertado ya, que exigirlas
 * simultaneas seria pasarse de exigente.
 */
public class ModoAprendizaje implements CapaSuperpuesta {

    /** Notas que empiezan mas juntas que esto cuentan como un acorde. */
    private static final long JUNTAS_US = 60_000;

    /** Cuanto antes del golpe se para la cancion. */
    private static final long ADELANTO_US = 120_000;

    /** Margen que se deja al saltar la intro, para no caer justo encima. */
    private static final long PREPARACION_US = 1_000_000;

    private static final Color ESPERADA = new Color(255, 214, 92);
    private static final Color ACERTADA = new Color(126, 220, 132);

    private final Reproductor reproductor;
    private final List<Grupo> grupos;
    private final int canal;

    private boolean activo;
    private boolean esperando;
    private int indice;
    private final Set<Integer> acertadas = new HashSet<>();

    public ModoAprendizaje(Partitura partitura, Reproductor reproductor, int canal) {
        this.reproductor = reproductor;
        this.canal = canal;
        this.grupos = agrupar(partitura, canal);
        this.activo = false;
        this.esperando = false;
        this.indice = 0;
    }

    /** Junta en un grupo las notas del canal que empiezan casi a la vez. */
    private static List<Grupo> agrupar(Partitura partitura, int canal) {
        List<NotaMidi> delCanal = new ArrayList<>();
        for (NotaMidi n : partitura.getNotas()) {
            if (n.canal() == canal) {
                delCanal.add(n);
            }
        }
        delCanal.sort(Comparator.comparingLong(NotaMidi::inicioUs));

        List<Grupo> grupos = new ArrayList<>();
        int i = 0;
        while (i < delCanal.size()) {
            long inicio = delCanal.get(i).inicioUs();
            List<Integer> notas = new ArrayList<>();
            while (i < delCanal.size()
                    && delCanal.get(i).inicioUs() - inicio <= JUNTAS_US) {
                if (!notas.contains(delCanal.get(i).nota())) {
                    notas.add(delCanal.get(i).nota());
                }
                i++;
            }
            grupos.add(new Grupo(inicio, notas));
        }
        return grupos;
    }

    /**
     * El canal que mas se parece a la melodia: el de notas mas agudas entre los
     * que tienen peso suficiente. Es lo que uno querria practicar.
     */
    public static int canalDeMelodia(Partitura partitura) {
        int[] cuantas = new int[16];
        long[] suma = new long[16];
        for (NotaMidi n : partitura.getNotas()) {
            cuantas[n.canal()]++;
            suma[n.canal()] += n.nota();
        }
        int total = partitura.getNotas().size();
        int mejor = -1;
        double masAgudo = -1;
        for (int c = 0; c < 16; c++) {
            // Se descartan los canales anecdoticos: un adorno de dos notas no
            // es la melodia aunque sea el mas agudo
            if (cuantas[c] >= Math.max(8, total / 20)) {
                double media = (double) suma[c] / cuantas[c];
                if (media > masAgudo) {
                    masAgudo = media;
                    mejor = c;
                }
            }
        }
        if (mejor < 0) {
            for (int c = 0; c < 16 && mejor < 0; c++) {
                if (cuantas[c] > 0) {
                    mejor = c;
                }
            }
        }
        return Math.max(0, mejor);
    }

    public int getCanal() {
        return this.canal;
    }

    public int getCuantosGrupos() {
        return this.grupos.size();
    }

    public boolean estaEsperando() {
        return this.esperando;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
        if (!activo) {
            this.esperando = false;
        }
        this.acertadas.clear();
        situarse();
        if (activo) {
            saltarLaIntro();
        }
    }

    /**
     * Si el canal que se practica no entra hasta mucho despues, se adelanta
     * hasta justo antes de su primera nota. En A_Dios_le_pido la melodia no
     * empieza hasta el segundo 22, y esperarlos parados no tiene sentido.
     *
     * Solo salta hacia delante: si se activa a mitad de cancion no retrocede.
     */
    private void saltarLaIntro() {
        if (this.indice >= this.grupos.size()) {
            return;
        }
        long destino = this.grupos.get(this.indice).inicioUs - PREPARACION_US;
        if (destino > this.reproductor.getPosicionUs() + PREPARACION_US) {
            this.reproductor.buscar(destino);
        }
    }

    public boolean isActivo() {
        return this.activo;
    }

    /** Coloca el indice en el primer grupo que aun no ha pasado. */
    private void situarse() {
        long posicion = this.reproductor.getPosicionUs();
        this.indice = 0;
        while (this.indice < this.grupos.size()
                && this.grupos.get(this.indice).inicioUs < posicion) {
            this.indice++;
        }
    }

    @Override
    public void dibujar(Graphics2D g, InstrumentoVisual instrumento) {
        if (!this.activo || instrumento == null || this.grupos.isEmpty()) {
            return;
        }
        actualizar(instrumento);
        if (this.esperando) {
            pintarEsperadas(g, instrumento);
        }
    }

    /**
     * Se lleva desde el dibujado porque hay que mirarlo en cada frame: mientras
     * la cancion esta parada nadie mas avanza el estado.
     */
    private void actualizar(InstrumentoVisual instrumento) {
        long posicion = this.reproductor.getPosicionUs();

        // Si han saltado a otro punto, hay que recolocarse
        if (this.indice < this.grupos.size()
                && this.grupos.get(this.indice).inicioUs + 2_000_000 < posicion) {
            situarse();
            this.acertadas.clear();
            this.esperando = false;
        }
        if (this.indice >= this.grupos.size()) {
            this.esperando = false;
            return;
        }

        Grupo grupo = this.grupos.get(this.indice);
        if (!this.esperando && posicion >= grupo.inicioUs - ADELANTO_US) {
            this.esperando = true;
            this.acertadas.clear();
            this.reproductor.pausar();
        }

        if (this.esperando) {
            // Con la cancion parada, cualquier tecla pulsada la ha pulsado el
            // usuario: el reproductor no manda nada
            for (int nota : grupo.notas) {
                PiezaPulsable pieza = buscar(instrumento, nota);
                if (pieza != null && pieza.estaPulsado()) {
                    this.acertadas.add(nota);
                }
            }
            if (this.acertadas.size() >= grupo.notas.size()) {
                this.esperando = false;
                this.indice++;
                this.acertadas.clear();
                this.reproductor.reproducir();
            }
        }
    }

    private PiezaPulsable buscar(InstrumentoVisual instrumento, int nota) {
        PiezaPulsable pieza = instrumento.piezaDe(this.canal, nota);
        if (pieza == null) {
            pieza = instrumento.piezaDe(SintetizadorTeclado.CANAL, nota);
        }
        return pieza;
    }

    private void pintarEsperadas(Graphics2D g, InstrumentoVisual instrumento) {
        Grupo grupo = this.grupos.get(this.indice);
        List<String> nombres = new ArrayList<>();
        Rectangle primera = null;

        for (int nota : grupo.notas) {
            PiezaPulsable pieza = buscar(instrumento, nota);
            if (pieza == null) {
                continue;
            }
            boolean hecha = this.acertadas.contains(nota);
            Color color = hecha ? ACERTADA : ESPERADA;
            Shape forma = pieza.getForma();
            Rectangle caja = forma.getBounds();
            if (primera == null || caja.x < primera.x) {
                primera = caja;
            }
            nombres.add(Estilo.etiquetaDe(nota));

            g.setStroke(new BasicStroke(2.5f));
            g.setColor(Estilo.conAlfa(color, hecha ? 200 : 255));
            g.draw(forma);
            if (!hecha) {
                g.setColor(Estilo.conAlfa(color, 70));
                g.fill(forma);
                dibujarFlecha(g, caja, color);
            }
        }

        if (primera != null) {
            dibujarCartel(g, instrumento, nombres);
        }
    }

    /** Una punta de flecha sobre la tecla, senalando donde hay que pulsar. */
    private static void dibujarFlecha(Graphics2D g, Rectangle caja, Color color) {
        int ancho = Math.max(8, caja.width - 4);
        int alto = ancho / 2 + 2;
        int x = caja.x + (caja.width - ancho) / 2;
        int y = caja.y - alto - 6;
        int[] xs = {x, x + ancho, x + ancho / 2};
        int[] ys = {y, y, y + alto};
        g.setColor(Estilo.conAlfa(color, 235));
        g.fillPolygon(xs, ys, 3);
    }

    private void dibujarCartel(Graphics2D g, InstrumentoVisual instrumento,
                               List<String> nombres) {
        Rectangle limites = instrumento.getLimites();
        if (limites == null) {
            return;
        }
        String texto = "Toca " + String.join(" + ", nombres);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        FontMetrics metricas = g.getFontMetrics();
        int ancho = metricas.stringWidth(texto) + 24;
        int alto = metricas.getHeight() + 10;
        int x = limites.x + (limites.width - ancho) / 2;
        int y = limites.y - alto - 34;

        g.setColor(new Color(24, 18, 10, 225));
        g.fill(new RoundRectangle2D.Double(x, y, ancho, alto, 10, 10));
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(Estilo.conAlfa(ESPERADA, 200));
        g.draw(new RoundRectangle2D.Double(x, y, ancho, alto, 10, 10));
        g.setColor(ESPERADA);
        g.drawString(texto, x + 12, y + metricas.getAscent() + 5);
    }

    /** Un acorde: las notas que hay que tocar juntas y cuando toca. */
    private record Grupo(long inicioUs, List<Integer> notas) {
    }
}
