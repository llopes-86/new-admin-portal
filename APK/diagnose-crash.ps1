# =============================================================================
# diagnose-crash.ps1 — instala/abre o app e coleta o log de crash
# =============================================================================
# Uso:
#   .\diagnose-crash.ps1                       # usa o dispositivo USB conectado
#   .\diagnose-crash.ps1 -Wireless "192.168.0.10:5555"   # depuracao sem fio
#
# O que faz:
#   1. conecta via adb (usb ou wifi)
#   2. instala o APK de release
#   3. limpa o logcat, abre o app e espera
#   4. imprime o log do app (tag AdminML) e o stack trace do crash
# =============================================================================

param(
  [string]$Wireless = "",
  [switch]$SkipInstall
)

$ErrorActionPreference = "Continue"
$ProjectDir = $PSScriptRoot
$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$Apk = Join-Path $ProjectDir "dist\AdminML-1.0.apk"
$Package = "com.bodasmel.adminml"

if (-not (Test-Path $Apk)) { throw "APK nao encontrado: $Apk" }

Write-Host "==> conectando no dispositivo" -ForegroundColor Cyan
if ($Wireless -ne "") {
  & $Adb connect $Wireless
  & $Adb devices
} else {
  & $Adb devices
}

$devices = & $Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
if (-not $devices) {
  Write-Host "Nenhum dispositivo conectado." -ForegroundColor Red
  Write-Host "Conecte o celular por USB (com depuracao USB ativada) OU use:"
  Write-Host '  .\diagnose-crash.ps1 -Wireless "IP:PORTA"   (Depuracao sem fio nas Developer Options)'
  exit 1
}

if (-not $SkipInstall) {
  Write-Host "==> instalando o APK" -ForegroundColor Cyan
  & $Adb install -r $Apk
}

Write-Host "==> limpando logcat e abrindo o app" -ForegroundColor Cyan
& $Adb logcat -c
& $Adb shell am force-stop $Package
& $Adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 6

Write-Host "`n===== LOG DO APP (tag AdminML) =====" -ForegroundColor Green
& $Adb logcat -d -s AdminML AndroidRuntime:E ActivityManager:E *:S

Write-Host "`n===== CRASHES RECENTES =====" -ForegroundColor Green
& $Adb logcat -d -b crash | Select-Object -Last 80

Write-Host "`n===== ESTADO DO PROCESSO =====" -ForegroundColor Green
& $Adb shell pidof $Package
& $Adb shell dumpsys activity activities | Select-String -Pattern "mResumedActivity|topResumedActivity" | Select-Object -First 3
