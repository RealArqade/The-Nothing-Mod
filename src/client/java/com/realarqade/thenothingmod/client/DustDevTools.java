package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.platform.Platform;

import com.realarqade.thenothingmod.DustSettingsPayload;
import com.realarqade.thenothingmod.ModBlocks;
import com.realarqade.thenothingmod.PlayRestrictions;
import com.realarqade.thenothingmod.SuspiciousDustProperties;
import com.realarqade.thenothingmod.TheNothingMod;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Development helpers. The "Dev tools" tab is only built when {@link #hasDevControls()}: in a development environment, or for an
 * account listed in {@code DevUsers}. Everyone else never sees it.
 */
final class DustDevTools {
    private DustDevTools() {
    }

    /** Launched from the IDE (not an installed release). Skippable disclaimer, baking into the source files. */
    static boolean inDevelopmentEnvironment() {
        return Platform.get().isDevelopment();
    }

    /** The Dev tools tab: in a development environment, or for a listed dev account in a real release too. */
    static boolean hasDevControls() {
        return inDevelopmentEnvironment() || PlayRestrictions.isDevUuid(Minecraft.getInstance().getUser().getProfileId());
    }

    /**
     * Writes the current tuning values into the project's source resources, so the next build ships exactly what you
     * were looking at in-game. The dev game runs in {@code <project>/run}, so the project root is its parent folder.
     * Only called in a development environment: a release has no project folder to write into.
     */
    static void bakeForRelease() {
        Path root = Platform.get().gameDir().toAbsolutePath().normalize().getParent();
        Path resources = root == null ? null : root.resolve("src").resolve("main").resolve("resources");
        if (resources == null || !Files.isDirectory(resources)) {
            TheNothingMod.LOGGER.warn("placeholder text");
            return;
        }

        Path target = resources.resolve(SuspiciousDustProperties.BAKED_FILE_NAME);
        try {
            Files.writeString(target, ModBlocks.DUST_PROPERTIES.toJson(), StandardCharsets.UTF_8);
            TheNothingMod.LOGGER.info("placeholder text", target);
        } catch (IOException e) {
            TheNothingMod.LOGGER.warn("placeholder text", target, e);
        }
    }

    /**
     * When you are connected to someone else's server, the dust settings that matter (colours, conducting, diagonals) live on that
     * server, so ask it to change them. The server checks who is asking and ignores anyone who isn't allowed.
     * In single player there is nothing to send: the game and the world share the same settings.
     */
    static void pushToServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() != null && !client.isLocalServer() && ClientPlatform.get().canSend(DustSettingsPayload.TYPE)) {
            ClientPlatform.get().send(DustSettingsPayload.of(ModBlocks.DUST_PROPERTIES));
        }
    }
}
