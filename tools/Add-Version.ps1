# Adds a version entry to versions/versions.json (copied from another entry with changed numbers) and copies its compat folder.
#   powershell -File tools\Add-Version.ps1 -Version 1.21.4 -CopyFrom 1.21.5 -Neo 21.4.161 -Forge 1.21.4-54.1.22 -Spec 54 -Fapi 0.119.4+1.21.4 -Yacl 3.8.1+1.21.4 -Modmenu 13.0.4 -Pack 61 [-Java 21]
param([Parameter(Mandatory=$true)][string]$Version,[Parameter(Mandatory=$true)][string]$CopyFrom,[string]$Neo,[string]$Forge,[string]$Spec,[string]$Fapi,[string]$Yacl,[string]$Modmenu,[string]$Pack,[string]$Java,[string]$Mc)
$root = Split-Path -Parent $PSScriptRoot; $enc = New-Object Text.UTF8Encoding $false
$vp = Join-Path $root 'versions\versions.json'
$all = Get-Content $vp -Raw | ConvertFrom-Json
$src = $all.$CopyFrom | ConvertTo-Json -Depth 5 | ConvertFrom-Json
if (-not $Mc) { $Mc = $Version }
$src.minecraft_version = $Mc; $src.minecraft_range = "~$Mc"; $src.compat = $Version; $src.status = 'trying'
if ($Neo) { $src.neoforge_version = $Neo }; if ($Forge) { $src.forge_version = $Forge }; if ($Spec) { $src.forge_spec = $Spec }
if ($Fapi) { $src.fabric_api_version = $Fapi }
if ($Yacl) { $src.yacl_version = "$Yacl-fabric"; $src.yacl_neoforge_version = "$Yacl-neoforge" }
if ($Modmenu) { $src.modmenu_version = $Modmenu }; if ($Pack) { $src.pack_format = $Pack }; if ($Java) { $src.java = [int]$Java }
$all | Add-Member -NotePropertyName $Version -NotePropertyValue $src -Force
# keep file order roughly stable: write with the new entry
[IO.File]::WriteAllText($vp, ($all | ConvertTo-Json -Depth 6), $enc)
$dst = Join-Path $root "compat\$Version"; if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
Copy-Item (Join-Path $root "compat\$CopyFrom") $dst -Recurse
Write-Host "added $Version (from $CopyFrom)"