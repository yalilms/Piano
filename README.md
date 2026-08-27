# Piano MIDI 🎹

Visualizador de ficheros MIDI en Java: las notas caen sobre un teclado que se
ilumina al ritmo de la canción, y también se puede tocar.

![Pantalla de inicio](docs/inicio.jpg)

## 📋 Descripción

Proyecto académico grupal realizado para el Grado Superior en Desarrollo de
Aplicaciones Multiplataforma (DAM) en el IES Hermenegildo Lanz (Granada).

## 🎛️ Cinco modos de uso

Al abrir sale la pantalla de inicio y **no suena nada** hasta que eliges.
Después se puede cambiar de modo en el menú **Modo**, con `Ctrl+1` a `Ctrl+5`,
o volver al inicio con `Ctrl+0`.

| Modo | Qué hace |
|---|---|
| **Ver una canción** | Un instrumento con las notas cayendo |
| **Ver por canales** | Un teclado por cada canal de la canción |
| **Varios instrumentos** | Cada pista en el instrumento que le pega, todos a la vez |
| **Aprender a tocarla** | Se para y espera a que toques la nota que viene |
| **Tocar libre** | Sin canción: el teclado para ti |

## 🎸 Cinco instrumentos

Un piano no es la única forma de mirar una canción. El desplegable **Ver como**
cambia con qué se dibuja, y el panel avisa de cuánto de la canción cabe en el
que elijas, porque una guitarra no llega a las notas graves de un piano.

| Instrumento | Qué es | Alcance |
|---|---|---|
| **Teclado** | Las 88 teclas de un piano de cola, de La0 a Do8 | prácticamente todo |
| **Guitarra** | Seis cuerdas y quince trastes | 40 notas |
| **Bajo** | Cuatro cuerdas, una octava más grave | 31 notas |
| **Xilófono** | Cinco octavas de barras de madera, las alteradas detrás | 61 notas |
| **Círculo cromático** | Las doce notas en rueda, con líneas entre las que suenan juntas | todo |

En el mástil una nota se puede pisar en varias posiciones —el Mi de la cuarta
cuerda es el mismo que el de la quinta con cinco trastes— y se elige siempre el
traste más bajo, que es el que queda cerca de la cejuela. Los trastes se separan
como en un mástil de verdad, cada vez menos conforme se sube.

![Xilófono](docs/xilofono.jpg)

El círculo cromático no es un instrumento: es una forma de ver los intervalos.
Un acorde mayor siempre dibuja el mismo triángulo, y al cambiar de tonalidad el
triángulo gira sin deformarse. Con un teclado eso no se ve. Es el único que
representa el 100 % de cualquier canción, porque todas las octavas de la misma
nota caen en el mismo sitio.

![Círculo cromático](docs/circulo.jpg)

### Varios instrumentos a la vez

Reparte los canales de la canción entre varios instrumentos apilados, mirando el
`PROGRAM_CHANGE` de cada uno: las pistas de guitarra al mástil, las de bajo al
bajo, las de marimba al xilófono y el resto al teclado. Cada panel lleva el
color de los canales que le tocan.

El reparto tiene la última palabra el rango, no la familia: una pista de
guitarra que baja al Mi grave del bajo no cabe en un mástil, así que se le da un
teclado. Con eso el reparto **no pierde nada que un teclado solo sí mostraría**
—medido sobre las nueve canciones: entre el 99,7 % y el 100 % de las notas.

Y se puede cambiar a mano: cada canal tiene su desplegable **se ve en** en el
panel de mezcla, y moverlo no para la canción.

![Varios instrumentos](docs/conjunto.jpg)

### Tocar libre

Sin ninguna canción cargada. Lo que toques deja una barra que crece hacia arriba
mientras mantienes la tecla y se desvanece al soltarla, con el nombre de la
última nota en grande. El espacio hace de pedal de resonancia.

![Tocar libre](docs/tocar-libre.jpg)

## ✨ Qué hace

- **Notas cayendo** — barras de color que bajan y llegan a su tecla justo cuando
  esa nota empieza a sonar. La altura de cada barra es su duración.
- **Batería** — los golpes del canal 10 como pads con el nombre de cada pieza.
  Se montan solo los que la canción usa. Antes ese canal se descartaba entero:
  eran el **35 % de todas las notas**.
- **Modo aprendizaje** — la canción se para y espera a que toques la nota que
  viene, estilo Synthesia.
- **Un teclado por canal** — modo alternativo con un teclado para cada canal
  MIDI de la canción, cada uno con su color.
- **Cinco instrumentos** — teclado, guitarra, bajo, xilófono y círculo
  cromático, intercambiables sobre la marcha.
- **Varios a la vez** — cada pista de la canción en el instrumento que le pega,
  repartido solo y cambiable canal por canal.
- **Se puede tocar** — con el teclado del ordenador o con el ratón, eligiendo
  entre los 128 instrumentos de General MIDI.
