package jd.piano.instrumentos;

import jd.piano.teclas.Estilo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Las doce notas puestas en rueda, y una linea entre las que suenan a la vez.
 *
 * Esto no es un instrumento, es otra forma de mirar la misma cancion: en vez de
 * ver donde estan las notas, se ve que intervalos forman. Un acorde mayor
 * siempre dibuja el mismo triangulo, y al cambiar de tonalidad el triangulo
 * gira sin deformarse. Con un teclado eso no se ve.
 *
 * Como todas las octavas del mismo Do caen en el mismo sitio, se representa el
 * 100% de cualquier cancion, y hay que llevar la cuenta de cuantas notas de
 * cada clase estan sonando: si suenan dos Do y se suelta uno, el nodo tiene que
 * seguir encendido.
 */
public class CirculoCromatico implements InstrumentoVisual {

    private static final int CLASES = 12;
    private static final int CANALES = 16;
    private static final int NOTAS = 128;

    private static final int DIAMETRO_BASE = 620;
    private static final int DIAMETRO_MINIMO = 160;

    private static final Color ANILLO = new Color(214, 196, 168, 90);
    private static final Color RADIO_TENUE = new Color(214, 196, 168, 38);
    private static final Color NODO_APAGADO = new Color(56, 40, 26, 190);
    private static final Color NODO_CANTO = new Color(196, 176, 148, 120);
    private static final Color ETIQUETA = new Color(226, 212, 188, 210);
    private static final Color ETIQUETA_VIVA = new Color(255, 246, 224);

    private final Nodo[] nodos;

    /**
     * Que notas concretas estan sonando y cuantas hay de cada clase. Sin esta
     * cuenta, soltar un Do apagaria el nodo aunque siguiera sonando el Do de
     * la octava de al lado.
     */
    private final boolean[][] sonando;
    private final int[] cuantas;

    private Point posicion;
    private int diametro;

    public CirculoCromatico() {
        this.nodos = new Nodo[CLASES];
        for (int clase = 0; clase < CLASES; clase++) {
            this.nodos[clase] = new Nodo(clase);
        }
        this.sonando = new boolean[CANALES][NOTAS];
        this.cuantas = new int[CLASES];
        this.posicion = null;
        this.diametro = DIAMETRO_BASE;
    }

    @Override
    public String getNombre() {
        return "Circulo cromatico";
    }

    private static int claseDe(int nota) {
        return ((nota % CLASES) + CLASES) % CLASES;
    }

    @Override
    public PiezaPulsable piezaDe(int canal, int nota) {
        return nota < 0 || nota >= NOTAS ? null : this.nodos[claseDe(nota)];
    }

    /**
     * Se apunta la nota concreta y se sube la cuenta de su clase. El nodo se
     * enciende con el color del canal y con la fuerza de la nota.
     */
    @Override
    public boolean pulsar(int canal, int nota, int velocidad) {
        if (canal < 0 || canal >= CANALES || nota < 0 || nota >= NOTAS) {
            return false;
        }
        int clase = claseDe(nota);
        if (!this.sonando[canal][nota]) {
            this.sonando[canal][nota] = true;
            this.cuantas[clase]++;
        }
        this.nodos[clase].setColorPulsado(Estilo.colorDeCanal(canal));
        this.nodos[clase].pulsar(velocidad);
        return true;
    }

    @Override
    public void soltar(int canal, int nota) {
        if (canal < 0 || canal >= CANALES || nota < 0 || nota >= NOTAS) {
            return;
        }
        int clase = claseDe(nota);
        if (this.sonando[canal][nota]) {
            this.sonando[canal][nota] = false;
            this.cuantas[clase]--;
        }
        if (this.cuantas[clase] <= 0) {
            this.cuantas[clase] = 0;
            this.nodos[clase].soltar();
        }
    }

    @Override
    public void soltarTodas() {
        for (int canal = 0; canal < CANALES; canal++) {
            for (int nota = 0; nota < NOTAS; nota++) {
                this.sonando[canal][nota] = false;
            }
        }
        for (int clase = 0; clase < CLASES; clase++) {
            this.cuantas[clase] = 0;
            if (this.nodos[clase].estaPulsado()) {
                this.nodos[clase].soltar();
            }
        }
    }

    @Override
    public void soltarCanal(int canal) {
        if (canal < 0 || canal >= CANALES) {
            return;
        }
        for (int nota = 0; nota < NOTAS; nota++) {
            if (this.sonando[canal][nota]) {
                soltar(canal, nota);
            }
        }
    }

