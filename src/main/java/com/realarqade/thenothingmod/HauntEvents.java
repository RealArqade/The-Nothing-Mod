package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The rare, loud things that happen to the player's own computer screen: a fake blue screen, and, even more rarely, a deliberate
 * crash of the game (saved first, and it can be switched off in the mod's settings). Both are deliberately limited:
 * <ul>
 *   <li>not in the first {@link #MIN_SESSION_TICKS} of a session (so a crash can never greet you on launch),</li>
 *   <li>at most {@link #MAX_BLUESCREENS} blue screens and {@link #MAX_CRASHES} crashes per player per world, counted with tags on the
 *   player, so a relaunch never repeats one,</li>
 *   <li>never anything that kicks, bans or touches the world. After a crash you just start the game again.</li>
 * </ul>
 */
public final class HauntEvents {
    public static final int MIN_SESSION_TICKS = 20 * 60 * 25;
    public static final int MAX_BLUESCREENS = 3;
    public static final int MAX_CRASHES = 2;
    /** On average one event per player per this many seconds of play, once they are past the minimum. */
    public static final int AVERAGE_SECONDS = 60 * 35;

    private HauntEvents() {
    }

    public static void register() {
        Platform.get().onServerTickEnd(HauntEvents::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Fake corruption: a few seconds of glitching in the game, from about 8 minutes in, rarely, never more than 6 times per world.
            if (player.tickCount > 20 * 60 * 8 && player.isAlive() && player.getRandom().nextInt(60 * 12) == 0
                    && count(player, "corrupt") < 6 && Platform.get().canSend(player, HauntPayloads.Corruption.TYPE)) {
                bump(player, "corrupt");
                Platform.get().send(player, new HauntPayloads.Corruption());
            }
            // The first two notes on the Desktop arrive in order, from 15 and 40 minutes into a session.
            if (player.tickCount > 20 * 60 * 15 && player.isAlive() && player.getRandom().nextInt(60 * 10) == 0) {
                desktopFile(player, Compat.hasTag(player, "tnm_desktopfile_0") ? 1 : 0);
            }
            if (player.tickCount < MIN_SESSION_TICKS || !player.isAlive()
                    || player.getRandom().nextInt(AVERAGE_SECONDS) != 0) {
                continue;
            }
            // Mostly blue screens; a crash is the rarer one.
            if (player.getRandom().nextInt(4) != 0) {
                if (count(player, "bsod") < MAX_BLUESCREENS && Platform.get().canSend(player, HauntPayloads.Bsod.TYPE)) {
                    bump(player, "bsod");
                    Platform.get().send(player, new HauntPayloads.Bsod());
                }
            } else if (count(player, "crash") < MAX_CRASHES && Platform.get().canSend(player, HauntPayloads.Crash.TYPE)) {
                bump(player, "crash");
                // Make sure the world is on disk first, in single player the integrated server is this one.
                server.saveEverything(true, false, false);
                Platform.get().send(player, new HauntPayloads.Crash());
            }
        }
    }

    /** Tells the player's game to write Desktop file number id, once per player per world (counted with a tag). */
    public static void desktopFile(ServerPlayer player, int id) {
        String tag = "tnm_desktopfile_" + id;
        if (Compat.hasTag(player, tag) || !Platform.get().canSend(player, HauntPayloads.DesktopFile.TYPE)) {
            return;
        }
        player.addTag(tag);
        Platform.get().send(player, new HauntPayloads.DesktopFile(id));
    }

    private static int count(ServerPlayer player, String kind) {
        int n = 0;
        while (Compat.hasTag(player, "tnm_" + kind + "_" + (n + 1))) {
            n++;
        }
        return n;
    }

    private static void bump(ServerPlayer player, String kind) {
        player.addTag("tnm_" + kind + "_" + (count(player, kind) + 1));
    }
}
