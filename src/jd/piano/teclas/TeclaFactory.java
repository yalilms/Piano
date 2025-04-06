package jd.piano.teclas;

public class TeclaFactory {

    private TeclaFactory() {
    }

    public static Tecla crearTecla(int numeroNota) {
        int notaEquivalente = numeroNota % 12;
        Tecla tecla = null;

        if (notaEquivalente == 1 || notaEquivalente == 3 ||
                notaEquivalente == 6 || notaEquivalente == 8 ||
                notaEquivalente == 10) {
            tecla = new TeclaNegra(numeroNota);

        } else if (notaEquivalente == 0 || notaEquivalente == 5) {
            tecla = new TeclaBlanca1(numeroNota);

        } else if (notaEquivalente == 2 || notaEquivalente == 7 ||
                notaEquivalente == 9) {
            tecla = new TeclaBlanca2(numeroNota);

        } else if (notaEquivalente == 4 || notaEquivalente == 11) {
            tecla = new TeclaBlanca3(numeroNota);

        } else {
            tecla = new TeclaBlanca1(numeroNota);
        }

        return tecla;
    }
}