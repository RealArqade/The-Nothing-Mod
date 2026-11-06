param([Parameter(Mandatory=$true)][string]$Version, [string[]]$Loaders=@('neoforge','forge','fabric'))
$root = Split-Path -Parent $PSScriptRoot; Set-Location $root
foreach ($l in $Loaders) {
    $b = powershell -File tools\Build-Version.ps1 -Version $Version -Loader $l 2>&1
    if (-not ($b | Select-String '^OK:|> OK:')) { "$Version $l BUILD FAIL"; $b | Select-String 'error:' | Select-Object -First 6 | ForEach-Object { '  ' + ($_.Line -replace '.*thenothingmod\\','').Trim() }; continue }
    $t = powershell -File tools\Test-Version.ps1 -Version $Version -Loader $l -Mode server 2>&1
    $r = ($t | Select-String 'SELFTEST done|RESULT').Line -join ' | '
    $m = ($r -replace '\[[^\]]*\] ','' -replace '\(the-nothing-mod\)|\[the-nothing-mod/\]:',''); "$Version $l : " + $m.Substring(0, [Math]::Min(120, $m.Length))
}
