# Compila y lanza las comprobaciones automaticas.
#   Uso:  .\probar.ps1
# Devuelve codigo 0 si todo va bien, 1 si algo falla.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

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
}

$fuentes = @()
$fuentes += Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
$fuentes += Get-ChildItem -Recurse test -Filter *.java | ForEach-Object { $_.FullName }

# -Encoding ascii a proposito: con utf8 PowerShell mete un BOM y javac lo rechaza
$lista = Join-Path $env:TEMP 'fuentes-piano-pruebas.txt'
$fuentes | Set-Content $lista -Encoding ascii

Write-Host "Compilando $($fuentes.Count) ficheros (src + test)..."
& $javac -encoding UTF-8 -cp 'lib\flatlaf-3.7.2.jar' -d out-test "@$lista"
if ($LASTEXITCODE -ne 0) { throw "javac ha fallado con codigo $LASTEXITCODE" }

& $java "-Djava.awt.headless=true" -cp "out-test;lib/*" jd.piano.pruebas.Pruebas
exit $LASTEXITCODE
