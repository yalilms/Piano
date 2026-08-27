# Piano MIDI 🎹

Visualizador de ficheros MIDI en Java: las notas caen sobre un teclado que se
ilumina al ritmo de la canción, y también se puede tocar.

![Piano sencillo con las notas cayendo](docs/piano-sencillo.png)

## 📋 Descripción

Proyecto académico grupal realizado para el Grado Superior en Desarrollo de
Aplicaciones Multiplataforma (DAM) en el IES Hermenegildo Lanz (Granada).

## ✨ Qué hace

- **Notas cayendo** — barras de color que bajan y llegan a su tecla justo cuando
  esa nota empieza a sonar. La altura de cada barra es su duración.
- **Un teclado por canal** — modo alternativo con un teclado para cada canal
  MIDI de la canción, cada uno con su color.
- **Se puede tocar** — con el teclado del ordenador o con el ratón, encima de la
  canción o a solas.
- **Mezcla** — silenciar o aislar cualquier canal, con el nombre del instrumento
  leído del propio fichero.
- **Transporte** — pausa, salto a cualquier punto y tempo de x0,25 a x3.

![Un teclado por canal](docs/multi-piano.png)

## ▶️ Cómo se lanza

```powershell
.\ejecutar-swing.ps1
```

El script comprueba el JDK, se baja FlatLaf la primera vez y compila. Si
prefieres hacerlo a mano:

```powershell
$fuentes = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
$fuentes | Set-Content "$env:TEMP\fuentes.txt" -Encoding ascii
javac -encoding UTF-8 -cp lib\flatlaf-3.7.2.jar -d out "@$env:TEMP\fuentes.txt"
java -cp "out;lib/*" jd.piano.programa.ProgramaSwing
```

> El `-Encoding ascii` no es un descuido: con `utf8`, PowerShell mete un BOM al
> principio del fichero y `javac` lo rechaza.

## ⚙️ Requisitos

- **JDK 21 o superior.** Se usan `record`, `List.getFirst()`, `instanceof` con
  patrón y bloques de texto.
- **FlatLaf** para el aspecto de los controles. Es opcional: si el JAR no está,
  el programa arranca con el aspecto de serie de Swing. Ver
  [lib/LEEME.md](lib/LEEME.md).
- Un dispositivo MIDI en el sistema. El sintetizador que trae Java sirve.

## ⌨️ Controles

| | |
|---|---|
| Clic en el piano | Le da el foco al teclado |
| `Z` `S` `X` `D` `C` `V` `G` `B` `H` `N` `J` `M` | Una octava: las letras de abajo son blancas, las de encima negras |
| `Q` `2` `W` `3` `E` `R` `5` `T` `6` `Y` `7` `U` `I` | La octava siguiente |
| `←` `→` | Baja o sube una octava |
| Espacio | Pausa y reanuda |
| Clic y arrastre en las teclas | Toca con el ratón |

Lo que tocas suena por el canal 16, que es el que menos usan los ficheros MIDI,
así que no le cambia el instrumento a ninguna pista de la canción.

## 🎵 Canciones

Las de la carpeta `canciones` salen solas en la lista. También se puede
**arrastrar un `.mid`** sobre la ventana.

## 🧱 Cómo está organizado

```
src/jd/piano/teclas/     el instrumento: teclas, medidas y estilo
  Tecla, TeclaBlanca1/2/3, TeclaNegra     una tecla y sus formas
  Piano                                   teclado abstracto
  MedidasTeclado, LayoutTeclados          tamaños y reparto en pantalla
  Estilo                                  paleta y nombres de nota

src/jd/piano/programa/   la aplicación
  PianoSencillo, MultiPiano               los dos modos
  Escena, Lienzo, VentanaPrincipal        dibujado y ventana
  Cascada, Particulas                     capas animadas
  Partitura, AnalisisMidi, Reproductor    el MIDI
  SintetizadorTeclado                     tocar
```

Dos decisiones que explican casi todo lo demás:

**Las teclas son formas, no listas de vértices.** Cada blanca se construye
restando del cuerpo el hueco de las negras vecinas. Eso hace imposible
equivocarse en una coordenada, y de paso regala el saber en qué tecla se ha
hecho clic: basta preguntarle a la forma si contiene el punto.

**Se dibuja por frames, no acumulando.** El fondo y el teclado en reposo se
pre-componen en dos imágenes, y en cada frame solo se repintan las zonas donde
hay teclas encendidas. Sin eso no habría animación posible, y con 88 teclas no
se llegaría a 60 fps.

## ✅ Comprobaciones

```powershell
.\probar.ps1
```

57 comprobaciones automáticas que no necesitan pantalla. Vigilan lo que se
puede romper sin darse cuenta:

- Que las teclas **encajen exactas** a cualquier escala, sin pisarse ni dejar
  huecos. Es el invariante que ya se rompió una vez: `TeclaBlanca2` y `3`
  enumeraban vértices a mano y una coordenada mal metía una cuña blanca encima
  de la tecla negra vecina.
- Que el clic acierte la tecla, **barriendo los 129 074 puntos** de la
  superficie del teclado uno a uno.
- Que la conversión de pulsos a tiempo cuadre con la duración que declara Java,
  incluso en las canciones que cambian de tempo. Si se desviara, las notas
  cayendo se irían desincronizando.
- Que no se pierda ninguna nota al leer el fichero.
- El transporte entero: pausa, saltos con sus límites, tempo acotado.
- Que no queden notas encendidas para siempre, por ninguna de las tres vías por
  las que pasaba.

Lo que **no** cubren, porque no se puede automatizar sin pantalla ni oído: que
suene, que el ratón y el teclado respondan como esperas, y que vaya fluido.

## 📷 Capturas

El botón **Captura** guarda lo que se ve en `capturas/` como PNG.

## 👥 Autores

Proyecto de grupo. *(Pendiente de completar con los nombres.)*
