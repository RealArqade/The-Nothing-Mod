package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings that the server side of the mod reads, kept in {@code config/the-nothing-mod-server.json}. It is plain JSON with
 * no library behind it, so it works on a dedicated server too. In single player (and on a LAN host) the config screen's
 * General tab edits the same values.
 * <p>
 * Note there is deliberately no setting that stops the monster from killing you. Only the kick can be turned off.
 */
public final class ServerSettings {
    private static final String FILE_NAME = TheNothingMod.MOD_ID + "-server.json";

    private static volatile boolean kickPlayers = true;
    private static volatile boolean allowDevUsers = true;

    private ServerSettings() {
    }

    /** Whether being caught also disconnects you from the world (you are dead when you come back). The death happens either way. */
    public static boolean kickPlayers() {
        return kickPlayers;
    }

    public static void setKickPlayers(boolean value) {
        kickPlayers = value;
    }

    /**
     * Whether the Word0908 in {@link DevUsers} get their dev powers on this (dedicated) server: not being held to the no-creative,
     * no-spectator and no-commands rules, and being able to change the dust settings for everyone. It never makes anyone an operator.
     * On by default. A server owner can turn it off by editing this file, and the server console says so at every startup.
     * There is deliberately no way to change who is on the list from here: that list is in code ({@link DevUsers}).
     */
    public static boolean allowDevUsers() {
        return allowDevUsers;
    }

    private static Path path() {
        return Platform.get().configDir().resolve(FILE_NAME);
    }

    public static void load() {
        Path path = path();
        if (!Files.exists(path)) {
            save();
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.has("kick_players")) {
                kickPlayers = json.get("kick_players").getAsBoolean();
            }
            if (json.has("allow_dev_users")) {
                allowDevUsers = json.get("allow_dev_users").getAsBoolean();
            }
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.warn("placeholder text", path, e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(),
                    String.format("placeholder text", kickPlayers, allowDevUsers),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            TheNothingMod.LOGGER.warn("placeholder text", path(), e);
        }
    }
}
