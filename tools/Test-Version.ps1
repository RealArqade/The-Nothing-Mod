# Smoke-tests one Minecraft version on one loader.
#   powershell -File tools\Test-Version.ps1 -Version 26.2 -Loader neoforge -Mode server   # dev server self-test (every dimension and place)
#   powershell -File tools\Test-Version.ps1 -Version 26.2 -Loader neoforge -Mode client   # starts the game until the main menu
# Prints RESULT and the interesting log lines, then stops the game. It opens a game window in client mode.
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][ValidateSet('fabric', 'neoforge', 'forge')][string]$Loader,
    [ValidateSet('server', 'client')][string]$Mode = 'server',
    [int]$MaxSeconds = 600
)
$root = Split-Path -Parent $PSScriptRoot
$v = (Get-Content (Join-Path $root 'versions\versions.json') -Raw | ConvertFrom-Json).$Version
if (-not $v) { Write-Error "Unknown version $Version"; exit 1 }
$prefix = @{ fabric = ':'; neoforge = ':neoforge:'; forge = ':forge:' }
$runDir = if ($Loader -eq 'fabric') { $root } else { Join-Path $root $Loader }
$log = Join-Path $runDir 'run\logs\latest.log'
$props = @("-Pminecraft_version=$($v.minecraft_version)", "-Pminecraft_range=$($v.minecraft_range)", "-Ploader_version=$($v.loader_version)", "-Pfabric_api_version=$($v.fabric_api_version)",
    "-Pyacl_version=$($v.yacl_version)", "-Pmodmenu_version=$($v.modmenu_version)", "-Pneoforge_version=$($v.neoforge_version)",
    "-Pyacl_neoforge_version=$($v.yacl_neoforge_version)", "-Pforge_version=$($v.forge_version)", "-Pcompat=$($v.compat)", "-Pdrop_attributes=$($v.drop_attributes)", "-Pforge_spec=$($v.forge_spec)", "-Pjava_version=$($v.java)", "-Ploom_suffix=$($v.loom_suffix)", "-Psource_replacements=$($v.source_replacements)")
if ($Loader -ne 'fabric') { $props += '-Pno_fabric=true' }
$task = "$($prefix[$Loader])$(if ($Mode -eq 'server') { 'runServer' } else { 'runClient' })"
if ($Mode -eq 'server') {
    New-Item -ItemType Directory -Force (Join-Path $runDir 'run') | Out-Null
    Set-Content (Join-Path $runDir 'run\eula.txt') 'eula=true' -Encoding ascii
    # each Minecraft version gets its own test world: a world saved by a newer version cannot be opened by an older one
    $sp = Join-Path $runDir 'run\server.properties'
    $lines = if (Test-Path $sp) { @(Get-Content $sp | Where-Object { $_ -notmatch '^(level-name|max-tick-time)=' }) } else { @() }
    Set-Content $sp ($lines + "level-name=world-$Version", "max-tick-time=-1") -Encoding ascii
    $env:THENOTHING_SELFTEST = '1'
}
Remove-Item $log -ErrorAction SilentlyContinue
$p = Start-Process -FilePath (Join-Path $root 'gradlew.bat') -ArgumentList (@($task, '--console=plain', '--no-configuration-cache') + $props) -WorkingDirectory $root -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput "$env:TEMP\test-version-out.txt" -RedirectStandardError "$env:TEMP\test-version-err.txt"
$deadline = (Get-Date).AddSeconds($MaxSeconds); $result = 'timeout'
while ((Get-Date) -lt $deadline) {
    Start-Sleep 3
    $t = if (Test-Path $log) { Get-Content $log -Raw -ErrorAction SilentlyContinue } else { '' }
    if ($Mode -eq 'server' -and $t -match 'SELFTEST done') { $result = 'selftest-done'; Start-Sleep 3; break }
    if ($Mode -eq 'client' -and $t -match 'Sound engine started|Narrator library') { $result = 'reached-menu'; Start-Sleep 8; break }
    if ($t -match 'FATAL|Failed to start|Crash report saved|Exception in thread "main"') { $result = 'failed'; break }
    if ($p.HasExited) { $result = 'exited'; break }
}
$t = if (Test-Path $log) { Get-Content $log -Raw } else { '' }
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'minecraft|forge|neoforge|fabric' -and $_.CommandLine -notmatch 'GradleDaemon|gradle-launcher|GradleWrapperMain' } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
Write-Host "RESULT: $result  ($Loader on $Version, $Mode)"
($t -split "`n" | Select-String 'SELFTEST done|did not load|the-nothing-mod.*(ERROR|FATAL)|the_nothing_mod.*(ERROR|FATAL)|FATAL|Missing textures|Unknown element|Failed to parse atlas|Exception caught|LoadingFailed' | Select-Object -First 20) -join "`n"
