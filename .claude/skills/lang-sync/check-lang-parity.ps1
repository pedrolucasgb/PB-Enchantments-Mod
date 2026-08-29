# Tool Mastery lang parity checker (see SKILL.md next to this file).
# Fails (exit 1) when a locale drifts from en_us.json:
#   - missing keys / extra keys
#   - a value whose %s / %n$s placeholder multiset differs from English
#   - es_mx.json not byte-identical to es_es.json
param(
    [string]$LangDir = "src/main/resources/assets/toolmastery/lang"
)

$ErrorActionPreference = 'Stop'
$failures = 0

function Get-Placeholders([string]$value) {
    $found = [regex]::Matches($value, '%(?:\d+\$)?s') | ForEach-Object { $_.Value }
    return ($found | Sort-Object) -join ','
}

$sourcePath = Join-Path $LangDir 'en_us.json'
if (-not (Test-Path $sourcePath)) {
    Write-Host "FAIL: $sourcePath not found (run from the repo root)" -ForegroundColor Red
    exit 1
}
$source = Get-Content $sourcePath -Raw -Encoding UTF8 | ConvertFrom-Json
$sourceKeys = $source.PSObject.Properties.Name

foreach ($locale in @('pt_br', 'es_es')) {
    $path = Join-Path $LangDir "$locale.json"
    if (-not (Test-Path $path)) {
        Write-Host "FAIL: $path is missing" -ForegroundColor Red
        $failures++
        continue
    }
    $translated = Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json
    $keys = $translated.PSObject.Properties.Name

    foreach ($key in $sourceKeys) {
        if ($keys -notcontains $key) {
            Write-Host "FAIL [$locale]: missing key $key" -ForegroundColor Red
            $failures++
        } elseif ((Get-Placeholders $source.$key) -ne (Get-Placeholders $translated.$key)) {
            Write-Host "FAIL [$locale]: placeholder mismatch on $key" -ForegroundColor Red
            Write-Host "  en: $($source.$key)"
            Write-Host "  ${locale}: $($translated.$key)"
            $failures++
        }
    }
    foreach ($key in $keys) {
        if ($sourceKeys -notcontains $key) {
            Write-Host "FAIL [$locale]: extra key not in en_us: $key" -ForegroundColor Red
            $failures++
        }
    }
    Write-Host "$locale.json: $($keys.Count) keys checked against $($sourceKeys.Count) in en_us.json"
}

$esEs = Join-Path $LangDir 'es_es.json'
$esMx = Join-Path $LangDir 'es_mx.json'
if (-not (Test-Path $esMx)) {
    Write-Host "FAIL: es_mx.json is missing (copy of es_es.json)" -ForegroundColor Red
    $failures++
} elseif ((Get-FileHash $esEs).Hash -ne (Get-FileHash $esMx).Hash) {
    Write-Host "FAIL: es_mx.json differs from es_es.json - refresh the copy" -ForegroundColor Red
    $failures++
}

if ($failures -gt 0) {
    Write-Host "$failures problem(s) found." -ForegroundColor Red
    exit 1
}
Write-Host "All locales in sync." -ForegroundColor Green
exit 0
