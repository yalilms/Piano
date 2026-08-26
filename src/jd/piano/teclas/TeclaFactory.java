package jd.piano.teclas;

public class TeclaFactory {

    private TeclaFactory() {
    }

    public static Tecla crearTecla(int numeroNota, MedidasTeclado medidas) {
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

        return tecla;
    }
}
