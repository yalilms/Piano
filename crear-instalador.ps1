# Empaqueta el piano para poder pasarlo a alguien que no tenga Java.
#
# Deja dos cosas en dist\:
#   Piano MIDI\Piano MIDI.exe   la aplicacion, con su propio Java dentro
#   Piano-MIDI.zip              lo mismo comprimido, para mandarlo
#
# Quien lo reciba descomprime y hace doble clic. No hay que instalar nada, ni
# hacen falta permisos de administrador, ni tener Java.
#
# Un instalador .msi de verdad haria falta WiX Toolset, que pide administrador y
# .NET Framework 3.5. Si algun dia se quiere:
#     winget install WiXToolset.WiXToolset      (en una consola de administrador)
# y luego cambiar $tipo a 'msi' aqui abajo.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$tipo = 'app-image'
$version = '1.0'
$nombre = 'Piano MIDI'
$flatlaf = '3.7.2'
$principal = 'jd.piano.programa.ProgramaSwing'

# ---- JDK ----
$jdk = $null
if (Get-Command javac -ErrorAction SilentlyContinue) {
    $jdk = Split-Path (Split-Path (Get-Command javac).Source)
} else {
    $encontrado = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($encontrado) { $jdk = $encontrado.FullName }
}
if (-not $jdk) {
    throw "No encuentro ningun JDK. Instala uno con:`nwinget install EclipseAdoptium.Temurin.21.JDK"
}
$javac = Join-Path $jdk 'bin\javac.exe'
$jar = Join-Path $jdk 'bin\jar.exe'
$java = Join-Path $jdk 'bin\java.exe'
$jpackage = Join-Path $jdk 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) {
    throw "Este JDK no trae jpackage. Hace falta el JDK 14 o superior."
}
Write-Host "JDK:      $jdk"

# ---- FlatLaf ----
$jarFlatlaf = "lib\flatlaf-$flatlaf.jar"
if (-not (Test-Path $jarFlatlaf)) {
    throw "Falta $jarFlatlaf. Ejecuta .\lanzar.ps1 una vez, que lo descarga."
}

# ---- Compilar ----
Write-Host 'Compilando...'
$compilado = 'build\clases'
if (Test-Path 'build') { Remove-Item 'build' -Recurse -Force }
New-Item -ItemType Directory $compilado -Force | Out-Null
$fuentes = Get-ChildItem 'src' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
$fuentes | Set-Content 'build\fuentes.txt' -Encoding ascii
& $javac -encoding UTF-8 -cp $jarFlatlaf -d $compilado '@build\fuentes.txt'
if ($LASTEXITCODE -ne 0) { throw "javac ha fallado con codigo $LASTEXITCODE" }

# ---- El icono ----
Write-Host 'Generando el icono...'
& $java -cp $compilado jd.piano.programa.Iconos
if ($LASTEXITCODE -ne 0) { throw 'no se ha podido generar piano.ico' }

# ---- Preparar lo que va dentro ----
# Todo lo que se ponga aqui acaba en la carpeta app\ del programa, que es justo
# uno de los sitios donde Recursos busca: asi el fondo y las canciones se
# encuentran aunque se ejecute desde cualquier directorio.
Write-Host 'Preparando el contenido...'
$dentro = 'build\dentro'
New-Item -ItemType Directory $dentro -Force | Out-Null

# El Class-Path del manifiesto es lo que hace que FlatLaf se cargue: jpackage
# no anade solo los demas jar al classpath.
$manifiesto = "build\manifiesto.txt"
@(
    "Main-Class: $principal"
    "Class-Path: flatlaf-$flatlaf.jar"
    "Implementation-Title: $nombre"
    "Implementation-Version: $version"
    ''
) | Set-Content $manifiesto -Encoding ascii

& $jar --create --file "$dentro\piano.jar" --manifest $manifiesto -C $compilado .
if ($LASTEXITCODE -ne 0) { throw 'no se ha podido crear piano.jar' }

Copy-Item $jarFlatlaf $dentro
Copy-Item 'madera.jpg' $dentro
Copy-Item 'canciones' "$dentro\canciones" -Recurse
Copy-Item 'README.md' $dentro

# ---- Empaquetar ----
if (Test-Path 'dist') { Remove-Item 'dist' -Recurse -Force }
New-Item -ItemType Directory 'dist' -Force | Out-Null
Write-Host "Empaquetando ($tipo)... esto tarda un poco."

$argumentos = @(
    '--type', $tipo
    '--name', $nombre
    '--app-version', $version
    '--input', $dentro
    '--main-jar', 'piano.jar'
    '--main-class', $principal
    '--icon', 'piano.ico'
    '--dest', 'dist'
    '--vendor', 'IES Hermenegildo Lanz'
    '--description', 'Visualizador de ficheros MIDI'
    # Sin --win-console: se quiere una ventana, no una consola detras

    # Sin esto jpackage mete el JDK entero y son 154 MB. jdeps dice que solo
    # hacen falta java.base y java.desktop, pero jdeps no ve lo que se carga
    # por reflexion, asi que van tambien los que FlatLaf y el MIDI pueden pedir
    # en caliente. Comprobado despues con el propio runtime empaquetado.
    '--add-modules', 'java.base,java.desktop,java.logging,java.prefs,java.xml,jdk.unsupported'
    '--jlink-options', '--strip-debug --no-header-files --no-man-pages --compress=zip-6'
)
# Los instaladores de verdad admiten accesos directos; la carpeta suelta no
if ($tipo -ne 'app-image') {
    $argumentos += @('--win-menu', '--win-shortcut', '--win-dir-chooser',
        '--win-menu-group', $nombre)
}

& $jpackage @argumentos
if ($LASTEXITCODE -ne 0) { throw "jpackage ha fallado con codigo $LASTEXITCODE" }

# ---- El zip para mandarlo ----
if ($tipo -eq 'app-image') {
    $carpeta = Join-Path 'dist' $nombre
    $zip = 'dist\Piano-MIDI.zip'
    Write-Host 'Comprimiendo...'
    # Se reintenta porque el antivirus se queda un momento con los
    # ficheros recien creados abiertos y Compress-Archive da acceso denegado
    $intentos = 0
    while ($true) {
        try {
            Compress-Archive -Path $carpeta -DestinationPath $zip -Force
            break
        } catch {
            $intentos++
            if ($intentos -ge 5) { throw }
            Write-Host "  ocupado, reintento $intentos de 5..."
            Start-Sleep -Seconds 3
        }
    }
    $mb = [math]::Round((Get-Item $zip).Length / 1MB, 1)
    $mbCarpeta = [math]::Round(((Get-ChildItem $carpeta -Recurse |
        Measure-Object Length -Sum).Sum / 1MB), 1)
    Write-Host ''
    Write-Host "Listo." -ForegroundColor Green
    Write-Host "  $carpeta\$nombre.exe   ($mbCarpeta MB)"
    Write-Host "  $zip   ($mb MB)  <- esto es lo que se manda"
} else {
    Write-Host ''
    Write-Host 'Listo.' -ForegroundColor Green
    Get-ChildItem 'dist' | ForEach-Object {
        $mb = [math]::Round($_.Length / 1MB, 1)
        Write-Host "  $($_.FullName)   ($mb MB)"
    }
}
