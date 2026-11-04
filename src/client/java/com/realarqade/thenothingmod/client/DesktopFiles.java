package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.platform.Platform;

import com.realarqade.thenothingmod.TheNothingMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The notes that appear on the player's Desktop, as the content warning says they might. They are part of the story and nothing else:
 * <ul>
 *   <li>Plain text files (.txt), a few lines long, named "placeholder text" and starting with a line saying the mod made them and that
 *   they can be deleted.</li>
 *   <li>At most five, each written once; an existing file is never overwritten; nothing is ever deleted except the files this class made
 *   itself (listed in {@code config/the-nothing-mod-desktop.txt}), and only when the player presses the button in the settings.</li>
 *   <li>Never in Word0949 Mode (a Desktop is on stream), never when "Desktop notes" is switched off, and never anywhere but the Desktop.</li>
 *   <li>Only one of them uses anything about the player (see {@link PersonalDetails}), and then only if that is allowed.</li>
 *   <li>Nothing here touches the network.</li>
 * </ul>
 * Which note when: 0 and 1 arrive at random in long sessions, 2 the first time the player goes through a gate or falls into the void,
 * 3 when the Word0794 is called up, 4 when it is beaten (see {@code HauntEvents.desktopFile}).
 */
final class DesktopFiles {
    private static final String MARKER = "placeholder text";

    private DesktopFiles() {
    }

    private static Path manifest() {
        return Platform.get().configDir().resolve("the-nothing-mod-desktop.txt");
    }

    /** The Desktop folder, or null if there isn't one we can find. A system property can point somewhere else, for testing. */
    static Path desktop() {
        String test = System.getProperty("thenothing.desktop");
        if (test != null) {
            return Path.of(test);
        }
        String home = System.getProperty("user.home");
        List<Path> candidates = new ArrayList<>();
        // OneDrive's Desktop first (it is the visible one when OneDrive backs the Desktop up), then whatever Windows says, then the plain one.
        if (System.getenv("OneDrive") != null) {
            candidates.add(Path.of(System.getenv("OneDrive"), "Desktop"));
        }
        // On Windows, ask the system where the visible Desktop is (it is the OneDrive one when OneDrive backs the Desktop up).
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                Process p = new ProcessBuilder("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "/v", "Desktop")
                        .redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                p.waitFor();
                for (String line : out.split("\\R")) {
                    int at = line.indexOf("REG_SZ");
                    if (line.contains("Desktop") && at >= 0) {
                        candidates.add(Path.of(line.substring(at + "REG_SZ".length()).trim()));
                    }
                }
            } catch (IOException | InterruptedException | RuntimeException e) {
                // fall back to the guesses below
            }
        }
        // Linux: the Desktop folder can have another name or place (it follows the language); the user's own setting says where.
        String xdg = System.getenv("XDG_DESKTOP_DIR");
        if (xdg != null && !xdg.isBlank()) {
            candidates.add(Path.of(xdg));
        }
        try {
            Path dirs = Path.of(home, ".config", "user-dirs.dirs");
            if (Files.isRegularFile(dirs)) {
                for (String line : Files.readAllLines(dirs, StandardCharsets.UTF_8)) {
                    if (line.startsWith("XDG_DESKTOP_DIR=")) {
                        String value = line.substring("XDG_DESKTOP_DIR=".length()).trim().replace("\"", "").replace("$HOME", home);
                        candidates.add(Path.of(value));
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            // fall back to the plain one
        }
        // macOS (and the fallback everywhere): ~/Desktop. With iCloud syncing the Desktop, macOS still shows it there.
        candidates.add(Path.of(home, "Desktop"));
        for (Path p : candidates) {
            if (Files.isDirectory(p)) {
                return p;
            }
        }        return null;
    }

    private static String name(int id) {
        return switch (id) {
            case 0 -> "placeholder text";
            case 1 -> "placeholder text";
            case 2 -> "placeholder text";
            case 3 -> "placeholder text";
            default -> "placeholder text";
        };
    }

    private static String body(int id) {
        return switch (id) {
            case 0 -> String.join("\n",
                    "placeholder text",
                    "placeholder text",
                    "",
                    "placeholder text",
                    "placeholder text");
            case 1 -> {
                List<String> lines = new ArrayList<>();
                lines.add(PersonalDetails.allowed() ? "Word0948, " + PersonalDetails.userName() + "." : "Word0948.");
                lines.add("");
                lines.add("placeholder text");
                if (PersonalDetails.allowed()) {
                    lines.add("It is " + PersonalDetails.localTime() + " on " + PersonalDetails.machine() + ".");
                    lines.add("placeholder text");
                } else {
                    lines.add("placeholder text");
                }
                yield String.join("\n", lines);
            }
            case 2 -> String.join("\n",
                    "placeholder text",
                    "placeholder text",
                    "Difference: 412.",
                    "placeholder text",
                    "Remaining: 19.",
                    "",
                    "placeholder text");
            case 3 -> String.join("\n",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text");
            default -> String.join("\n",
                    "placeholder text",
                    "placeholder text",
                    "placeholder text");
        };
    }

    /** Writes note {@code id} to the Desktop, if the player allows it. Returns true if a file was written. */
    static boolean write(int id) {
        DustConfig config = DustConfig.get();
        if (!config.desktopFiles || config.streamerMode || id < 0 || id > 4) {
            return false;
        }
        Path dir = desktop();
        if (dir == null) {
            return false;
        }
        try {
            if (created().size() >= 5) {
                return false;
            }
            Path file = dir.resolve(name(id));
            if (Files.exists(file)) {
                return false; // never overwrite anything
            }
            String text = MARKER + "placeholder text"
                    + "------------------------------------------------------------\n\n" + body(id) + "\n";
            Files.writeString(file, text.replace("\n", "\r\n"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            Files.writeString(manifest(), file.toAbsolutePath() + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.debug("placeholder text", e);
            return false;
        }
    }

    private static List<Path> created() throws IOException {
        List<Path> list = new ArrayList<>();
        if (Files.exists(manifest())) {
            for (String line : Files.readAllLines(manifest(), StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    list.add(Path.of(line.trim()));
                }
            }
        }
        return list;
    }

    /** Deletes the notes this mod wrote (only those, and only if they still start with the mod's own line). Returns how many. */
    static int removeAll() {
        int removed = 0;
        try {
            for (Path file : created()) {
                if (Files.isRegularFile(file) && Files.readString(file, StandardCharsets.UTF_8).startsWith(MARKER)) {
                    Files.delete(file);
                    removed++;
                }
            }
            Files.deleteIfExists(manifest());
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.debug("placeholder text", e);
        }
        return removed;
    }
}
