# Rebuilds each jar on its own and tests it on a clean production server. One result line per jar goes to verify-results.txt.
#   powershell -File tools\Verify-Publish.ps1 -Versions 26.3,1.21.11 -Loaders neoforge,forge,fabric
param([Parameter(Mandatory=$true)][string[]]$Versions, [string[]]$Loaders = @('neoforge','forge','fabric'))
$root = Split-Path -Parent $PSScriptRoot; Set-Location $root
$Versions = @($Versions | ForEach-Object { $_ -split ',' } | Where-Object { $_ }); $Loaders = @($Loaders | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
$res = Join-Path $root 'verify-results.txt'
$all = Get-Content (Join-Path $root 'versions\versions.json') -Raw | ConvertFrom-Json
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($v in $Versions) { foreach ($l in $Loaders) {
    $b = @(powershell -File tools\Build-Version.ps1 -Version $v -Loader $l *>&1 | ForEach-Object { "$_" })
    $built = ($b | Select-String '^OK:|> OK:')
    $line = "$l $v : build=" + $(if ($built) { 'ok' } else { 'FAIL' })
    if ($built) {
        $jar = Join-Path $root "dist\the-nothing-mod-$l-$v-1.0.0.jar"
        if ($l -eq 'fabric' -and $all.$v.loom_suffix -eq '-remap') {
            $z = [IO.Compression.ZipFile]::OpenRead($jar); $ref = ($z.Entries | Where-Object { $_.FullName -match 'refmap' }).Count; $z.Dispose(); $line += " refmaps=$ref"
        }
        $p = @(powershell -File tools\Test-Production.ps1 -Loader $l -Version $v *>&1 | ForEach-Object { "$_" })
        $line += ' | ' + (($p | Select-String 'PRODUCTION').Line)
        if ($p -match 'FAILED|exited|timeout') { $line += ' :: ' + (($p | Select-Object -Skip 1 -First 2) -join ' ') }
    }
    Add-Content $res $line
    Write-Host $line
} }