- **Mezcla** — silenciar o aislar cualquier canal, y cambiar el instrumento de
  cualquier pista sin tocar el fichero.
- **Transporte** — pausa, salto a cualquier punto y tempo de x0,25 a x3.

![Notas cayendo](docs/piano-sencillo.jpg)

### Modo aprendizaje

Señala la tecla que toca con contorno, flecha y un cartel, y no sigue hasta que
la pulses. Vigila un solo canal —en estas canciones hay hasta trece sonando a la
vez y no habría manera de tocarlos todos— y elige el que más se parece a la
melodía. Al activarlo adelanta hasta la primera nota que hay que tocar.

![Modo aprendizaje esperando una nota](docs/aprendizaje.jpg)

### Un teclado por canal

![Un teclado por canal](docs/multi-piano.jpg)

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
| Espacio | Pausa, o pedal de resonancia en el modo libre |
| `Ctrl+1` a `Ctrl+5` | Cambiar de modo |
| `Ctrl+0` | Volver a la pantalla de inicio |
| Clic y arrastre en las teclas | Toca con el ratón |

Lo que tocas suena por el canal 16, que es el que menos usan los ficheros MIDI,
así que no le cambia el instrumento a ninguna pista de la canción. Con el
desplegable **Tocas con** eliges cualquiera de los 128 instrumentos de General
MIDI: el banco que trae Java los tiene cargados desde el arranque.

## 🎵 Canciones

Las de la carpeta `canciones` salen solas en la lista. También se puede
**arrastrar un `.mid`** sobre la ventana.

## 🧱 Cómo está organizado

```
src/jd/piano/instrumentos/ los instrumentos y lo que comparten
src/jd/piano/teclas/     el teclado: teclas, medidas y estilo
  Tecla, TeclaBlanca1/2/3, TeclaNegra     una tecla y sus formas
  Piano                                   teclado abstracto
  MedidasTeclado, LayoutTeclados          tamaños y reparto en pantalla
  Estilo                                  paleta y nombres de nota

src/jd/piano/programa/   la aplicación
  PianoSencillo, MultiPiano               los dos dibujados de teclado
  Escena, Lienzo, VentanaPrincipal        dibujado y ventana
  Cascada, Particulas, Bateria            capas animadas
  PantallaInicio                          el menu de inicio
  ModoDeUso, BarraDeMenu                  los cinco modos y el menu
  ModoAprendizaje                         esperar la nota correcta
  RastroLibre                             el rastro del modo libre
  Partitura, AnalisisMidi, Reproductor    el MIDI
  InstrumentosGM, PiezasPercusion         nombres de General MIDI
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

164 comprobaciones automáticas que no necesitan pantalla. Vigilan lo que se
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
- Que buscar funcione **mientras suena**, no solo en pausa. El secuenciador de
  Java ignora el cambio de posición si está en marcha, así que pinchar en la
  barra de progreso no hacía nada, y ninguna comprobación lo cubría.
- El mapa completo de percusión y el montaje de los pads.
- Que el modo aprendizaje pare al llegar a un acorde y siga al tocarlo.
- Que los cinco modos sean coherentes: no puede haber notas cayendo sin
  canción, ni con varios teclados, ni con varios instrumentos apilados.
- Que el rastro del modo libre aparezca al tocar.
- Que la pantalla de inicio se pinte y que cada tarjeta elija su modo, sin que
  ninguna pise a otra. La posición se le pregunta a la pantalla en vez de
  repetir aquí la cuenta de la rejilla, que es justo lo que se quedó viejo al
  añadir el quinto modo.
- Que en el mástil **cada nota caiga siempre en el mismo traste**, y que sea el
  más bajo posible.
- Que las barras del xilófono no se pisen y se vayan acortando al subir.
- Que el círculo lleve bien **la cuenta de cuántas notas de cada clase están
  sonando**: si suenan dos Do y se suelta uno, el nodo tiene que seguir
  encendido. Es el fallo fácil de ese instrumento.
- Que el reparto de canales **no pierda nada que un teclado solo sí mostraría**,
  y que los instrumentos apilados no se pisen.

Lo que **no** cubren, porque no se puede automatizar sin pantalla ni oído: que
suene, que el ratón y el teclado respondan como esperas, y que vaya fluido.

## 🖱️ Acceso en el escritorio

```powershell
.\crear-acceso.ps1
```

Deja un **Piano MIDI** en el escritorio con su icono, que lo dibuja el propio
programa (`Iconos`) para que sea el mismo que el de la ventana.

El acceso llama a `lanzar.ps1`, que recompila **solo si has tocado el codigo** y
abre la ventana sin consola. Si algo falla avisa con un cuadro de dialogo, que si
no arranca sin consola y no se veria el error.

## 📷 Capturas

El botón **Captura**, o `Ctrl+G`, guarda lo que se ve en `capturas/` como PNG.

## 👥 Autores

Proyecto de grupo. *(Pendiente de completar con los nombres.)*
