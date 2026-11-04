package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * The rules that keep real play fair: no creative mode, no spectator mode, no cheat commands.
 * The mixins ({@code ServerPlayerGameModeMixin}, {@code CommandsMixin}, {@code LevelSettingsMixin}) all ask this class.
 * <p>
 * The restrictions are switched off in a development environment, so you can still use {@code /summon}, {@code /give},
 * creative mode and so on while testing. Set {@link #ENFORCE_IN_DEV} to {@code true} to test the lockdown itself.
 * <p>
 * Players listed in {@link DevUsers} are exempt in a real release too (see {@link #isDevUser}).
 */
public final class PlayRestrictions {
    /** Apply the restrictions inside the dev environment too. */
    public static final boolean ENFORCE_IN_DEV = false;

    /** Set by the client at startup: is the account playing on this computer a listed dev user? Always false on a dedicated server. */
    private static volatile BooleanSupplier localUserIsDev = () -> false;

    private PlayRestrictions() {
    }

    public static boolean active() {
        return ENFORCE_IN_DEV || !Platform.get().isDevelopment();
    }

    /** Whether this account's UUID is in {@link DevUsers}. Only says who is listed; see {@link #isDevUser} for who actually gets the powers. */
    public static boolean isDevUuid(UUID uuid) {
        String wanted = uuid.toString();
        for (String listed : DevUsers.UUIDS) {
            if (listed.equalsIgnoreCase(wanted)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this player gets the dev powers on the server they are on. They have to be listed in {@link DevUsers}, and then:
     * <ul>
     *   <li>on a dedicated server, {@code allow_dev_users} must be on in that server's config (it is by default, and the server
     *   console warns about it at startup, so the owner can turn it off);</li>
     *   <li>in single player or on a LAN world, they have to be the owner (Name 0615). A guest on someone else's world never gets it.</li>
     * </ul>
     * This never makes anyone an operator: the server's normal operator rules still decide whether they can run a command.
     */
    public static boolean isDevUser(ServerPlayer player) {
        if (!isDevUuid(player.getUUID())) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        return server.isDedicatedServer()
                ? ServerSettings.allowDevUsers()
                : server.isSingleplayerOwner(player.nameAndId());
    }

    public static void setLocalUserIsDev(BooleanSupplier supplier) {
        localUserIsDev = supplier;
    }

    public static boolean localUserIsDev() {
        return localUserIsDev.getAsBoolean();
    }

    /** Creative and spectator become survival, whatever asked for them (a command, the F3+F4 switcher, a world's saved mode, ...). Dev users keep them. */
    public static GameType filter(GameType type, ServerPlayer player) {
        if (active() && (type == GameType.CREATIVE || type == GameType.SPECTATOR) && !isDevUser(player)) {
            return GameType.SURVIVAL;
        }
        return type;
    }

    /**
     * Commands typed by a player are blocked, unless they are a dev user (who is then still limited by the server's normal operator
     * rules). Commands from the server console and command blocks are left alone, so a server owner can still run {@code /stop}.
     */
    public static boolean blocksCommandsFrom(CommandSourceStack source) {
        return active() && source.getEntity() instanceof ServerPlayer player && !isDevUser(player);
    }

    /** Whether this player is not sent the list of commands (so there is nothing to tab-complete). */
    public static boolean hidesCommandsFrom(ServerPlayer player) {
        return active() && !isDevUser(player);
    }
}
