# Lanza el piano. Recompila solo si has tocado el codigo, para que arranque
# rapido cuando no ha cambiado nada.
#
# Es lo que ejecuta el acceso directo del escritorio. Si algo falla se avisa
# con una ventana, porque el acceso arranca sin consola y si no no se veria.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Avisar($mensaje) {
    Add-Type -AssemblyName System.Windows.Forms
    [System.Windows.Forms.MessageBox]::Show($mensaje, 'Piano MIDI',
        'OK', 'Error') | Out-Null
}

try {
    # ---- JDK ----
    $javac = 'javac'
    $javaw = 'javaw'
    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
        $jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | Select-Object -First 1
        if (-not $jdk) {
            throw "No encuentro ningun JDK.`n`nInstala uno con:`nwinget install EclipseAdoptium.Temurin.21.JDK"
        }
        $javac = Join-Path $jdk.FullName 'bin\javac.exe'
        $javaw = Join-Path $jdk.FullName 'bin\javaw.exe'
    }

    # ---- FlatLaf, si falta ----
    $version = '3.7.2'
    $jar = "lib\flatlaf-$version.jar"
    if (-not (Test-Path 'lib')) { New-Item -ItemType Directory 'lib' | Out-Null }
    if (-not (Test-Path $jar)) {
        $url = "https://repo1.maven.org/maven2/com/formdev/flatlaf/$version/flatlaf-$version.jar"
        Invoke-WebRequest -Uri $url -OutFile $jar -TimeoutSec 120
        $esperado = (Invoke-WebRequest -Uri "$url.sha1" -TimeoutSec 30 -UseBasicParsing).Content.Trim().Split()[0]
        if ((Get-FileHash $jar -Algorithm SHA1).Hash.ToLower() -ne $esperado) {
            Remove-Item $jar
            throw 'El SHA-1 de FlatLaf no coincide.'
        }
    }

    # ---- Compilar solo si hace falta ----
    $fuentes = Get-ChildItem -Recurse src -Filter *.java
    $masNueva = ($fuentes | Sort-Object LastWriteTime -Descending |
        Select-Object -First 1).LastWriteTime
    $clases = Get-ChildItem -Recurse out -Filter *.class -ErrorAction SilentlyContinue
    $compilada = if ($clases) {
        ($clases | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
    } else { [datetime]::MinValue }

    if ($masNueva -gt $compilada) {
        # -Encoding ascii a proposito: con utf8 PowerShell mete un BOM y javac lo rechaza
        $lista = Join-Path $env:TEMP 'fuentes-piano-lanzar.txt'
        $fuentes | ForEach-Object { $_.FullName } | Set-Content $lista -Encoding ascii
        & $javac -encoding UTF-8 -cp $jar -d out "@$lista"
        if ($LASTEXITCODE -ne 0) {
            throw "No he podido compilar el proyecto (javac ha devuelto $LASTEXITCODE)."
        }
    }

    # ---- Lanzar sin consola ----
    Start-Process -FilePath $javaw `
        -ArgumentList '-cp', 'out;lib/*', 'jd.piano.programa.ProgramaSwing' `
        -WorkingDirectory $PSScriptRoot
} catch {
    Avisar($_.Exception.Message)
    exit 1
}
