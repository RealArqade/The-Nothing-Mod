# Installs a CLEAN production server for one loader and Minecraft version, drops the real built jar from dist/ into it and runs the self-test.
#   powershell -File tools\Test-Production.ps1 -Loader neoforge -Version 1.21.8
# This is what a player's server does (no dev environment), so it catches name-mapping problems that the dev runs cannot.
param([Parameter(Mandatory=$true)][ValidateSet('fabric','neoforge','forge')][string]$Loader, [Parameter(Mandatory=$true)][string]$Version, [int]$MaxSeconds = 420)
$root = Split-Path -Parent $PSScriptRoot
$v = (Get-Content (Join-Path $root 'versions\versions.json') -Raw | ConvertFrom-Json).$Version
function FindJava($major) { Get-ChildItem 'C:\Program Files\Eclipse Adoptium','C:\Users\ichuc\.jdks','C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "(^|-)$major[.\-]|jdk-$major|temurin-$major|jdk$major" } | Select-Object -First 1 }
$jd = FindJava ([int]$v.java); if (-not $jd) { $jd = FindJava 21 }; if (-not $jd) { $jd = FindJava 25 }; $java = $jd.FullName
$java = Join-Path $java 'bin\java.exe'
$dir = Join-Path $root "prod-test\$Loader-$Version"
if (Test-Path $dir) { Remove-Item $dir -Recurse -Force -ErrorAction SilentlyContinue }
New-Item -ItemType Directory -Force "$dir\mods" | Out-Null
$ProgressPreference = 'SilentlyContinue'
Set-Content "$dir\eula.txt" 'eula=true' -Encoding ascii
Set-Content "$dir\server.properties" "level-name=world`nmax-tick-time=-1`nonline-mode=false`nserver-port=25591`nquery.port=25591" -Encoding ascii
Copy-Item (Join-Path $root "dist\the-nothing-mod-$Loader-$Version-1.0.0.jar") "$dir\mods\"
$cmd = $null
switch ($Loader) {
  'neoforge' { $inst = "$dir\installer.jar"; Invoke-WebRequest "https://maven.neoforged.net/releases/net/neoforged/neoforge/$($v.neoforge_version)/neoforge-$($v.neoforge_version)-installer.jar" -OutFile $inst
               & $java -jar $inst --installServer $dir 2>&1 | Out-Null
               $args = (Get-ChildItem "$dir\libraries\net\neoforged\neoforge\$($v.neoforge_version)\win_args.txt").FullName
               $cmd = @("@user_jvm_args.txt", "@$args", 'nogui') }
  'forge'    { $inst = "$dir\installer.jar"; Invoke-WebRequest "https://maven.minecraftforge.net/net/minecraftforge/forge/$($v.forge_version)/forge-$($v.forge_version)-installer.jar" -OutFile $inst
               & $java -jar $inst --installServer $dir 2>&1 | Out-Null
               $a = Get-ChildItem "$dir\libraries\net\minecraftforge\forge\*\win_args.txt" -ErrorAction SilentlyContinue | Select-Object -First 1
               if ($a) { $cmd = @("@user_jvm_args.txt", "@$($a.FullName)", 'nogui') } else { $shim = Get-ChildItem "$dir\forge-*-shim.jar" | Select-Object -First 1; $cmd = @('-jar', $shim.Name, 'nogui') } }
  'fabric'   { $ld = ((Invoke-WebRequest "https://meta.fabricmc.net/v2/versions/loader/$($v.minecraft_version)" -UseBasicParsing).Content | ConvertFrom-Json)[0].loader.version
               $inst = (((Invoke-WebRequest 'https://meta.fabricmc.net/v2/versions/installer' -UseBasicParsing).Content | ConvertFrom-Json) | Where-Object { $_.stable })[0].version
               Invoke-WebRequest "https://meta.fabricmc.net/v2/versions/loader/$($v.minecraft_version)/$ld/$inst/server/jar" -OutFile "$dir\fabric-server.jar"
               Invoke-WebRequest "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$($v.fabric_api_version)/fabric-api-$($v.fabric_api_version).jar" -OutFile "$dir\mods\fabric-api.jar"
               $cmd = @('-jar', 'fabric-server.jar', 'nogui') }
}
$env:THENOTHING_SELFTEST = '1'
$out = "$dir\out.txt"; $err = "$dir\err.txt"
$p = Start-Process -FilePath $java -ArgumentList $cmd -WorkingDirectory $dir -PassThru -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err
$deadline = (Get-Date).AddSeconds($MaxSeconds); $result = 'timeout'
while ((Get-Date) -lt $deadline) {
    Start-Sleep 5
    $t = (Get-Content $out -Raw -ErrorAction SilentlyContinue) + (Get-Content "$dir\logs\latest.log" -Raw -ErrorAction SilentlyContinue)
    if ($t -match 'For help, type') { $result = 'STARTED-OK'; break }
    if ($t -match 'Mixin apply.*failed|NoSuchMethodError|NoSuchFieldError|Exception in thread "main"|LoadingFailedException|Incompatible mod|FATAL') { $result = 'FAILED'; break }
    if ($p.HasExited) { $result = 'exited'; break }
}
$t = (Get-Content "$dir\logs\latest.log" -Raw -ErrorAction SilentlyContinue)
Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match [regex]::Escape($dir) } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
Write-Host "PRODUCTION $Loader $Version : $result"
if ($t) { ($t -split "`n" | Select-String 'Mixin apply|NoSuchMethodError|NoSuchFieldError|LoadingFailed|Incompatible|Exception' | Select-Object -First 4 | ForEach-Object { '  ' + $_.Line.Substring(0, [Math]::Min(220, $_.Line.Length)) }) -join "`n" }