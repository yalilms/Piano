package jd.piano.pruebas;

import java.util.ArrayList;
import java.util.List;

/**
 * Lanza todas las comprobaciones y devuelve un codigo de salida distinto de
 * cero si alguna falla, para poder usarlo desde un script.
 *
 * No necesita pantalla: todo lo que se comprueba aqui funciona sin ventana.
 * Lo que si hace falta comprobar a mano es lo que no se puede automatizar
 * (que suene, que el raton y el teclado respondan, que vaya fluido).
 *
 * Uso:  .\probar.ps1
 */
public final class Pruebas {

    private Pruebas() {
    }

    public static void main(String[] args) {
        List<Comprobador> resultados = new ArrayList<>();
        resultados.add(PruebaGeometria.ejecutar());
        resultados.add(PruebaMidi.ejecutar());
        resultados.add(PruebaReproductor.ejecutar());
        resultados.add(PruebaInterfaz.ejecutar());

        int aciertos = 0;
        int fallos = 0;
        System.out.println();
        System.out.println("== RESUMEN ==");
        for (Comprobador c : resultados) {
            aciertos += c.getAciertos();
            fallos += c.getFallos();
            System.out.printf("  %-26s %2d bien, %d mal%n",
                    c.getTitulo(), c.getAciertos(), c.getFallos());
        }
        System.out.println();
        System.out.printf("  %d comprobaciones, %d fallos%n", aciertos + fallos, fallos);
        System.out.println(fallos == 0 ? "  TODO CORRECTO" : "  HAY FALLOS");

        if (fallos > 0) {
            System.exit(1);
        }
    }
}
