# PB Enchantments patch-notes checker (see SKILL.md next to this file).
# Fails (exit 1) when the release that is about to be tagged would ship without
# usable notes:
#   - no docs/patchnotes/<version>.md for the version in gradle.properties
#   - a missing or malformed "<!-- released: YYYY-MM-DD -->" line
#   - a missing or empty "## en" / "## pt" / "## es" block
#   - the three blocks disagreeing on how many bullets they carry
#   - a group heading (###) sitting outside any language block
param(
    [string]$NotesDir = "docs/patchnotes",
    [string]$Version,
    [switch]$All
)

$ErrorActionPreference = 'Stop'
$failures = 0
$LANGS = @('en', 'pt', 'es')

function Fail([string]$msg) {
    Write-Host "FAIL: $msg" -ForegroundColor Red
    $script:failures++
}

if (-not $Version) {
    $props = 'gradle.properties'
    if (-not (Test-Path $props)) {
        Fail "gradle.properties not found (run from the repo root, or pass -Version)"
        exit 1
    }
    $line = Get-Content $props | Where-Object { $_ -match '^\s*version\s*=' } | Select-Object -First 1
    if (-not $line) { Fail "no version= line in gradle.properties"; exit 1 }
    $Version = ($line -split '=', 2)[1].Trim()
}

# One file, checked from top to bottom. Returns nothing; reports as it goes.
function Test-NotesFile([string]$path) {
    $name = Split-Path $path -Leaf
    $text = Get-Content $path -Raw -Encoding UTF8

    if ($text -notmatch '<!--\s*released:\s*\d{4}-\d{2}-\d{2}\s*-->') {
        Fail "[$name] no '<!-- released: YYYY-MM-DD -->' line"
    }

    $current = $null
    $counts = @{}
    $sawGroup = @{}
    foreach ($raw in ($text -split "`r?`n")) {
        $line = $raw.Trim()
        if ($line -match '^##\s+([A-Za-z]{2})\s*$') {
            $code = $Matches[1].ToLower()
            if ($LANGS -notcontains $code) {
                Fail "[$name] unknown language block '## $code' (expected en, pt or es)"
                $current = $null
                continue
            }
            if ($counts.ContainsKey($code)) { Fail "[$name] '## $code' appears twice" }
            $current = $code
            $counts[$code] = 0
            $sawGroup[$code] = $false
            continue
        }
        if ($line -match '^###\s+\S') {
            if (-not $current) { Fail "[$name] group heading '$line' is outside any language block" }
            else { $sawGroup[$current] = $true }
            continue
        }
        if ($line -match '^-\s+\S' -and $current) { $counts[$current]++ }
    }

    foreach ($code in $LANGS) {
        if (-not $counts.ContainsKey($code)) {
            Fail "[$name] missing the '## $code' block"
        } elseif ($counts[$code] -eq 0) {
            Fail "[$name] the '## $code' block has no bullets"
        } elseif (-not $sawGroup[$code]) {
            Fail "[$name] the '## $code' block has no '### ' group heading"
        }
    }

    $present = $LANGS | Where-Object { $counts.ContainsKey($_) -and $counts[$_] -gt 0 }
    $distinct = $present | ForEach-Object { $counts[$_] } | Sort-Object -Unique
    if ($distinct.Count -gt 1) {
        $detail = ($present | ForEach-Object { "$_=$($counts[$_])" }) -join ', '
        Fail "[$name] the languages carry different numbers of bullets ($detail) - every bullet must exist in all three"
    }

    if ($failures -eq 0 -or $true) {
        Write-Host "$name : $(($present | ForEach-Object { "$_=$($counts[$_])" }) -join ', ') bullets"
    }
}

if ($All) {
    $files = Get-ChildItem $NotesDir -Filter '*.md' -ErrorAction SilentlyContinue
    if (-not $files) { Fail "no notes files in $NotesDir" }
    foreach ($f in $files) { Test-NotesFile $f.FullName }
} else {
    $path = Join-Path $NotesDir "$Version.md"
    if (-not (Test-Path $path)) {
        Fail "no notes for version $Version - expected $path (see .claude/skills/patch-notes/SKILL.md)"
    } else {
        Test-NotesFile $path
    }
}

if ($failures -gt 0) {
    Write-Host ""
    Write-Host "$failures problem(s) found." -ForegroundColor Red
    exit 1
}
Write-Host "Patch notes OK." -ForegroundColor Green
