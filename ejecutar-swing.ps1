# Compila y lanza el piano.
#   Uso:  .\ejecutar-swing.ps1
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
    Write-Host "javac no esta en el PATH, uso el de $($jdk.FullName)"
}

$fuentes = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }

# -Encoding ascii a proposito: con utf8 PowerShell mete un BOM que javac rechaza.
$lista = Join-Path $env:TEMP 'fuentes-piano-swing.txt'
$fuentes | Set-Content $lista -Encoding ascii

Write-Host "Compilando $($fuentes.Count) ficheros..."
& $javac -encoding UTF-8 -d out "@$lista"
if ($LASTEXITCODE -ne 0) { throw "javac ha fallado con codigo $LASTEXITCODE" }

Write-Host "Lanzando ProgramaSwing..."
& $java -cp out jd.piano.programa.ProgramaSwing