    @Override
    public PiezaPulsable piezaEn(int x, int y) {
        if (this.posicion == null) {
            return null;
        }
        for (Nodo nodo : this.nodos) {
            if (nodo.getForma().contains(x, y)) {
                return nodo;
            }
        }
        return null;
    }

    /**
     * En la rueda no tiene sentido que caigan notas desde arriba: cruzarian por
     * encima del circulo para aterrizar en los nodos de abajo.
     */
    @Override
    public boolean admiteCascada() {
        return false;
    }

    /** Una rueda flotando no se apoya en nada: no tiene donde reflejarse. */
    @Override
    public boolean tieneReflejo() {
        return false;
    }

    // ---------- colocacion ----------

    @Override
    public void setPosicion(int x, int y) {
        this.posicion = new Point(x, y);
        colocarNodos();
    }

    @Override
    public Rectangle getLimites() {
        return this.posicion == null ? null
                : new Rectangle(this.posicion.x, this.posicion.y,
                        getAnchura(), getAltura());
    }

    @Override
    public int getAnchura() {
        return this.diametro;
    }

    @Override
    public int getAltura() {
        return this.diametro;
    }

    @Override
    public void ajustarA(int ancho, int alto) {
        this.diametro = Math.max(DIAMETRO_MINIMO,
                Math.min(DIAMETRO_BASE, Math.min(ancho, alto)));
        colocarNodos();
    }

    private int centroX() {
        return this.posicion.x + this.diametro / 2;
    }

    private int centroY() {
        return this.posicion.y + this.diametro / 2;
    }

    private int radioNodo() {
        return Math.max(7, this.diametro / 18);
    }

    private int tamanoLetra() {
        return Math.max(10, this.diametro / 24);
    }

    /**
     * Radio de la circunferencia sobre la que van los centros de los nodos.
     * Se descuenta el nodo y la etiqueta para que quepan dentro de la caja: al
     * repartir solo el diametro, los nombres de las notas salian pintados
     * encima de los propios nodos.
     */
    private int radioAnillo() {
        return this.diametro / 2 - radioNodo() - tamanoLetra() - 6;
    }

    /** El Do arriba, y las demas en el sentido de las agujas del reloj. */
    private double anguloDe(int clase) {
        return -Math.PI / 2 + clase * 2 * Math.PI / CLASES;
    }

    private int xDe(int clase) {
        return (int) Math.round(centroX() + Math.cos(anguloDe(clase)) * radioAnillo());
    }

    private int yDe(int clase) {
        return (int) Math.round(centroY() + Math.sin(anguloDe(clase)) * radioAnillo());
    }

    private void colocarNodos() {
        if (this.posicion == null) {
            return;
        }
        int radio = radioNodo();
        for (int clase = 0; clase < CLASES; clase++) {
            Nodo nodo = this.nodos[clase];
            nodo.setRadio(radio);
            nodo.setPosicion(xDe(clase) - radio, yDe(clase) - radio);
        }
    }

    // ---------- dibujado ----------

    @Override
    public void dibujar(Graphics2D g) {
        dibujarRueda(g);
        dibujarIntervalos(g);
        for (Nodo nodo : this.nodos) {
            nodo.dibujar(g);
        }
        dibujarEtiquetas(g);
    }

    @Override
    public void dibujarEnReposo(Graphics2D g) {
        dibujarRueda(g);
        for (Nodo nodo : this.nodos) {
            nodo.dibujarEnReposo(g);
        }
        dibujarEtiquetas(g);
    }

    @Override
    public void dibujarBrillos(Graphics2D g) {
        for (Nodo nodo : this.nodos) {
            nodo.dibujarBrillo(g);
        }
    }

    /**
     * Toda la rueda de una vez, no los nodos por separado.
     *
     * Es a proposito: las lineas de los intervalos van de un nodo a otro, asi
     * que la region a repintar tiene que abarcar el circulo entero. Si se
     * devolviera solo la caja de cada nodo encendido, las lineas saldrian
     * cortadas por donde pasan por encima de lo que no se repinta.
     */
    @Override
    public void anadirRegionesActivas(List<Rectangle> destino) {
        boolean alguno = false;
        for (Nodo nodo : this.nodos) {
            alguno = alguno || nodo.getIntensidad() > 0;
        }
        if (alguno && this.posicion != null) {
            Rectangle todo = getLimites();
            todo.grow(PiezaPulsable.MARGEN_REPINTADO, PiezaPulsable.MARGEN_REPINTADO);
            destino.add(todo);
        }
    }

