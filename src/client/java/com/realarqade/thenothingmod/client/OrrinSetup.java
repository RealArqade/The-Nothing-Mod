package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The opt-in for Orrin's Bend, the old-Alpha part of the mod. Nothing happens unless the player presses the button in the mod's
 * settings and then confirms. The confirmation says exactly what is written and where; nothing is downloaded and nothing outside
 * {@code <game folder>/the-nothing-mod/orrin} is touched.
 * <p>
 * What it writes there: the Orrin loader (a small jar that is inside this mod), a short text file explaining how to start it, and
 * (by running the loader's own "world" command) a world save for Minecraft Alpha 1.0.14. It does not include or fetch any of Mojang's
 * game files: the loader uses the copy the player's own launcher already has.
 */
final class OrrinSetup {
    private OrrinSetup() {
    }

    static Path folder() {
        return Platform.get().gameDir().resolve("the-nothing-mod").resolve("orrin");
    }

    static void ask(Screen back) {
        Minecraft client = Minecraft.getInstance();
        client.setScreenAndShow(new ConfirmScreen(yes -> {
            if (yes) {
                String result = install();
                client.setScreenAndShow(new ConfirmScreen(open -> {
                    client.setScreenAndShow(back);
                }, Component.literal("Orrin's Bend"), Component.literal(result + "placeholder text" + folder())));
            } else {
                client.setScreenAndShow(back);
            }
        }, Component.literal("placeholder text"), Component.literal(
                "placeholder text" + folder() + ":\n"
                        + "placeholder text"
                        + "placeholder text"
                        + "placeholder text"
                        + "placeholder text"
                        + "placeholder text"
                        + "placeholder text")));
    }

    /** Start files that run the loader with the same Java that is running this game (any Java will do: the loader finds a Java 8 itself). */
    private static void writeStartScripts(Path dir) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        for (String version : new String[]{"a1.0.14", "a1.0.15"}) {
            String shortName = version.substring(1);
            String windows = "placeholder text" + java + "placeholder text" + version + "\r\npause\r\n";
            Files.writeString(dir.resolve("start " + shortName + ".bat"), windows, StandardCharsets.UTF_8);
            String unix = "placeholder text" + java + "placeholder text" + version + "\n";
            for (String name : new String[]{"start " + shortName + ".sh", "start " + shortName + ".command"}) {
                Path file = dir.resolve(name);
                Files.writeString(file, unix, StandardCharsets.UTF_8);
                file.toFile().setExecutable(true);
            }
        }
    }

    /** Starts the game for a version (the loader finds Java 8 itself). Returns a message for the player. */
    static String start(String version) {
        Path dir = folder();
        Path jar = dir.resolve("orrin-loader.jar");
        if (!Files.isRegularFile(jar)) {
            return "placeholder text";
        }
        try {
            new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar", jar.toString(), "launch", "--version", version)
                    .directory(dir.toFile()).redirectErrorStream(true)
                    .redirectOutput(dir.resolve("orrin-launch.log").toFile()).start();
            return "placeholder text" + dir
                    + "placeholder text";
        } catch (IOException e) {
            return "placeholder text" + e.getMessage();
        }
    }

    static void askToStart(Screen back, String version) {
        Minecraft client = Minecraft.getInstance();
        String message = start(version);
        client.setScreenAndShow(new ConfirmScreen(ok -> client.setScreenAndShow(back), Component.literal("Orrin's Bend"), Component.literal(message)));
    }

    private static String install() {
        try {
            Path dir = folder();
            Files.createDirectories(dir);
            Path jar = dir.resolve("orrin-loader.jar");
            try (InputStream in = OrrinSetup.class.getResourceAsStream("/orrin/orrin-loader.jar")) {
                if (in == null) {
                    return "placeholder text";
                }
                Files.copy(in, jar, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(dir.resolve("README.txt"), String.join("\n",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text",
                    "",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text",
                    "",
                    "placeholder text",
                    "placeholder text", ""), StandardCharsets.UTF_8);
            for (String version : new String[]{"a1.0.14", "a1.0.15"}) {
                // Run in the orrin folder: the loader puts the world where Alpha will look on this system (Windows, macOS or Linux).
                Process p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar", jar.toString(),
                        "world", "--version", version)
                        .directory(dir.toFile()).redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                p.waitFor();
            }
            writeStartScripts(dir);
            return "placeholder text";
        } catch (IOException | InterruptedException e) {
            return "placeholder text" + e.getMessage();
        }
    }
}