package jd.piano.pruebas;

import jd.piano.programa.BancoDeSonidos;
import jd.piano.programa.PianoSencillo;
import jd.piano.programa.SintetizadorTeclado;
import jd.piano.teclas.Piano;

import javax.sound.midi.VoiceStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Todos los sonidos que el sintetizador sabe hacer, no solo los 128 de la lista
 * fija.
 *
 * El programa ofrecia 128 porque los leia de un array escrito a mano, y el banco
 * que trae Java es el GS de Roland, que tiene 235 en trece bancos. Los 107 que
 * faltaban son las variaciones y, sobre todo, los efectos de sonido: de 8
 * efectos elegibles se pasa a 41.
 *
 * Lo que hay que comprobar de verdad es que elegir uno cambie el sonido y no
 * solo la etiqueta, porque el banco no viaja en el cambio de programa: son dos
 * cosas distintas en MIDI.
 */
public final class PruebaSonidos {

    private PruebaSonidos() {
    }

    public static Comprobador ejecutar() {
        Comprobador c = new Comprobador("banco de sonidos");
        catalogo(c);
        suenanDeVerdad(c);
        return c;
    }

    /** El catalogo: que salga completo, ordenado y sin nombres ambiguos. */
    private static void catalogo(Comprobador c) {
        BancoDeSonidos estandar = BancoDeSonidos.elEstandar();
        c.comprobar("el estandar son los 128 de General MIDI",
                estandar.cuantos() == 128);
        c.comprobar("sin sintetizador se cae al estandar",
                BancoDeSonidos.de(null).cuantos() == 128);

        SintetizadorTeclado s = new SintetizadorTeclado();
        s.abrir();
        try {
            BancoDeSonidos banco = BancoDeSonidos.de(s.getSintetizador());
            c.dato(banco.getNombreDelBanco() + ": " + banco.cuantos()
                    + " sonidos (con la lista fija habria "
                    + estandar.cuantos() + ")");
            c.comprobar("el sintetizador ofrece mas que la lista fija",
                    banco.cuantos() > estandar.cuantos());

            // Ningun sitio repetido: dos entradas con el mismo banco y programa
            // serian la misma cosa dos veces
            Set<String> sitios = new HashSet<>();
            boolean repetido = false;
            for (BancoDeSonidos.Sonido x : banco.getSonidos()) {
                repetido = repetido
                        || !sitios.add(x.banco() + ":" + x.programa());
            }
            c.comprobar("ningun sitio de banco y programa sale dos veces",
                    !repetido);

            // Todas las etiquetas distintas, que si no no se pueden elegir
            Set<String> etiquetas = new HashSet<>();
            boolean etiquetaRepetida = false;
            for (BancoDeSonidos.Sonido x : banco.getSonidos()) {
                etiquetaRepetida = etiquetaRepetida || !etiquetas.add(x.etiqueta());
            }
            c.comprobar("ninguna etiqueta se repite", !etiquetaRepetida);

            // El banco de siempre va primero y en su orden, que es el que la
            // gente reconoce
            List<BancoDeSonidos.Sonido> lista = banco.getSonidos();
            boolean ordenBueno = true;
            int programaAnterior = -1;
            int i = 0;
            while (i < lista.size() && lista.get(i).banco() == 0) {
                ordenBueno = ordenBueno
                        && lista.get(i).programa() > programaAnterior;
                programaAnterior = lista.get(i).programa();
                i++;
            }
            c.dato("los primeros " + i + " son del banco de siempre");
            c.comprobar("el banco de siempre son 128 y van primero y en orden",
                    ordenBueno && i == 128);
            boolean sinVolverAlCero = true;
            for (int j = i; j < lista.size(); j++) {
                sinVolverAlCero = sinVolverAlCero && lista.get(j).banco() != 0;
            }
            c.comprobar("detras no vuelve a aparecer el banco de siempre",
                    sinVolverAlCero);

            // Los efectos, que es lo que se queria poder elegir
            int efectos = 0;
            for (BancoDeSonidos.Sonido x : banco.getSonidos()) {
                if (x.programa() >= 120) {
                    efectos++;
                }
            }
            c.dato(efectos + " efectos de sonido elegibles");
            c.comprobar("hay muchos mas efectos que los 8 del estandar",
                    efectos > 8);

            // Buscar por sitio, que es como se guardan las preferencias
            BancoDeSonidos.Sonido piano = banco.buscar(0, 0);
            c.comprobar("se encuentra un sonido por su banco y programa",
                    piano != null && piano.programa() == 0);
            c.comprobar("un sitio que no existe devuelve null",
                    banco.buscar(9999, 77) == null);
            c.comprobar("siempre hay un sonido por defecto",
                    banco.porDefecto() != null);
            c.comprobar("la familia sale del programa",
                    BancoDeSonidos.familia(0).equals("pianos")
                            && BancoDeSonidos.familia(125)
                                    .equals("efectos de sonido"));
        } finally {
            s.cerrar();
        }
    }

