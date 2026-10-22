<#
.SYNOPSIS
  Versiona o app Android (Admin M&L) com SemVer amarrado ao git.

.DESCRIPTION
  A fonte da verdade é a TAG `apk/vX.Y.Z` do repositório New-Admin-Portal.
  O Gradle lê o git no momento do build e deriva:
    - versionName: 1.2.3 (tag em HEAD, árvore limpa) | 1.2.3-devN (N commits à frente) | 1.2.3-devN.dirty
    - versionCode: número de commits do repositório (monótono e único por build)
  Este script cria/atualiza a tag de release, valida a SemVer e registra a
  entrada no CHANGELOG do app.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/bump-app-version.ps1 -Version 1.1.0
  powershell -ExecutionPolicy Bypass -File tools/bump-app-version.ps1 -Version 1.1.0 -Push
#>
param(
  [Parameter(Mandatory = $true)]
  [string]$Version,

  [switch]$Push,

  [string]$Note = ""
)

$ErrorActionPreference = 'Stop'

# SemVer puro de 3 partes (mesma regra do bump-version.ps1 do portal web)
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
  Write-Error "Versão inválida '$Version'. Use SemVer puro com 3 partes (ex: 1.1.0)."
  exit 1
}
foreach ($part in $Version.Split('.')) {
  if ($part.Length -gt 1 -and $part.StartsWith('0')) {
    Write-Error "Versão inválida '$Version'. SemVer não aceita zeros à esquerda."
    exit 1
  }
}

$root = Split-Path -Parent $PSScriptRoot
$apkDir = Join-Path $root 'APK'
$tag = "apk/v$Version"
$changelog = Join-Path $apkDir 'CHANGELOG.md'

Push-Location $root
# git escreve em stderr (ex.: "No names found") e o PowerShell 5.1 transformaria
# isso em erro fatal — aqui stderr do git é apenas informativo.
$ErrorActionPreference = 'Continue'
try {
  # ---------------------------------------------------------------- git
  if (-not (Test-Path (Join-Path $root '.git'))) {
    Write-Error "Não é um repositório git: $root"
    exit 1
  }

  $status = git status --porcelain
  if ($status) {
    Write-Warning "Há alterações não commitadas — a tag aponta para o commit atual:"
    $status | Select-Object -First 10 | ForEach-Object { Write-Warning "  $_" }
  }

  $previous = git describe --tags --match "apk/v*" --abbrev=0 2>$null
  if ($previous) { Write-Output "Versão anterior do app: $previous" }

  # ------------------------------------------------------- changelog
  if (-not (Test-Path $changelog)) {
    $header = @"
# Changelog — Admin M&L (app Android)

Todas as mudanças do app Android. O versionamento segue SemVer e é controlado
pelas tags ``apk/vX.Y.Z`` deste repositório.

"@
    [System.IO.File]::WriteAllText($changelog, $header, (New-Object System.Text.UTF8Encoding $false))
  }

  $text = Get-Content -LiteralPath $changelog -Encoding utf8 -Raw
  $today = Get-Date -Format 'yyyy-MM-dd'
  $summary = if ($Note) { $Note } else { '(resumo pendente)' }
  $entry = "## $Version - $today`n`n$summary`n`n"

  if ($text -match "(?m)^## $([regex]::Escape($Version)) ") {
    Write-Output "CHANGELOG já possui a entrada $Version (mantida)."
  } else {
    # Insere a nova entrada logo após o parágrafo de cabeçalho
    $lines = $text -split "`r?`n"
    $insertAt = 0
    for ($i = 0; $i -lt $lines.Count; $i++) {
      if ($lines[$i] -match 'tags ``apk/vX\.Y\.Z``') { $insertAt = $i + 1; break }
    }
    $newText = (($lines[0..$insertAt] + ($entry -split "`r?`n") + $lines[($insertAt + 1)..($lines.Count - 1)]) -join "`n")
    [System.IO.File]::WriteAllText($changelog, $newText, (New-Object System.Text.UTF8Encoding $false))
  }

  # -------------------------------------------------------------- tag
  $existing = git tag -l $tag
  if ($existing) {
    Write-Output "Tag $tag já existe (mantida)."
  } else {
    git tag -a $tag -m "Admin M&L $Version"
    Write-Output "Tag criada: $tag"
  }

  if ($Push) {
    git push origin master
    git push origin $tag
    Write-Output "Tag enviada para o remoto."
  }

  Write-Output "---"
  Write-Output "Versão do app: $Version (tag $tag)"
  Write-Output "CHANGELOG atualizado: $changelog"
  Write-Output ""
  Write-Output "Próximo passo:"
  Write-Output "  cd APK; .\build-apk.ps1"
  Write-Output "  (o Gradle lê a tag: versionName=$Version, versionCode=<nº de commits>)"
}
finally {
  Pop-Location
}
