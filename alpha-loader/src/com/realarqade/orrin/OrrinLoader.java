package com.realarqade.orrin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The Orrin loader: starts Minecraft Alpha 1.0.14 or 1.0.15 (only those; see {@link AlphaVersion}) in its own game folder, with the
 * mods from its mods folder loaded first, and makes the world it starts in. It uses the game files the official launcher (or Prism)
 * already downloaded onto this computer: nothing of Mojang's is included in the loader or in the mod, and nothing is downloaded.
 * <pre>
 *   java -jar orrin-loader.jar world  [dir] [--version a1.0.14|a1.0.15]   make the world (default dir: ./orrin-home/<version>/.minecraft/saves/World1)
 *   java -jar orrin-loader.jar verify [dir] [--version v]                 read the world back and check it
 *   java -jar orrin-loader.jar find                                       show which game files it found, and where
 *   java -jar orrin-loader.jar launch [--version v] [--java path/to/java8] [--user Name] [--jar game.jar] [--home dir]   (finds a Java 8 itself if --java is left out)
 * </pre>
 * Alpha only runs on an old Java (8). Pass its path with --java; the world and verify commands work on any Java.
 * The game runs with its "user home" set to the loader's own folder, so it can't see or touch your real worlds.
 */
public final class OrrinLoader {
    private OrrinLoader() {
    }

    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "help" : args[0];
        String version = option(args, "--version", AlphaVersion.NIGHT);
        if (!AlphaVersion.supported(version)) {
            System.err.println("placeholder text" + AlphaVersion.NIGHT + "placeholder text" + AlphaVersion.AFTER + "placeholder text" + version);
            System.exit(2);
        }
        String positional = args.length > 1 && !args[1].startsWith("--") ? args[1] : null;
        File defaultSave = defaultSave(version);
        switch (command) {
            case "world": {
                File dir = positional != null ? new File(positional) : defaultSave;
                OrrinBend.write(dir, AlphaVersion.isAftermath(version));
                System.out.println("placeholder text" + version + ": " + AlphaVersion.lore(version) + ") to " + dir.getAbsolutePath());
                break;
            }
            case "verify":
                verify(positional != null ? new File(positional) : defaultSave);
                break;
            case "find":
                find();
                break;
            case "fakecrash": {
                for (String a : args) {
                    if (a.equals("--sound")) {
                        FakeCrash.playScareSoundAndWait();
                        return;
                    }
                }
                String scarePng = option(args, "--scare-png", null);
                if (scarePng != null) {
                    javax.imageio.ImageIO.write(FakeCrash.renderScare(854, 480, Integer.parseInt(option(args, "--tick", "60"))), "png", new File(scarePng));
                    System.out.println("Wrote " + scarePng);
                    break;
                }
                String png = option(args, "--png", null);
                if (png != null) {
                    javax.imageio.ImageIO.write(FakeCrash.render(version, 854, 480), "png", new File(png));
                    System.out.println("Wrote " + png);
                } else {
                    FakeCrash.show(version);
                    Thread.sleep((FakeCrash.SECONDS_ON_SCREEN + 2) * 1000L);
                    System.exit(0);
                }
                break;
            }
            case "launch":
                launch(args);
                break;
            default:
                System.out.println("placeholder text" + AlphaVersion.NIGHT + "placeholder text" + AlphaVersion.AFTER + "placeholder text");
                System.out.println("placeholder text");
        }
    }

    /** "windows", "mac" or "linux" (anything else is treated like linux). */
    static String os() {
        String name = System.getProperty("os.name", "").toLowerCase();
        return name.contains("win") ? "windows" : name.contains("mac") || name.contains("darwin") ? "mac" : "linux";
    }

    /**
     * Where Alpha keeps its game folder for a given "home": Alpha looks in %APPDATA%\.minecraft on Windows (which the loader points at
     * the home folder), in ~/Library/Application Support/minecraft on macOS and in ~/.minecraft on Linux (both under user.home, which the
     * loader also points at the home folder).
     */
    static File gameDir(File home) {
        return os().equals("mac") ? new File(home, "Library/Application Support/minecraft") : new File(home, ".minecraft");
    }

    private static File defaultSave(String version) {
        return new File(gameDir(new File("orrin-home/" + version)), "saves/World1");
    }

    private static String option(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    // ---------------------------------------------------------------- checking the world

    private static void verify(File saveDir) throws IOException {
        Nbt.Compound level = Nbt.readGzip(new java.io.FileInputStream(new File(saveDir, "level.dat")));
        Nbt.Compound data = (Nbt.Compound) level.get("Data");
        System.out.println("placeholder text" + data.get("RandomSeed") + "placeholder text" + data.get("SpawnX") + "," + data.get("SpawnY")
                + "," + data.get("SpawnZ") + "placeholder text" + data.get("Time"));
        int chunks = 0;
        long water = 0;
        long solid = 0;
        long figures = 0;
        long torches = 0;
        File[] a = saveDir.listFiles();
        for (File x : a == null ? new File[0] : a) {
            File[] b = x.isDirectory() ? x.listFiles() : null;
            for (File z : b == null ? new File[0] : b) {
                File[] c = z.listFiles();
                for (File chunk : c == null ? new File[0] : c) {
                    Nbt.Compound root = Nbt.readGzip(new java.io.FileInputStream(chunk));
                    Nbt.Compound l = (Nbt.Compound) root.get("Level");
                    byte[] blocks = (byte[]) l.get("Blocks");
                    if (blocks.length != 32768 || ((byte[]) l.get("SkyLight")).length != 16384 || ((byte[]) l.get("HeightMap")).length != 256) {
                        throw new IOException("placeholder text" + chunk);
                    }
                    figures += OrrinBend.spriteCountIn(root);
                    for (byte id : blocks) {
                        if (id == OrrinBend.TORCH) {
                            torches++;
                        }
                        if (id == OrrinBend.WATER) {
                            water++;
                        } else if (id != 0) {
                            solid++;
                        }
                    }
                    chunks++;
                }
            }
        }
        System.out.println("placeholder text" + chunks + "placeholder text" + water + "placeholder text" + solid + "placeholder text" + torches + "placeholder text" + figures + ")");
    }

    // ---------------------------------------------------------------- finding the game files

    /** The loader's own game folder (downloaded files); searched before any launcher's. */
    private static File extraRoot;

    private static List<File> roots() {
        List<File> roots = new ArrayList<File>();
        if (extraRoot != null) {
            roots.add(extraRoot);
        }
        String appData = System.getenv("APPDATA");
        String home = System.getProperty("user.home");
        if (appData != null) {
            roots.add(new File(appData, ".minecraft"));
            roots.add(new File(appData, "PrismLauncher"));
            roots.add(new File(appData, "PolyMC"));
        }
        roots.add(new File(home, ".minecraft"));
        roots.add(new File(home, "Library/Application Support/minecraft"));
        roots.add(new File(home, "Library/Application Support/PrismLauncher"));
        roots.add(new File(home, ".local/share/PrismLauncher"));
        roots.add(new File(home, ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher")); // Flatpak
        roots.add(new File(home, ".var/app/com.mojang.Minecraft/.minecraft"));
        return roots;
    }

    private static void collect(File dir, int depth, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null || depth > 9) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, depth + 1, out);
            } else if (f.getName().endsWith(".jar")) {
                out.add(f);
            }
        }
    }

    private static List<File> allJars() {
        List<File> jars = new ArrayList<File>();
        // Only the folders where launchers keep the game and its libraries. Walking everything (instances, mods, saves) is slow
        // and can take minutes on a computer with a lot of Minecraft files.
        for (File root : roots()) {
            collect(new File(root, "libraries"), 0, jars);
            collect(new File(root, "versions"), 0, jars);
        }
        return jars;
    }

    private static File newest(List<File> jars, String contains, String mustNotContain) {
        return newest(jars, contains, mustNotContain, null);
    }

    private static File newest(List<File> jars, String contains, String mustNotContain, String alsoContains) {
        File best = null;
        for (File j : jars) {
            String path = j.getPath().replace('\\', '/');
            if (path.contains(contains) && (alsoContains == null || path.contains(alsoContains))
                    && (mustNotContain == null || !path.contains(mustNotContain))
                    && (best == null || path.compareTo(best.getPath().replace('\\', '/')) > 0)) {
                best = j;
            }
        }
        return best;
    }

    /** The player's own copy of the game for this version, or null. */
    static File gameJar(String version) {
        return findGame(allJars(), version);
    }

    private static File findGame(List<File> jars, String version) {
        for (File j : jars) {
            String n = j.getName();
            if (n.equals(version + ".jar") || n.equals("minecraft-" + version + "-client.jar")) {
                return j;
            }
        }
        return null;
    }

    private static void find() {
        List<File> jars = allJars();
        for (String v : AlphaVersion.ALL) {
            System.out.println("game " + v + ": " + findGame(jars, v));
        }
        System.out.println("lwjgl: " + newest(jars, "/lwjgl-2", "natives"));
        System.out.println("lwjgl_util: " + newest(jars, "/lwjgl_util-2", null));
        System.out.println("jinput: " + newest(jars, "/jinput-2", "natives"));
        String classifier = os().equals("windows") ? "natives-windows" : os().equals("mac") ? "natives-osx" : "natives-linux";
        System.out.println("system: " + os() + "placeholder text" + classifier + ")");
        System.out.println("placeholder text" + newest(jars, "lwjgl-platform-2", null, classifier));
        System.out.println("placeholder text" + newest(jars, "jinput-platform-2", null, classifier));
        System.out.println("placeholder text"
                + "placeholder text");
    }

    // ---------------------------------------------------------------- starting the game

    private static void launch(String[] args) throws Exception {
        String java = "java";
        String user = "Player";
        String version = option(args, "--version", AlphaVersion.NIGHT);
        File homeDir = new File("orrin-home/" + version);
        File gameJar = null;
        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].equals("--java")) {
                java = args[i + 1];
            } else if (args[i].equals("--user")) {
                user = args[i + 1];
            } else if (args[i].equals("--jar")) {
                gameJar = new File(args[i + 1]);
            } else if (args[i].equals("--home")) {
                homeDir = new File(args[i + 1]);
            }
        }
        if (!AlphaVersion.supported(version)) {
            System.err.println("placeholder text" + AlphaVersion.NIGHT + "placeholder text" + AlphaVersion.AFTER + "placeholder text" + version);
            System.exit(2);
        }
        // Game files: this loader's own folder first, then any launcher's. Anything missing is downloaded from Mojang (unless --no-download),
        // so no launcher is needed at all.
        File gameRoot = new File(homeDir, "game");
        extraRoot = gameRoot;
        List<File> jars = allJars();
        String nativesName = os().equals("windows") ? "natives-windows" : os().equals("mac") ? "natives-osx" : "natives-linux";
        boolean complete = findGame(jars, version) != null || gameJar != null;
        complete = complete && newest(jars, "/lwjgl-2", "natives") != null && newest(jars, "/lwjgl_util-2", null) != null
                && newest(jars, "/jinput-2", "natives") != null && newest(jars, "lwjgl-platform-2", null, nativesName) != null
                && newest(jars, "jinput-platform-2", null, nativesName) != null;
        boolean noDownload = false;
        for (String a : args) {
            if (a.equals("--no-download")) {
                noDownload = true;
            }
        }
        if (!complete && !noDownload) {
            try {
                GameDownload.ensure(version, gameRoot);
            } catch (IOException e) {
                System.err.println("placeholder text" + e.getMessage()
                        + "placeholder text" + version + "placeholder text");
                System.exit(2);
            }
            jars = allJars();
        }
        if (option(args, "--java", null) == null) {
            String found = findJava8();
            if (found == null) {
                // No Java 8 anywhere: Alpha also runs on the Java that is running this loader (tested on Java 25).
                found = new File(System.getProperty("java.home"), "bin/java" + (os().equals("windows") ? ".exe" : "")).getPath();
                System.out.println("placeholder text" + found);
            } else {
                System.out.println("placeholder text" + found);
            }
            java = found;
        }
        if (gameJar == null) {
            gameJar = findGame(jars, version);
        }
        if (gameJar == null || !gameJar.isFile()) {
            System.err.println("placeholder text" + version + "placeholder text");
            System.exit(2);
        }
        if (!gameJar.getName().contains(version)) {
            System.err.println("placeholder text" + version + ": " + gameJar.getName());
            System.exit(2);
        }
        for (String a : args) {
            if (a.equals("--spawn-town")) {
                System.setProperty("orrin.spawn", "town"); // testing only: the world is made with its spawn in the town
            }
        }
        File world = new File(gameDir(homeDir), "saves/World1");
        if (!new File(world, "level.dat").exists()) {
            OrrinBend.write(world, AlphaVersion.isAftermath(version));
            System.out.println("placeholder text" + world);
        }
        File natives = new File(homeDir, "natives");
        natives.mkdirs();
        // The native libraries for this computer's system: natives-windows, natives-osx or natives-linux.
        String classifier = os().equals("windows") ? "natives-windows" : os().equals("mac") ? "natives-osx" : "natives-linux";
        File nativeJar = newest(jars, "lwjgl-platform-2", null, classifier);
        File jinputNatives = newest(jars, "jinput-platform-2", null, classifier);
        for (File jar : new File[]{nativeJar, jinputNatives}) {
            if (jar != null) {
                unzipNatives(jar, natives);
            }
        }
        List<String> classpath = new ArrayList<String>();
        File self = new File(OrrinLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        classpath.add(self.getAbsolutePath());
        classpath.add(gameJar.getAbsolutePath());
        for (String part : new String[]{"/lwjgl-2", "/lwjgl_util-2", "/jinput-2", "/jutils-"}) {
            File f = newest(jars, part, "natives");
            if (f != null) {
                classpath.add(f.getAbsolutePath());
            }
        }
        List<String> command = new ArrayList<String>();
        command.add(java);
        command.add("-Duser.home=" + homeDir.getAbsolutePath());
        command.add("-Word0943.mods=" + new File(homeDir, "mods").getAbsolutePath());
        command.add("-Word0943.version=" + version);
        String fake = option(args, "--fakecrash", null);
        if (fake != null) {
            command.add("-Word0943.fakecrash=" + fake);
        }
        if (option(args, "--autoload", null) != null) {
            command.add("-Word0943.autoload=" + option(args, "--autoload", null));
        }
        for (String a : args) {
            if (a.equals("--calm")) {
                command.add("-Word0943.calm=on");
            }
            if (a.equals("--no-close")) {
                command.add("-Word0943.fakecrash.close=off");
            }
        }
        command.add("-Djava.library.path=" + natives.getAbsolutePath());
        command.add("-Dnet.java.games.input.librarypath=" + natives.getAbsolutePath());
        command.add("-cp");
        command.add(join(classpath));
        command.add("com.realarqade.orrin.OrrinMain");
        command.add(user);
        command.add("-");
        new File(homeDir, "mods").mkdirs();
        System.out.println("Starting " + version + ": " + AlphaVersion.lore(version));
        // Alpha does not use "user.home" on Windows: it looks for the folder named in the APPDATA environment variable and puts
        // .minecraft inside it. Point both at our own folder, or the game would use (and write to) the real .minecraft.
        ProcessBuilder builder = new ProcessBuilder(command).inheritIO();
        builder.environment().put("APPDATA", homeDir.getAbsolutePath());
        builder.environment().put("HOME", homeDir.getAbsolutePath());
        Process process = builder.start();
        System.exit(process.waitFor());
    }

    /** Whether this java program is a Java 8 (Alpha only runs on that). */
    private static boolean isJava8(String java) {
        try {
            Process p = new ProcessBuilder(java, "-version").redirectErrorStream(true).start();
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            while (r.readLine() != null) {
                // drain
            }
            p.waitFor();
            return line != null && line.contains("\"1.8.");
        } catch (Exception e) {
            return false;
        }
    }

    private static void collectJava(File dir, int depth, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null || depth > 7) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collectJava(f, depth + 1, out);
            } else if ((f.getName().equals("java") || f.getName().equals("java.exe")) && f.getParentFile().getName().equals("bin")) {
                out.add(f);
            }
        }
    }

    /**
     * Looks for a Java 8 without being told where: JAVA_HOME and the "java" on the path, then the places launchers and systems keep one
     * (Prism's java folder, the official launcher's runtime folder, /usr/lib/jvm, macOS's JavaVirtualMachines). Returns null if none.
     */
    static String findJava8() {
        List<File> candidates = new ArrayList<File>();
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            candidates.add(new File(javaHome, "bin/java" + (os().equals("windows") ? ".exe" : "")));
        }
        for (File root : roots()) {
            collectJava(new File(root, "java"), 0, candidates);
            collectJava(new File(root, "runtime"), 0, candidates);
        }
        collectJava(new File("/usr/lib/jvm"), 0, candidates);
        collectJava(new File("/Library/Java/JavaVirtualMachines"), 0, candidates);
        collectJava(new File(System.getProperty("user.home"), ".jdks"), 0, candidates);
        if (os().equals("windows")) {
            collectJava(new File(System.getenv("ProgramFiles") == null ? "C:/Program Files" : System.getenv("ProgramFiles"), "Java"), 0, candidates);
        }
        candidates.add(new File(os().equals("windows") ? "java.exe" : "java")); // whatever is on the path, last
        for (File c : candidates) {
            if ((c.isAbsolute() ? c.isFile() : true) && isJava8(c.getPath())) {
                return c.getPath();
            }
        }
        return null;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(p);
        }
        return sb.toString();
    }

    private static void unzipNatives(File jar, File into) throws IOException {
        ZipFile zip = new ZipFile(jar);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String n = e.getName();
                if (e.isDirectory() || n.contains("/") || n.startsWith("META-INF")) {
                    continue;
                }
                InputStream in = zip.getInputStream(e);
                OutputStream out = new FileOutputStream(new File(into, n));
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                out.close();
                in.close();
            }
        } finally {
            zip.close();
        }
    }
}