    /**
     * Que elegir un sonido cambie de verdad lo que suena.
     *
     * Se toca una nota distinta con cada sonido y se busca SU voz por el numero
     * de nota. Hay que hacerlo asi: Gervill deja las voces viejas marcadas como
     * activas, asi que quedarse con la primera activa del canal devuelve la de
     * la nota anterior; y el banco no se puede leer del canal con getController,
     * porque no lo refleja ahi aunque el cambio si haya surtido efecto. Las dos
     * cosas dieron falsos fallos antes de dar con esta forma de medirlo.
     */
    private static void suenanDeVerdad(Comprobador c) {
        SintetizadorTeclado s = new SintetizadorTeclado();
        s.abrir();
        Piano piano = new PianoSencillo(21, 108);
        piano.setPosicion(0, 0);
        s.conectar(piano);
        try {
            BancoDeSonidos banco = BancoDeSonidos.de(s.getSintetizador());
            // Uno del banco de siempre y varios de los bancos de variacion
            List<BancoDeSonidos.Sonido> muestra = muestrear(banco);
            if (muestra.isEmpty()) {
                c.comprobar("hay sonidos que probar", false);
                return;
            }

            int bien = 0;
            String falla = "";
            int nota = 48;
            for (BancoDeSonidos.Sonido x : muestra) {
                nota++;
                s.setSonido(x.banco(), x.programa());
                s.encender(nota);
                esperar(200);
                int[] visto = loQueSuena(s, nota);
                s.apagar(nota);
                esperar(80);
                if (visto[0] == x.banco() && visto[1] == x.programa()) {
                    bien++;
                } else if (falla.isEmpty()) {
                    falla = x.nombre() + ": pedido " + x.banco() + "/"
                            + x.programa() + ", suena " + visto[0] + "/"
                            + visto[1];
                }
            }
            if (!falla.isEmpty()) {
                c.dato(falla);
            }
            c.dato("probados " + muestra.size() + " sonidos de bancos distintos");
            c.comprobar("cada sonido elegido es el que suena de verdad",
                    bien == muestra.size());

            // Y que el sintetizador se quede con lo pedido
            BancoDeSonidos.Sonido ultimo = muestra.get(muestra.size() - 1);
            c.comprobar("el sintetizador recuerda que sonido tiene puesto",
                    s.getBanco() == ultimo.banco()
                            && s.getPrograma() == ultimo.programa());

            // setPrograma sigue valiendo: es el banco de siempre
            s.setPrograma(42);
            c.comprobar("setPrograma es el banco de siempre",
                    s.getBanco() == 0 && s.getPrograma() == 42);
            c.comprobar("todavia no hay ningun banco externo cargado",
                    !s.tieneBancoPropio());
        } finally {
            s.cerrar();
        }
    }

    /** Un sonido de cada banco distinto, hasta seis. */
    private static List<BancoDeSonidos.Sonido> muestrear(BancoDeSonidos banco) {
        List<BancoDeSonidos.Sonido> muestra = new java.util.ArrayList<>();
        Set<Integer> vistos = new HashSet<>();
        for (BancoDeSonidos.Sonido x : banco.getSonidos()) {
            if (vistos.add(x.banco()) && muestra.size() < 6) {
                muestra.add(x);
            }
        }
        return muestra;
    }

    private static int[] loQueSuena(SintetizadorTeclado s, int nota) {
        for (VoiceStatus v : s.getSintetizador().getVoiceStatus()) {
            if (v.active && v.channel == SintetizadorTeclado.CANAL
                    && v.note == nota) {
                return new int[]{v.bank, v.program};
            }
        }
        return new int[]{-1, -1};
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
