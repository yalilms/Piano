package jd.piano.programa;

import jd.piano.teclas.Estilo;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * La pantalla que sale al abrir el programa: se elige que se quiere hacer y no
 * suena nada hasta entonces.
 *
 * Antes se cargaba y reproducia la primera cancion sin preguntar, que es un
 * recibimiento raro para algo que tiene cuatro formas de usarse.
 *
 * Se pinta a mano con Graphics2D, igual que el piano, para que no parezca un
 * dialogo pegado a otro programa.
 */
public class PantallaInicio extends JPanel {

    private static final int COLUMNAS = 2;
    private static final int SEPARACION = 18;
    private static final int MARGEN = 40;
    private static final int ALTO_CABECERA = 132;

    /** Las tarjetas no crecen mas de esto: estiradas se quedan vacias. */
    private static final int ALTO_MAXIMO_TARJETA = 118;
    private static final int ANCHO_GLIFO = 132;

    private static final Color TITULO = new Color(250, 244, 230);
    private static final Color SUBTITULO = new Color(206, 190, 166);
    private static final Color TARJETA = new Color(28, 21, 14, 214);
    private static final Color TARJETA_ENCIMA = new Color(52, 39, 25, 232);
    private static final Color BORDE = new Color(120, 100, 74);
    private static final Color BORDE_ENCIMA = new Color(240, 190, 96);

    private final List<Tarjeta> tarjetas = new ArrayList<>();
    private final Consumer<ModoDeUso> alElegir;
    private Image fondo;
    private Tarjeta debajoDelRaton;

    public PantallaInicio(Consumer<ModoDeUso> alElegir) {
        this.alElegir = alElegir;
        this.fondo = null;
        this.debajoDelRaton = null;
        for (ModoDeUso modo : ModoDeUso.values()) {
            this.tarjetas.add(new Tarjeta(modo));
        }
        setOpaque(true);
        anadirRaton();
    }

    public void setFondo(Image fondo) {
        this.fondo = fondo;
    }

    private void anadirRaton() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Tarjeta antes = PantallaInicio.this.debajoDelRaton;
                PantallaInicio.this.debajoDelRaton = tarjetaEn(e.getX(), e.getY());
                if (antes != PantallaInicio.this.debajoDelRaton) {
                    setCursor(Cursor.getPredefinedCursor(
                            PantallaInicio.this.debajoDelRaton == null
                                    ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
                    repaint();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Tarjeta elegida = tarjetaEn(e.getX(), e.getY());
                if (elegida != null) {
                    PantallaInicio.this.alElegir.accept(elegida.modo);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                PantallaInicio.this.debajoDelRaton = null;
                repaint();
            }
        });
    }

    private Tarjeta tarjetaEn(int x, int y) {
        Tarjeta encontrada = null;
        for (Tarjeta t : this.tarjetas) {
            if (t.caja != null && t.caja.contains(x, y)) {
                encontrada = t;
                break;
            }
        }
        return encontrada;
    }