    private void dibujarRueda(Graphics2D g) {
        if (this.posicion == null) {
            return;
        }
        int radio = radioAnillo();
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(ANILLO);
        g.draw(new Ellipse2D.Double(centroX() - radio, centroY() - radio,
                radio * 2, radio * 2));

        // Los radios, para que se vea que las doce estan repartidas por igual
        g.setStroke(new BasicStroke(1f));
        g.setColor(RADIO_TENUE);
        for (int clase = 0; clase < CLASES; clase++) {
            g.drawLine(centroX(), centroY(), xDe(clase), yDe(clase));
        }
    }

    /**
     * Una linea entre cada par de notas que suenan a la vez. Es lo que dibuja
     * la forma del acorde: dos notas dan una cuerda, tres un triangulo, y la
     * misma figura girada es el mismo acorde en otra tonalidad.
     */
    private void dibujarIntervalos(Graphics2D g) {
        List<Integer> vivas = new ArrayList<>();
        for (int clase = 0; clase < CLASES; clase++) {
            if (this.nodos[clase].getIntensidad() > 0.05) {
                vivas.add(clase);
            }
        }
        if (vivas.size() < 2) {
            return;
        }
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < vivas.size(); i++) {
            for (int j = i + 1; j < vivas.size(); j++) {
                Nodo a = this.nodos[vivas.get(i)];
                Nodo b = this.nodos[vivas.get(j)];
                // La linea se ve tanto como la mas apagada de las dos notas
                double fuerza = Math.min(a.getIntensidad(), b.getIntensidad());
                Color tinte = Estilo.mezclar(a.getColorPulsado(),
                        b.getColorPulsado(), 0.5);
                g.setColor(Estilo.conAlfa(tinte, (int) Math.round(170 * fuerza)));
                g.drawLine(xDe(vivas.get(i)), yDe(vivas.get(i)),
                        xDe(vivas.get(j)), yDe(vivas.get(j)));
            }
        }
    }

    private void dibujarEtiquetas(Graphics2D g) {
        if (this.posicion == null) {
            return;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamanoLetra()));
        FontMetrics m = g.getFontMetrics();
        // Justo por fuera del nodo, no encima de el
        int fuera = radioAnillo() + radioNodo() + tamanoLetra() / 2 + 4;
        for (int clase = 0; clase < CLASES; clase++) {
            String texto = Estilo.nombreDeNota(clase);
            int x = (int) Math.round(centroX() + Math.cos(anguloDe(clase)) * fuera);
            int y = (int) Math.round(centroY() + Math.sin(anguloDe(clase)) * fuera);
            boolean viva = this.nodos[clase].getIntensidad() > 0.05;
            g.setColor(viva ? ETIQUETA_VIVA : ETIQUETA);
            g.drawString(texto, x - m.stringWidth(texto) / 2f,
                    y + m.getAscent() / 2f - 1);
        }
    }

    /**
     * Una de las doce notas de la rueda. Se dibuja siempre, apagada o no: sin
     * los doce puntos no se entiende que es una rueda de doce.
     */
    private static final class Nodo extends PiezaPulsable {

        private int radio;

        Nodo(int clase) {
            super(clase);
            this.radio = 12;
        }

        void setRadio(int radio) {
            this.radio = radio;
            invalidarForma();
        }

        @Override
        public Color getColorNoPulsado() {
            return NODO_APAGADO;
        }

        @Override
        protected int getHundimiento() {
            return 0;
        }

        @Override
        protected Shape construirForma() {
            return new Ellipse2D.Double(this.posicion.x, this.posicion.y,
                    this.radio * 2, this.radio * 2);
        }

        @Override
        protected double getCentroDelBrillo(Rectangle2D caja) {
            return caja.getCenterY();
        }

        @Override
        protected void dibujar(Graphics2D g, double intensidad) {
            Shape forma = getForma();
            g.setColor(NODO_APAGADO);
            g.fill(forma);
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(NODO_CANTO);
            g.draw(forma);

            if (intensidad <= 0.02) {
                return;
            }
            Color tinte = getColorPulsado();
            g.setColor(Estilo.conAlfa(Estilo.mezclar(tinte, Color.WHITE, 0.35),
                    (int) Math.round(250 * intensidad)));
            g.fill(forma);
            g.setStroke(new BasicStroke(2f));
            g.setColor(Estilo.conAlfa(Estilo.mezclar(tinte, Color.WHITE, 0.7),
                    (int) Math.round(255 * intensidad)));
            g.draw(forma);
        }
    }
}
