# Builds (or runs) the mod for one Minecraft version listed in versions/versions.json.
#   powershell -File tools\Build-Version.ps1 -Version 26.2                    # build all three loaders
#   powershell -File tools\Build-Version.ps1 -Version 26.2 -Loader neoforge   # one loader (fabric, neoforge or forge)
#   powershell -File tools\Build-Version.ps1 -Version 26.2 -Loader fabric -Run  # run that loader's client
# Passes that version's numbers to Gradle as -P overrides, so gradle.properties never has to be edited.
# Finished jars are copied to dist/ with the Minecraft version in the name.
param([Parameter(Mandatory = $true)][string]$Version, [string]$Loader = 'all', [switch]$Run)
$root = Split-Path -Parent $PSScriptRoot
$all = Get-Content (Join-Path $root 'versions\versions.json') -Raw | ConvertFrom-Json
$v = $all.$Version
if (-not $v) { Write-Error "Unknown version '$Version'. See versions/versions.json."; exit 1 }
if ($v.status -eq 'planned') { Write-Error "Version '$Version' is 'planned': there is no code for it yet. See versions/versions.json."; exit 1 }
Set-Location $root
$props = @("-Pminecraft_version=$($v.minecraft_version)", "-Pminecraft_range=$($v.minecraft_range)", "-Ploader_version=$($v.loader_version)", "-Pfabric_api_version=$($v.fabric_api_version)",
    "-Pyacl_version=$($v.yacl_version)", "-Pmodmenu_version=$($v.modmenu_version)", "-Pneoforge_version=$($v.neoforge_version)",
    "-Pyacl_neoforge_version=$($v.yacl_neoforge_version)", "-Pforge_version=$($v.forge_version)", "-Pcompat=$($v.compat)", "-Pdrop_attributes=$($v.drop_attributes)", "-Pforge_spec=$($v.forge_spec)", "-Pjava_version=$($v.java)", "-Ploom_suffix=$($v.loom_suffix)", "-Psource_replacements=$($v.source_replacements)", "-Pdim_format=$($v.dim_format)", "-Ppack_format=$($v.pack_format)", "-Pitem_models=$($v.item_models)", "-Pdata_paths=$($v.data_paths)", "-Pbundle_yacl=$($v.bundle_yacl)", "-Pmods_toml_name=$($v.mods_toml_name)")
if ($Loader -ne 'fabric') { $props += '-Pno_fabric=true' }
$prefix = @{ fabric = ':'; neoforge = ':neoforge:'; forge = ':forge:' }
$loaders = if ($Loader -eq 'all') { @('fabric', 'neoforge', 'forge') } else { @($Loader) }
foreach ($l in $loaders) {
    $task = if ($Run) { "$($prefix[$l])runClient" } else { "$($prefix[$l])build" }
    # output goes to a file, not a pipe: a Gradle daemon started by this build would otherwise keep a pipe open and hang the caller
    $logFile = Join-Path $env:TEMP "build-version-$l.log"
    $useF6 = ($l -eq 'forge' -and $v.forge_gradle -eq '6')
    if ($useF6) { $task = 'build'; $env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-21*' | Select-Object -First 1).FullName }
    $gw = if ($useF6) { Join-Path $root 'forge6\gradlew.bat' } else { Join-Path $root 'gradlew.bat' }
    $wd = if ($useF6) { Join-Path $root 'forge6' } else { $root }
    $proc = Start-Process -FilePath $gw -ArgumentList ((@($task, '--no-configuration-cache') + $props | ForEach-Object { '"' + ($_ -replace '"', '\"') + '"' }) -join ' ') -WorkingDirectory $wd -NoNewWindow -PassThru -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err"
    $null = $proc.Handle
    $proc.WaitForExit()
    Get-Content $logFile, "$logFile.err" -ErrorAction SilentlyContinue | Write-Host
    $global:LASTEXITCODE = $proc.ExitCode
    if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: $l on $Version"; exit $LASTEXITCODE }
    if (-not $Run) {
        New-Item -ItemType Directory -Force (Join-Path $root 'dist') | Out-Null
        $src = switch ($l) { 'fabric' { 'build\libs\the-nothing-mod-1.0.0.jar' } 'neoforge' { 'neoforge\build\libs\the-nothing-mod-neoforge-1.0.0.jar' } 'forge' { if ($useF6) { 'forge6\build\libs\the-nothing-mod-forge-1.0.0.jar' } else { 'forge\build\libs\the-nothing-mod-forge-1.0.0.jar' } } }
        Copy-Item (Join-Path $root $src) (Join-Path $root "dist\the-nothing-mod-$l-$Version-1.0.0.jar") -Force
        Write-Host "OK: $l on $Version -> dist\the-nothing-mod-$l-$Version-1.0.0.jar"
    }
}
