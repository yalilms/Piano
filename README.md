# Piano MIDI 🎹

Aplicación de piano MIDI desarrollada en Java.

## 📋 Descripción

Proyecto académico grupal realizado para el Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM) en el IES Hermenegildo Lanz (Granada).

La aplicación simula un piano virtual que permite reproducir notas musicales mediante la interfaz MIDI de Java.

## ⚙️ Requisitos

- **JDK 21 o superior.** El código usa `List.getFirst()`, `instanceof` con patrón
  y bloques de texto. El proyecto está configurado para JDK 23.
- **`ConsolaDAW.jar`** (librería de clase, paquete `bpc.daw.consola`) copiado en
  `lib/`. No viene en el repositorio: ver [lib/LEEME.md](lib/LEEME.md).
- Un dispositivo MIDI disponible en el sistema (el sintetizador por defecto de
  Java sirve).

## ▶️ Ejecución

Con IntelliJ IDEA: abre el proyecto, comprueba que la librería `ConsolaDAW`
resuelve a `lib/ConsolaDAW.jar` y lanza `jd.piano.programa.Programa`.

Desde la línea de comandos (PowerShell, en la raíz del proyecto):

```powershell
$fuentes = "$env:TEMP\fuentes-piano.txt"
Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName } | Set-Content $fuentes
javac -cp lib\ConsolaDAW.jar -d out "@$fuentes"
java -cp "out;lib\ConsolaDAW.jar" jd.piano.programa.Programa
```

> **Importante:** hay que ejecutarlo con el directorio de trabajo en la raíz del
> proyecto. `Programa` abre `madera.jpg` y la carpeta `canciones` por ruta
> relativa, así que desde otro directorio no encontrará ni el fondo ni las
> canciones.

## 🎵 Uso

1. Elige una canción de las disponibles en `canciones/`.
2. Elige el modo de visualización:
   - **Piano Sencillo** — un teclado de 85 teclas (notas 24 a 108) donde se
     iluminan todas las notas de la canción.
   - **Multi Piano** — 16 teclados en cuadrícula 4×4, uno por canal MIDI, cada
     uno con su propio color. Usa un rango más corto (notas 48 a 71) para que
     los 16 quepan en pantalla.
3. El canal 10 (percusión) se ignora a propósito.
