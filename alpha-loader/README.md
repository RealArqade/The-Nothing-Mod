# Orrin loader (Minecraft Alpha 1.0.14)

A small loader written for The Nothing Mod, for two game versions only: **Alpha 1.0.14** (the night of October 14th, 1962) and **Alpha 1.0.15** (the aftermath, the next afternoon: the same valley, changed). Fabric and Forge do not exist for Alpha, so this starts the game itself, with mods loaded first.

- `OrrinLoader`: the command line (`world`, `verify`, `find`, `launch`).
- `OrrinMain`: runs inside the game's own process, loads mods, then starts Minecraft's own `main`.
- `OrrinMod`: the interface a mod implements (a jar with an `orrin.mod` file naming the class).
- `OrrinBend`: makes the Alpha world (a real Alpha save, made by code).
- `Nbt`: just enough of the save format to write and read it back.

It contains none of Mojang's files and downloads nothing. It uses the game jar and libraries that your own launcher (or Prism) already downloaded. Build: `powershell -File tools\Build-Alpha.ps1` (Java 8 class files, since Alpha needs an old Java).

Try it: make an Alpha 1.0.14 instance in Prism with **no mod loader**, start it once, close it, then
`java -jar orrin-loader.jar find` and `java -jar orrin-loader.jar launch --version a1.0.14 --java "<path to a Java 8>"`.

## Windows, macOS and Linux

The loader works on all three. It finds the right native libraries for the system (natives-windows, natives-osx or natives-linux) and puts the world where Alpha looks on that system: `orrin-home/<version>/.minecraft` on Windows and Linux, `orrin-home/<version>/Library/Application Support/minecraft` on macOS. It looks for your launcher's files in the usual places for each system (including Prism, and Prism's Flatpak folder on Linux). Pass the path of a Java 8 with `--java`, for example `--java /path/to/java8/bin/java`. `java -jar orrin-loader.jar find` tells you which system it thinks it is on and what it found. Tested: Windows fully (the game runs). Linux (Ubuntu in WSL2): `find`, `world` and `verify` work and the world lands in `.minecraft`, but the game itself was not started there (no Linux natives in that test). macOS: only the folder layout was checked, by pretending to be a Mac. The Desktop-folder lookup in the mod was tested on Linux for a translated Desktop (XDG), the XDG variable, a plain Desktop and no Desktop.


## How a player starts it

In the mod's settings: press **Orrin's Bend (Minecraft Alpha 1.0.14 and 1.0.15)...** once and confirm what it writes. That puts the loader in `<game folder>/the-nothing-mod/orrin` with start files (`start 1.0.14.bat`, `start 1.0.14.sh`, `start 1.0.14.command`, and the same for 1.0.15) and a README.txt. Then either double-click a start file or press **Start Orrin's Bend** in the settings. The loader finds a Java 8 by itself (JAVA_HOME, Prism's java folder, the official launcher's runtime, /usr/lib/jvm, macOS's JavaVirtualMachines). The one thing the player must do first is start Alpha 1.0.14 (and/or 1.0.15) once in their launcher with no mod loader, so the launcher downloads it.


## No launcher needed

The loader does not need Prism, the official launcher or any other launcher. If the game files for the version are not in the loader's own folder (`orrin-home/<version>/game`) or in a launcher's folders, it downloads that version of Minecraft Alpha and the libraries it asks for (LWJGL, JInput and the native files for this system) straight from Mojang's own servers, checks each file against Mojang's published SHA-1, and keeps them for next time. Use `--no-download` to forbid that. It uses a Java 8 if it can find one, and otherwise the Java that is running the loader (Alpha was tested on Java 25 and starts fine).
