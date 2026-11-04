# Builds alpha-loader/ into alpha-loader/build/orrin-loader.jar (Java 8 class files, so it runs on any Java from 8 up).
# The 2D figure code is made from alpha-loader/templates/sprites.template once per supported game version, using the scrambled names
# in alpha-loader/mappings/<version>.json (made by tools/AnalyzeAlpha.ps1), and compiled against stubs that stand in for the game's classes.
# Run:  powershell -File tools\Build-Alpha.ps1
$root = Split-Path -Parent $PSScriptRoot
$al = Join-Path $root 'alpha-loader'
$src = Join-Path $al 'src'
$out = Join-Path $al 'build'
$classes = Join-Path $out 'classes'
Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes | Out-Null

# 1. the loader itself
$files = @(Get-ChildItem $src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& javac --release 8 -encoding UTF-8 -d $classes $files
if ($LASTEXITCODE -ne 0) { throw 'javac failed (loader)' }

# 2. the 2D figure code, one copy per supported game version
$template = [IO.File]::ReadAllText((Join-Path $al 'templates\sprites.template'))
$gl = Join-Path $al 'stubs\org\lwjgl\opengl\GL11.java'
foreach ($mapFile in Get-ChildItem (Join-Path $al 'mappings') -Filter *.json) {
    $map = Get-Content $mapFile.FullName -Raw | ConvertFrom-Json
    $gen = Join-Path $al "generated\$($map.tag)"
    $tmp = Join-Path $out "classes-$($map.tag)"
    Remove-Item $gen -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force $gen, $tmp | Out-Null
    $tokens = @{
        TAG = $map.tag; ENTITY = $map.entity; WORLD = $map.world; NBT = $map.nbt; ENTITYLIST = $map.entityList; REGISTERMETHOD = $map.registerMethod
        ENTITYREAD = $map.entityRead; ENTITYWRITE = $map.entityWrite; NBTSETSTRING = $map.nbtSetString; NBTGETSTRING = $map.nbtGetString
        RENDER = $map.render; RENDERMANAGER = $map.renderManager; RENDERDRAW = $map.renderDraw; RENDERBIND = $map.renderBind
        RENDERMANAGERFIELD = $map.renderManagerField; RENDERSETMANAGER = $map.renderSetManager
        MANAGERINSTANCE = $map.managerInstance; MANAGERMAP = $map.managerMap; MANAGERYAW = $map.managerYaw
        TESS = $map.tess; TESSINSTANCE = $map.tessInstance; TESSDRAW = $map.tessDraw; TESSSTART = $map.tessStart; TESSVERTEXUV = $map.tessVertexUV
    }
    $stubClasses = @()
    $sections = [regex]::Split($template, '(?m)^//// FILE: ')
    foreach ($section in $sections | Select-Object -Skip 1) {
        $nl = $section.IndexOf("`n")
        $header = $section.Substring(0, $nl).Trim()
        $body = $section.Substring($nl + 1)
        foreach ($k in $tokens.Keys) { $header = $header.Replace("@$k@", $tokens[$k]); $body = $body.Replace("@$k@", $tokens[$k]) }
        $isStub = $header.EndsWith('(stub)')
        $fileName = $header -replace '\s*\(stub\)$', ''
        [IO.File]::WriteAllText((Join-Path $gen $fileName), $body, (New-Object Text.UTF8Encoding($false)))
        if ($isStub) { $stubClasses += ($fileName -replace '\.java$', '.class') }
    }
    $genFiles = @(Get-ChildItem $gen -Filter *.java | ForEach-Object { $_.FullName }) + $gl
    & javac --release 8 -encoding UTF-8 -d $tmp $genFiles
    if ($LASTEXITCODE -ne 0) { throw "javac failed (figures for $($map.version))" }
    # keep our classes, leave the stubs out: at run time the game's own classes of those names are used
    foreach ($stub in $stubClasses) { Remove-Item (Join-Path $tmp $stub) -Force -ErrorAction SilentlyContinue }
    Remove-Item (Join-Path $tmp 'org') -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item (Join-Path $tmp '*') $classes -Recurse -Force
    Write-Host "Figures built for $($map.version): $((Get-ChildItem $tmp -Filter *.class | Measure-Object).Count) classes"
}

Set-Content (Join-Path $out 'manifest.txt') "Main-Class: com.realarqade.orrin.OrrinLoader`n" -Encoding ascii
if (Test-Path (Join-Path $al 'resources')) { Copy-Item (Join-Path $al 'resources\*') $classes -Recurse -Force }   # the scream, the sprites, the splashes and the credits
Copy-Item (Join-Path $al 'CREDITS.md') $classes -Force
& jar cfm (Join-Path $out 'orrin-loader.jar') (Join-Path $out 'manifest.txt') -C $classes .
Get-ChildItem $out -Directory -Filter 'classes-*' | Remove-Item -Recurse -Force
New-Item -ItemType Directory -Force (Join-Path $root 'src\main\resources\orrin') | Out-Null
Copy-Item (Join-Path $out 'orrin-loader.jar') (Join-Path $root 'src\main\resources\orrin\orrin-loader.jar') -Force   # shipped inside the mod, extracted only if the player opts in
Write-Host "Built $out\orrin-loader.jar and copied it into the mod resources"