    /** Reparte las tarjetas en una rejilla que ocupe el hueco disponible. */
    private void recolocar() {
        int filas = (this.tarjetas.size() + COLUMNAS - 1) / COLUMNAS;
        int disponibleAncho = getWidth() - 2 * MARGEN;
        int disponibleAlto = getHeight() - ALTO_CABECERA - MARGEN;
        if (disponibleAncho <= 0 || disponibleAlto <= 0) {
            return;
        }
        int ancho = (disponibleAncho - (COLUMNAS - 1) * SEPARACION) / COLUMNAS;
        int alto = Math.min(ALTO_MAXIMO_TARJETA,
                (disponibleAlto - (filas - 1) * SEPARACION) / filas);

        // La rejilla se centra en el hueco que queda, en vez de estirarse
        int altoRejilla = filas * alto + (filas - 1) * SEPARACION;
        int arriba = ALTO_CABECERA + Math.max(0, (disponibleAlto - altoRejilla) / 2);

        for (int i = 0; i < this.tarjetas.size(); i++) {
            int fila = i / COLUMNAS;
            int columna = i % COLUMNAS;
            this.tarjetas.get(i).caja = new Rectangle(
                    MARGEN + columna * (ancho + SEPARACION),
                    arriba + fila * (alto + SEPARACION),
                    ancho, alto);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        recolocar();

        dibujarFondo(g2);
        dibujarCabecera(g2);
        for (Tarjeta t : this.tarjetas) {
            dibujarTarjeta(g2, t);
        }
        dibujarPie(g2);
    }

    private void dibujarFondo(Graphics2D g) {
        if (this.fondo != null) {
            g.drawImage(this.fondo, 0, 0, getWidth(), getHeight(), null);
        } else {
            g.setColor(new Color(90, 60, 40));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        // La misma vineta que la escena, para que no parezcan dos programas
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radio = (float) Math.max(1, Math.hypot(cx, cy));
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), radio,
                new float[]{0.35f, 1f},
                new Color[]{new Color(0, 0, 0, 40), new Color(0, 0, 0, 165)}));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void dibujarCabecera(Graphics2D g) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 46));
        FontMetrics m = g.getFontMetrics();
        String titulo = "Piano MIDI";
        int x = (getWidth() - m.stringWidth(titulo)) / 2;
        int y = 66;
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(titulo, x + 2, y + 2);
        g.setColor(TITULO);
        g.drawString(titulo, x, y);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        FontMetrics m2 = g.getFontMetrics();
        String sub = "Elige que quieres hacer";
        g.setColor(SUBTITULO);
        g.drawString(sub, (getWidth() - m2.stringWidth(sub)) / 2, y + 26);

        // Un teclado decorativo bajo el titulo
        dibujarTeclasDecorativas(g, getWidth() / 2 - 90, y + 42, 180, 22);
    }

    private void dibujarTeclasDecorativas(Graphics2D g, int x, int y,
                                          int ancho, int alto) {
        int blancas = 10;
        double paso = ancho / (double) blancas;
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(x - 2, y - 2, ancho + 4, alto + 4, 6, 6));
        for (int i = 0; i < blancas; i++) {
            g.setPaint(new LinearGradientPaint(
                    new Point2D.Float(0, y), new Point2D.Float(0, y + alto),
                    new float[]{0f, 1f},
                    new Color[]{Estilo.MARFIL_ALTO, Estilo.MARFIL_BAJO}));
            g.fill(new RoundRectangle2D.Double(x + i * paso + 0.6, y,
                    paso - 1.2, alto, 3, 3));
        }
        int[] negras = {0, 1, 3, 4, 5, 7, 8};
        g.setColor(Estilo.GRAFITO_BAJO);
        for (int i : negras) {
            g.fill(new RoundRectangle2D.Double(
                    x + (i + 1) * paso - paso * 0.28, y,
                    paso * 0.56, alto * 0.62, 2, 2));
        }
    }

    private void dibujarTarjeta(Graphics2D g, Tarjeta t) {
        if (t.caja == null) {
            return;
        }
        boolean encima = t == this.debajoDelRaton;
        RoundRectangle2D forma = new RoundRectangle2D.Double(
                t.caja.x, t.caja.y, t.caja.width, t.caja.height, 14, 14);

        g.setPaint(new GradientPaint(
                0f, t.caja.y, encima ? TARJETA_ENCIMA : TARJETA,
                0f, t.caja.y + t.caja.height, new Color(16, 12, 8, 226)));
        g.fill(forma);
        g.setStroke(new BasicStroke(encima ? 2.2f : 1.2f));
        g.setColor(encima ? BORDE_ENCIMA : BORDE);
        g.draw(forma);

        // El dibujo a la izquierda y el texto al lado, que apilados dejaban la
        // tarjeta medio vacia
        int margen = 16;
        int altoGlifo = t.caja.height - 2 * margen;
        dibujarGlifo(g, t.modo, t.caja.x + margen, t.caja.y + margen,
                ANCHO_GLIFO, altoGlifo);

        int xTexto = t.caja.x + margen + ANCHO_GLIFO + 18;
        int anchoTexto = t.caja.x + t.caja.width - margen - xTexto;

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        FontMetrics mTitulo = g.getFontMetrics();
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        FontMetrics mTexto = g.getFontMetrics();
        List<String> lineas = partir(t.modo.getExplicacion(), mTexto, anchoTexto);

        // Titulo y explicacion centrados a lo alto de la tarjeta
        int altoBloque = mTitulo.getHeight() + lineas.size() * mTexto.getHeight();
        int y = t.caja.y + (t.caja.height - altoBloque) / 2 + mTitulo.getAscent();

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        g.setColor(encima ? BORDE_ENCIMA : TITULO);
        g.drawString(t.modo.getNombre(), xTexto, y);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.setColor(SUBTITULO);
        int linea = y + mTexto.getHeight() + 2;
        for (String trozo : lineas) {
            g.drawString(trozo, xTexto, linea);
            linea += mTexto.getHeight();
        }
    }

    /** Un dibujito por modo, para reconocerlo de un vistazo. */
    private void dibujarGlifo(Graphics2D g, ModoDeUso modo, int x, int y,
                              int ancho, int alto) {
        int altoTeclas = Math.max(10, alto / 3);
        int yTeclas = y + alto - altoTeclas;

        switch (modo) {
            case VER_CANCION -> {
                barras(g, x, y, ancho, alto - altoTeclas - 3, new int[]{1, 3, 5},
                        Estilo.colorDeCanal(1), false);
                dibujarTeclasDecorativas(g, x, yTeclas, ancho, altoTeclas);
            }
            case VER_CANALES -> {
                int cuantos = 3;
                int altoFila = (alto - (cuantos - 1) * 4) / cuantos;
                for (int i = 0; i < cuantos; i++) {
                    dibujarTeclasDecorativas(g, x, y + i * (altoFila + 4),
                            ancho, altoFila);
                }
            }
            case APRENDER -> {
                dibujarTeclasDecorativas(g, x, yTeclas, ancho, altoTeclas);
                // La flecha que senala la tecla que hay que tocar
                int cx = x + ancho / 2;
                int puntaY = yTeclas - 5;
                int lado = Math.max(8, altoTeclas);
                g.setColor(BORDE_ENCIMA);
                g.fillPolygon(new int[]{cx - lado / 2, cx + lado / 2, cx},
                        new int[]{puntaY - lado, puntaY - lado, puntaY}, 3);
            }
            case TOCAR_LIBRE -> {
                barras(g, x, y, ancho, alto - altoTeclas - 3, new int[]{2, 4, 6},
                        Estilo.COLOR_USUARIO, true);
                dibujarTeclasDecorativas(g, x, yTeclas, ancho, altoTeclas);
            }
            default -> dibujarTeclasDecorativas(g, x, yTeclas, ancho, altoTeclas);
        }
    }

    /**
     * Las barras del glifo. Hacia abajo son notas que caen, hacia arriba es el
     * rastro de lo que tocas.
     */
    private void barras(Graphics2D g, int x, int y, int ancho, int alto,
                        int[] posiciones, Color color, boolean haciaArriba) {
        double paso = ancho / 10.0;
        for (int i = 0; i < posiciones.length; i++) {
            double bx = x + posiciones[i] * paso;
            double largo = alto * (0.45 + 0.18 * i);
            double by = haciaArriba ? y + alto - largo : y;
            Color arriba = haciaArriba ? Estilo.conAlfa(color, 60)
                    : Estilo.conAlfa(color, 220);
            Color abajo = haciaArriba ? Estilo.conAlfa(color, 220)
                    : Estilo.conAlfa(color, 60);
            g.setPaint(new GradientPaint(
                    0f, (float) by, arriba, 0f, (float) (by + largo), abajo));
            g.fill(new RoundRectangle2D.Double(bx, by, paso * 0.8, largo, 4, 4));
        }
    }

    private void dibujarPie(Graphics2D g) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        FontMetrics m = g.getFontMetrics();
        String pie = "Tambien puedes arrastrar un fichero .mid sobre la ventana"
                + "   -   Ctrl+1 a Ctrl+4 para cambiar de modo";
        g.setColor(new Color(178, 162, 140));
        g.drawString(pie, (getWidth() - m.stringWidth(pie)) / 2, getHeight() - 14);
    }

    private static List<String> partir(String texto, FontMetrics m, int ancho) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (m.stringWidth(prueba) > ancho && !actual.isEmpty()) {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(prueba);
            }
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    /** Una opcion de la pantalla: su modo y donde esta. */
    private static final class Tarjeta {
        private final ModoDeUso modo;
        private Rectangle caja;

        Tarjeta(ModoDeUso modo) {
            this.modo = modo;
            this.caja = null;
        }
    }
}
