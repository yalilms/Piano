package jd.piano.programa;

/**
 * Una nota con su sitio en el tiempo, en microsegundos.
 *
 * @param canal     canal MIDI, 0-15
 * @param nota      altura MIDI, 0-127
 * @param velocidad fuerza con la que se toca, 1-127
 * @param inicioUs  cuando empieza a sonar
 * @param duracionUs cuanto dura
 */
public record NotaMidi(int canal, int nota, int velocidad,
                       long inicioUs, long duracionUs) {

    public long finUs() {
        return this.inicioUs + this.duracionUs;
    }
}
