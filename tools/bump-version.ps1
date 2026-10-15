<#
.SYNOPSIS
  Aplica bump de versão SemVer (MAJOR.MINOR.PATCH) em todos os pontos do New-Admin-Portal.

.DESCRIPTION
  Fonte canônica: public/js/config.js (APP_VERSION).
  Espelhos atualizados: index.html, dashboard.html (?v= + meta app-version),
  manifest.json (version), sw.js (nome do CACHE).
  Scripts sem ?v= são invalidados pela troca do nome do CACHE — nenhum toque neles.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/bump-version.ps1 -Version 2.0.2
#>
param(
  [Parameter(Mandatory = $true)]
  [string]$Version
)

$ErrorActionPreference = 'Stop'

if ($Version -notmatch '^\d+\.\d+\.\d+$') {
  Write-Error "Versão inválida '$Version'. Use SemVer puro com 3 partes (ex: 2.0.2). Versões com 4 segmentos são rejeitadas."
  exit 1
}

$root = Split-Path -Parent $PSScriptRoot

$configFile = Join-Path $root 'public\js\config.js'
$configText = Get-Content -LiteralPath $configFile -Encoding utf8 -Raw
if ($configText -notmatch "APP_VERSION:\s*'([^']+)'") {
  Write-Error "APP_VERSION não encontrado em $configFile"
  exit 1
}
$old = $Matches[1]
if ($old -eq $Version) {
  Write-Error "Versão atual já é $Version. Nada a fazer."
  exit 1
}
$oldEsc = [regex]::Escape($old)

$targets = @(
  @{ File = 'public\index.html';     Replacements = @("?v=$old", "app-version`" content=`"$old") }
  @{ File = 'public\dashboard.html'; Replacements = @("?v=$old", "app-version`" content=`"$old") }
  @{ File = 'public\manifest.json';  Replacements = @("`"version`": `"$old") }
  @{ File = 'public\sw.js';          Replacements = @("bodas-admin-v$old") }
  @{ File = 'public\js\config.js';   Replacements = @("APP_VERSION: '$old") }
)

$newTokens = @{
  "?v=$old"                        = "?v=$Version"
  "app-version`" content=`"$old"   = "app-version`" content=`"$Version"
  "`"version`": `"$old"            = "`"version`": `"$Version"
  "bodas-admin-v$old"              = "bodas-admin-v$Version"
  "APP_VERSION: '$old"             = "APP_VERSION: '$Version"
}

$changedTotal = 0
foreach ($t in $targets) {
  $path = Join-Path $root $t.File
  $text = Get-Content -LiteralPath $path -Encoding utf8 -Raw
  $fileChanged = 0
  foreach ($token in $t.Replacements) {
    $count = ([regex]::Matches($text, [regex]::Escape($token))).Count
    if ($count -gt 0) {
      $text = $text.Replace($token, $newTokens[$token])
      $fileChanged += $count
    }
  }
  if ($fileChanged -gt 0) {
    # UTF-8 sem BOM (igual ao original dos arquivos do projeto)
    [System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding $false))
  }
  Write-Output "$($t.File): $fileChanged ponto(s)"
  $changedTotal += $fileChanged
}

$leftover = 0
foreach ($t in $targets) {
  $path = Join-Path $root $t.File
  $leftover += ([regex]::Matches((Get-Content -LiteralPath $path -Encoding utf8 -Raw), $oldEsc)).Count
}
$newCount = 0
foreach ($t in $targets) {
  $path = Join-Path $root $t.File
  $newCount += ([regex]::Matches((Get-Content -LiteralPath $path -Encoding utf8 -Raw), [regex]::Escape($Version))).Count
}

Write-Output "---"
Write-Output "Antiga ($old): $changedTotal ponto(s) atualizado(s), restantes: $leftover"
Write-Output "Nova ($Version): $newCount ocorrência(s)"

if ($leftover -ne 0) {
  Write-Error "FALHA: restaram $leftover ocorrência(s) de $old"
  exit 1
}
if ($changedTotal -ne 18) {
  Write-Warning "ATENÇÃO: esperado 18 pontos, atualizados $changedTotal. Revise se algum ponto novo foi adicionado ao projeto."
}
Write-Output "BUMP OK: $old -> $Version"
