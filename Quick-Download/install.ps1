# PB Enchantments installer for Minecraft 26.2 (Fabric)
# Plain ASCII on purpose, so the console reads the same under any code page.

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$mcVersion = '26.2'
$mc        = Join-Path $env:APPDATA '.minecraft'

function Fail($msg) {
    Write-Host ''
    Write-Host "[ERROR] $msg" -ForegroundColor Red
    exit 1
}

Write-Host '======================================='
Write-Host '  PB Enchantments installer'
Write-Host "  Minecraft $mcVersion + Fabric"
Write-Host '======================================='
Write-Host ''

# --- Preflight ---------------------------------------------------------------
if (-not (Test-Path $mc)) {
    Fail "Minecraft folder not found at $mc. Install Minecraft (official launcher) and run it once before running this installer."
}

# The jar is found by name pattern, so updating the mod is just a matter of
# swapping the file in this folder - the installer never needs editing.
$modJar = Get-ChildItem $PSScriptRoot -Filter 'pbenchants-*.jar' -ErrorAction SilentlyContinue |
          Where-Object { $_.Name -notlike '*-sources.jar' } |
          Sort-Object Name -Descending | Select-Object -First 1
if (-not $modJar) {
    Fail 'No pbenchants-*.jar found in this folder. It has to sit next to this installer.'
}
Write-Host "      Mod found: $($modJar.Name)"
Write-Host ''

# --- 1. Locate Java ----------------------------------------------------------
Write-Host '[1/4] Looking for Java...'
$java = $null
$cmd = Get-Command java -ErrorAction SilentlyContinue
if ($cmd) { $java = $cmd.Source }

if (-not $java) {
    # The official launcher ships its own Java; go find it.
    $roots = @(
        (Join-Path $mc 'runtime'),
        'C:\Program Files (x86)\Minecraft Launcher\runtime',
        'C:\XboxGames\Minecraft Launcher\Content\runtime'
    )
    foreach ($root in $roots) {
        if (Test-Path $root) {
            $found = Get-ChildItem $root -Recurse -Filter 'java.exe' -ErrorAction SilentlyContinue |
                     Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($found) { $java = $found.FullName; break }
        }
    }
}
if (-not $java) {
    Fail 'Java not found. Launch Minecraft 26.2 once from the official launcher (it downloads Java itself), or install Java from adoptium.net, then run this installer again.'
}
Write-Host "      Java found: $java"

# --- 2. Fabric Loader --------------------------------------------------------
Write-Host '[2/4] Checking Fabric Loader...'
$versionsDir = Join-Path $mc 'versions'
$hasFabric = $false
if (Test-Path $versionsDir) {
    $hasFabric = [bool](Get-ChildItem $versionsDir -Directory -ErrorAction SilentlyContinue |
                        Where-Object { $_.Name -like "fabric-loader-*-$mcVersion" })
}

if ($hasFabric) {
    Write-Host "      Fabric Loader for $mcVersion is already installed. Skipping."
} else {
    Write-Host '      Not found. Downloading the Fabric installer...'
    $meta = Invoke-RestMethod 'https://meta.fabricmc.net/v2/versions/installer'
    $instUrl = ($meta | Where-Object { $_.stable } | Select-Object -First 1).url
    if (-not $instUrl) { Fail 'Could not reach the Fabric installer (meta.fabricmc.net).' }

    $instJar = Join-Path $env:TEMP 'fabric-installer.jar'
    Invoke-WebRequest $instUrl -OutFile $instJar

    Write-Host '      Installing Fabric Loader (this creates the launcher profile)...'
    & $java -jar $instJar client -dir $mc -mcversion $mcVersion
    if ($LASTEXITCODE -ne 0) { Fail 'The Fabric installer exited with an error. See the messages above.' }
    Remove-Item $instJar -ErrorAction SilentlyContinue
    Write-Host '      Fabric Loader installed.'
}

# --- 3. Fabric API -----------------------------------------------------------
Write-Host '[3/4] Checking Fabric API...'
$mods = Join-Path $mc 'mods'
New-Item -ItemType Directory -Force -Path $mods | Out-Null

$hasApi = Get-ChildItem $mods -Filter 'fabric-api-*.jar' -ErrorAction SilentlyContinue
if ($hasApi) {
    Write-Host "      Fabric API is already in the mods folder ($($hasApi[0].Name)). Skipping."
} else {
    Write-Host '      Not found. Downloading from Modrinth...'
    $apiUrl = 'https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%22' + $mcVersion + '%22%5D&loaders=%5B%22fabric%22%5D'
    $versions = Invoke-RestMethod $apiUrl
    if (-not $versions) { Fail "No Fabric API build found for Minecraft $mcVersion on Modrinth." }
    $file = $versions[0].files | Where-Object { $_.primary } | Select-Object -First 1
    if (-not $file) { $file = $versions[0].files | Select-Object -First 1 }
    Invoke-WebRequest $file.url -OutFile (Join-Path $mods $file.filename)
    Write-Host "      Downloaded: $($file.filename)"
}

# --- 4. PB Enchantments ---------------------------------------------------------
Write-Host '[4/4] Installing the PB Enchantments mod...'

# Two copies of the same mod in the folder = the game crashes on launch
# (duplicate mod id). So the old one leaves before the new one arrives.
$previous = Get-ChildItem $mods -Filter 'pbenchants-*.jar' -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -ne $modJar.Name }
foreach ($old in $previous) {
    Write-Host "      Removing the previous version: $($old.Name)"
    Remove-Item $old.FullName -Force
}

Copy-Item $modJar.FullName $mods -Force
Write-Host "      Installed: $($modJar.Name)"

# --- Heads-up about other mods -----------------------------------------------
$others = Get-ChildItem $mods -Filter '*.jar' -ErrorAction SilentlyContinue |
          Where-Object { $_.Name -notlike 'fabric-api-*' -and $_.Name -notlike 'pbenchants-*' }
if ($others) {
    Write-Host ''
    Write-Host '[NOTE] There are other mods in your mods folder:' -ForegroundColor Yellow
    $others | ForEach-Object { Write-Host "       - $($_.Name)" -ForegroundColor Yellow }
    Write-Host "       If any of them is not compatible with Minecraft $mcVersion, the game may crash on launch." -ForegroundColor Yellow
}

Write-Host ''
Write-Host '======================================='
Write-Host '  All set!'  -ForegroundColor Green
Write-Host '======================================='
Write-Host ''
Write-Host 'Now just:'
Write-Host '  1. Open the Minecraft launcher;'
Write-Host "  2. Pick the ""fabric-loader-$mcVersion"" profile;"
Write-Host '  3. Hit Play.'
Write-Host ''
Write-Host 'In game, press K to open the skill trees.'
Write-Host ''
