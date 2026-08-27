# Crea un acceso directo al piano en el escritorio.
#   Uso:  .\crear-acceso.ps1
# Se puede volver a ejecutar sin problema: rehace el acceso y el icono.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# ---- JDK, para generar el icono ----
$java = 'java'
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $jdk) {
        throw 'No encuentro ningun JDK. Instala uno con: winget install EclipseAdoptium.Temurin.21.JDK'
    }
    $java = Join-Path $jdk.FullName 'bin\java.exe'
}

# ---- El icono ----
# Lo dibuja el propio programa, asi el acceso y la ventana llevan el mismo.
if (-not (Test-Path 'out\jd\piano\programa\Iconos.class')) {
    Write-Host 'Compilando primero...'
    & '.\lanzar.ps1'
    Start-Sleep -Seconds 3
    Get-Process javaw -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
}
$icono = Join-Path $PSScriptRoot 'piano.ico'
& $java '-Djava.awt.headless=true' -cp 'out' jd.piano.programa.Iconos $icono
if ($LASTEXITCODE -ne 0) { throw 'No he podido generar el icono.' }

# ---- El acceso directo ----
$escritorio = [Environment]::GetFolderPath('Desktop')
$destino = Join-Path $escritorio 'Piano MIDI.lnk'

$shell = New-Object -ComObject WScript.Shell
$acceso = $shell.CreateShortcut($destino)
# Se apunta a lanzar.ps1 y no a java directamente para que recompile solo si
# se ha cambiado el codigo, y para que avise si algo falla.
$acceso.TargetPath = (Get-Command powershell.exe).Source
$acceso.Arguments = '-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "' +
    (Join-Path $PSScriptRoot 'lanzar.ps1') + '"'
$acceso.WorkingDirectory = $PSScriptRoot
$acceso.IconLocation = "$icono,0"
$acceso.Description = 'Piano MIDI - visualizador y teclado'
$acceso.WindowStyle = 7   # minimizado: la consola de PowerShell no molesta
$acceso.Save()

Write-Host ''
Write-Host "Acceso creado en: $destino"
Write-Host "Icono: $icono"
Write-Host ''
Write-Host 'Ya puedes lanzarlo desde el escritorio. Recompila solo si has tocado'
Write-Host 'el codigo, asi que las siguientes veces arranca del tiron.'
