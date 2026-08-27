# Compila y lanza el piano.
#   Uso:  .\ejecutar-swing.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# ---- JDK ----
$javac = 'javac'
$java = 'java'
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    $jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $jdk) {
        throw 'No encuentro ningun JDK. Instala uno con: winget install EclipseAdoptium.Temurin.21.JDK'
    }
    $javac = Join-Path $jdk.FullName 'bin\javac.exe'
    $java = Join-Path $jdk.FullName 'bin\java.exe'
    Write-Host "javac no esta en el PATH, uso el de $($jdk.FullName)"
}

# ---- FlatLaf ----
# El JAR no se versiona (ver .gitignore); si falta se baja de Maven Central y
# se comprueba su SHA-1 antes de usarlo.
$version = '3.7.2'
$jar = "lib\flatlaf-$version.jar"
if (-not (Test-Path 'lib')) { New-Item -ItemType Directory 'lib' | Out-Null }
if (-not (Test-Path $jar)) {
    $url = "https://repo1.maven.org/maven2/com/formdev/flatlaf/$version/flatlaf-$version.jar"
    Write-Host "Bajando FlatLaf $version (992 KB)..."
    Invoke-WebRequest -Uri $url -OutFile $jar -TimeoutSec 120
    $esperado = (Invoke-WebRequest -Uri "$url.sha1" -TimeoutSec 30 -UseBasicParsing).Content.Trim().Split()[0]
    $real = (Get-FileHash $jar -Algorithm SHA1).Hash.ToLower()
    if ($real -ne $esperado) {
        Remove-Item $jar
        throw "El SHA-1 de FlatLaf no coincide (esperado $esperado, real $real)"
    }
    Write-Host 'FlatLaf verificado.'
}

# ---- Compilar ----
$fuentes = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }

# -Encoding ascii a proposito: con utf8 PowerShell mete un BOM que javac rechaza.
$lista = Join-Path $env:TEMP 'fuentes-piano-swing.txt'
$fuentes | Set-Content $lista -Encoding ascii

Write-Host "Compilando $($fuentes.Count) ficheros..."
& $javac -encoding UTF-8 -cp $jar -d out "@$lista"
if ($LASTEXITCODE -ne 0) { throw "javac ha fallado con codigo $LASTEXITCODE" }

# ---- Lanzar ----
Write-Host 'Lanzando...'
& $java -cp "out;lib/*" jd.piano.programa.ProgramaSwing
