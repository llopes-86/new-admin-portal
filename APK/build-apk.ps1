# =============================================================================
# build-apk.ps1 — compila o APK do Portal Admin (Admin M&L)
# =============================================================================
# Uso:
#   .\build-apk.ps1              # release assinado (vai para dist\)
#   .\build-apk.ps1 -Debug       # build debug
# =============================================================================

param(
  [switch]$Debug
)

$ErrorActionPreference = "Stop"

$ProjectDir = $PSScriptRoot
$JavaHome   = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$SdkRoot    = "$env:LOCALAPPDATA\Android\Sdk"

if (-not (Test-Path $JavaHome)) { throw "JDK 21 nao encontrado em $JavaHome" }
if (-not (Test-Path $SdkRoot))  { throw "Android SDK nao encontrado em $SdkRoot" }

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot

Push-Location $ProjectDir
try {
  $task = if ($Debug) { "assembleDebug" } else { "assembleRelease" }
  Write-Host "==> gradlew $task" -ForegroundColor Cyan
  $previous = $ErrorActionPreference
  $ErrorActionPreference = "Continue" # o gradle escreve avisos em stderr
  & "$ProjectDir\gradlew.bat" --no-daemon $task 2>&1 | ForEach-Object { "$_" }
  $code = $LASTEXITCODE
  $ErrorActionPreference = $previous
  if ($code -ne 0) { throw "Falha no build (exit $code)" }

  if (-not $Debug) {
    $out = "$ProjectDir\app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path $out)) { throw "APK de release nao encontrado em $out" }
    $dist = "$ProjectDir\dist"
    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    $version = (Select-String -Path "$ProjectDir\app\build.gradle.kts" -Pattern 'versionName = "([^"]+)"').Matches[0].Groups[1].Value
    $target = Join-Path $dist "AdminML-$version.apk"
    Copy-Item $out $target -Force
    Write-Host "==> APK: $target ($([math]::Round((Get-Item $target).Length / 1MB, 1)) MB)" -ForegroundColor Green
  }
}
finally {
  Pop-Location
}
