package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Everything about dev users that happens on the server:
 * <ul>
 *   <li>the console warning at startup on a dedicated server, and a line whenever a dev user joins;</li>
 *   <li>keeping every player's copy of the dust settings in step with the server's;</li>
 *   <li>letting a dev user change those settings on the server, after checking with {@link PlayRestrictions#isDevUser}.</li>
 * </ul>
 */
public final class DevSync {
    private DevSync() {
    }

    public static void register() {
        Platform platform = Platform.get();
        platform.registerClientbound(DustSettingsPayload.TYPE, DustSettingsPayload.CODEC);
        platform.registerServerbound(DustSettingsPayload.TYPE, DustSettingsPayload.CODEC, DevSync::receive);

        platform.onServerStarted(DevSync::announce);

        platform.onPlayerJoin(player -> {
            // Every player learns this server's dust settings, so everyone sees the same colours.
            if (platform.canSend(player, DustSettingsPayload.TYPE)) {
                platform.send(player, DustSettingsPayload.of(ModBlocks.DUST_PROPERTIES));
            }
            if (platform.isDedicatedServer() && PlayRestrictions.isDevUuid(player.getUUID())) {
                if (ServerSettings.allowDevUsers()) {
                    TheNothingMod.LOGGER.warn("placeholder text",
                            player.getName().getString(), player.getUUID());
                } else {
                    TheNothingMod.LOGGER.info("placeholder text",
                            player.getName().getString());
                }
            }
        });
    }

    private static void receive(DustSettingsPayload payload, ServerPlayer player) {
        Platform platform = Platform.get();
        if (!PlayRestrictions.isDevUser(player)) {
            TheNothingMod.LOGGER.warn("placeholder text",
                    player.getName().getString());
            return;
        }
        if (payload.matches(ModBlocks.DUST_PROPERTIES)) {
            return;
        }
        payload.applyTo(ModBlocks.DUST_PROPERTIES);
        TheNothingMod.LOGGER.info("placeholder text", player.getName().getString());
        for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
            if (platform.canSend(other, DustSettingsPayload.TYPE)) {
                platform.send(other, DustSettingsPayload.of(ModBlocks.DUST_PROPERTIES));
            }
        }
    }

    /** The warning every server owner sees when the server starts. Only on a dedicated server: that is where somebody else is the owner. */
    private static void announce(MinecraftServer server) {
        if (!server.isDedicatedServer()) {
            return;
        }
        int count = DevUsers.UUIDS.size();
        if (ServerSettings.allowDevUsers()) {
            TheNothingMod.LOGGER.warn("placeholder text");
            TheNothingMod.LOGGER.warn("placeholder text", count);
            TheNothingMod.LOGGER.warn("placeholder text");
            TheNothingMod.LOGGER.warn("placeholder text");
            TheNothingMod.LOGGER.warn("placeholder text");
            TheNothingMod.LOGGER.warn("placeholder text", TheNothingMod.MOD_ID);
            TheNothingMod.LOGGER.warn("placeholder text");
            TheNothingMod.LOGGER.warn("placeholder text");
        } else {
            TheNothingMod.LOGGER.info("placeholder text");
        }
    }
}
