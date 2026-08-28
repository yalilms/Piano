package jd.piano.teclas;

/**
 * Crea la tecla que le toca a cada nota.
 *
 * Ademas de la forma, decide si a las blancas de los extremos hay que recortarles
 * el hueco de la negra vecina: en el ultimo Do de un piano no hay Do# detras, y
 * recortarlo dejaba un mordisco en la esquina.
 */
public class TeclaFactory {

    private TeclaFactory() {
    }

    /**
     * Una tecla suelta, sin teclado alrededor: se dibuja como si tuviera
     * vecinas por los dos lados.
     */
    public static Tecla crearTecla(int numeroNota, MedidasTeclado medidas) {
        return crearTecla(numeroNota, medidas, 0, 127);
    }

    /**
     * La tecla de esa nota dentro de un teclado que va de notaMinima a
     * notaMaxima. El rango es lo que permite dejar enteras la primera y la
     * ultima.
     */
    public static Tecla crearTecla(int numeroNota, MedidasTeclado medidas,
                                   int notaMinima, int notaMaxima) {
        int notaEquivalente = ((numeroNota % 12) + 12) % 12;
        Tecla tecla;

        if (notaEquivalente == 1 || notaEquivalente == 3 ||
                notaEquivalente == 6 || notaEquivalente == 8 ||
                notaEquivalente == 10) {
            tecla = new TeclaNegra(numeroNota, medidas);

        } else if (notaEquivalente == 0 || notaEquivalente == 5) {
            tecla = new TeclaBlanca1(numeroNota, medidas);

        } else if (notaEquivalente == 2 || notaEquivalente == 7 ||
                notaEquivalente == 9) {
            tecla = new TeclaBlanca2(numeroNota, medidas);

        } else {
            tecla = new TeclaBlanca3(numeroNota, medidas);
        }

        if (tecla instanceof TeclaBlanca blanca) {
            blanca.setNegrasVecinas(numeroNota - 1 >= notaMinima,
                    numeroNota + 1 <= notaMaxima);
        }
        return tecla;
    }
}
