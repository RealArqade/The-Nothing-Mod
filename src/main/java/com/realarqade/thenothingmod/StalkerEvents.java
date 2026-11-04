package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The server-side events around the monsters:
 * <ul>
 *   <li>Every second, each player has a small chance of one of the four turning up (each has its own rule, see {@link #RULES}).
 *   Only one is ever around a player at a time.</li>
 *   <li>A player who was kicked by The Nothing is dead when they rejoin, so they land on the respawn screen.</li>
 * </ul>
 * Kicked players are remembered with a tag on the player ({@link #DOOMED_TAG}), which is saved with their player data, so it
 * survives the disconnect and a server restart.
 */
public final class StalkerEvents {
    public static final String DOOMED_TAG = "the_nothing_mod_doomed";

    /** Ticks to wait after rejoining before the death, so the world has loaded in and the death screen shows properly. */
    private static final int REJOIN_DELAY_TICKS = 40;

    /**
     * When and where each monster appears.
     *
     * @param averageMinutes on average one appearance per player this often
     * @param needsView      only appears somewhere the player can see
     * @param behindPlayer   appears behind where the player is facing
     * @param nightOnly      only appears in the dark
     */
    private record Rule(String name, EntityType<? extends Entity> type, int averageMinutes, int cooldownTicks,
                        int minDistance, int maxDistance, boolean needsView, boolean behindPlayer, boolean nightOnly,
                        float width, float height) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("nothing", ModEntities.ID_0193,
                    StalkerProperties.SPAWN_AVERAGE_MINUTES, StalkerProperties.SPAWN_COOLDOWN_TICKS,
                    StalkerProperties.SPAWN_DISTANCE_MIN, StalkerProperties.SPAWN_DISTANCE_MAX,
                    true, false, false, StalkerProperties.HITBOX_WIDTH, StalkerProperties.HITBOX_HEIGHT),
            new Rule("surveyor", ModEntities.ID_0462,
                    ApparitionProperties.SURVEYOR_AVERAGE_MINUTES, ApparitionProperties.SURVEYOR_COOLDOWN_TICKS,
                    ApparitionProperties.SURVEYOR_SPAWN_DISTANCE_MIN, ApparitionProperties.SURVEYOR_SPAWN_DISTANCE_MAX,
                    true, false, false, ApparitionProperties.SURVEYOR_HITBOX_WIDTH, ApparitionProperties.SURVEYOR_HITBOX_HEIGHT),
            new Rule("girl", ModEntities.ID_0198,
                    ApparitionProperties.GIRL_AVERAGE_MINUTES, ApparitionProperties.GIRL_COOLDOWN_TICKS,
                    ApparitionProperties.GIRL_SPAWN_DISTANCE_MIN, ApparitionProperties.GIRL_SPAWN_DISTANCE_MAX,
                    false, true, false, ApparitionProperties.GIRL_HITBOX_WIDTH, ApparitionProperties.GIRL_HITBOX_HEIGHT),
            new Rule("Word0754", ModEntities.ID_0487,
                    ApparitionProperties.LAMPLIGHTER_AVERAGE_MINUTES, ApparitionProperties.LAMPLIGHTER_COOLDOWN_TICKS,
                    ApparitionProperties.LAMPLIGHTER_SPAWN_DISTANCE_MIN, ApparitionProperties.LAMPLIGHTER_SPAWN_DISTANCE_MAX,
                    false, false, true, ApparitionProperties.LAMPLIGHTER_HITBOX_WIDTH, ApparitionProperties.LAMPLIGHTER_HITBOX_HEIGHT));

    private static final Map<UUID, Integer> pendingDeaths = new HashMap<>();
    private static final List<Delayed> delayed = new ArrayList<>();
    /** Per player and monster, the game tick before which it can't appear again. */
    private static final Map<String, Long> noSpawnBefore = new HashMap<>();

    private static final class Delayed {
        final UUID player;
        int ticksLeft;
        final Consumer<ServerPlayer> action;

        Delayed(UUID player, int ticksLeft, Consumer<ServerPlayer> action) {
            this.player = player;
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }

    /** Runs something for a player after a number of ticks, if they are still in the game by then. */
    public static void runLater(ServerPlayer player, int ticks, Consumer<ServerPlayer> action) {
        delayed.add(new Delayed(player.getUUID(), ticks, action));
    }

    private StalkerEvents() {
    }

    public static void register() {
        Platform.get().onPlayerJoin(player -> {
            if (Compat.hasTag(player, DOOMED_TAG)) {
                pendingDeaths.put(player.getUUID(), REJOIN_DELAY_TICKS);
            }
        });
        Platform.get().onServerTickEnd(StalkerEvents::tick);
        Platform.get().onServerStopped(server -> {
            delayed.clear();
            pendingDeaths.clear();
            noSpawnBefore.clear();
        });
    }

    private static void tick(MinecraftServer server) {
        // Players who rejoin after being kicked: dead, once the delay is over.
        Iterator<Map.Entry<UUID, Integer>> iterator = pendingDeaths.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove(); // left again; the tag stays, so it still happens on the next rejoin
                continue;
            }
            int left = entry.getValue() - 1;
            if (left > 0) {
                entry.setValue(left);
                continue;
            }
            iterator.remove();
            player.removeTag(DOOMED_TAG);
            if (player.isAlive() && player.level() instanceof ServerLevel level) {
                player.kill(level);
            }
        }

        // Things scheduled with runLater (the kick or death that follows the jumpscare).
        for (Iterator<Delayed> it = delayed.iterator(); it.hasNext(); ) {
            Delayed item = it.next();
            if (--item.ticksLeft > 0) {
                continue;
            }
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(item.player);
            if (player != null) {
                item.action.accept(player);
            }
        }

        if (StalkerProperties.SPAWNING_ENABLED && server.getTickCount() % 20 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                maybeSpawnFor(server, player);
            }
        }
    }

    // --- Appearing ---

    private static void maybeSpawnFor(MinecraftServer server, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || level.getDifficulty() == Difficulty.PEACEFUL
                || !player.isAlive()
                || player.isSpectator()
                || player.tickCount < StalkerProperties.SPAWN_GRACE_TICKS) {
            return;
        }
        // Only one monster of any kind around a player at a time.
        if (!level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(StalkerProperties.VANISH_DISTANCE),
                entity -> entity instanceof Apparition).isEmpty()) {
            return;
        }

        for (Rule rule : RULES) {
            String key = player.getUUID() + "/" + rule.name();
            if (noSpawnBefore.getOrDefault(key, 0L) > server.getTickCount()
                    || (rule.nightOnly() && !level.isDarkOutside())
                    // This runs once a second, so this is the chance per second.
                    || player.getRandom().nextDouble() > 1.0 / (rule.averageMinutes() * 60.0)) {
                continue;
            }
            if (spawn(level, player, rule)) {
                noSpawnBefore.put(key, (long) server.getTickCount() + rule.cooldownTicks());
                return; // one at a time
            }
        }
    }

    /** Looks for open ground in the rule's distance range (in view, or behind the player, if the rule says so) and puts the monster there. */
    private static boolean spawn(ServerLevel level, ServerPlayer player, Rule rule) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle;
            if (rule.behindPlayer()) {
                // Straight behind where they're facing, give or take 40 degrees.
                double yaw = Math.toRadians(player.getYRot());
                angle = Math.atan2(-Math.cos(yaw), Math.sin(yaw)) + (player.getRandom().nextDouble() - 0.5) * Math.toRadians(80);
            } else {
                angle = player.getRandom().nextDouble() * Math.PI * 2;
            }
            double distance = rule.minDistance() + player.getRandom().nextDouble() * (rule.maxDistance() - rule.minDistance());
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z))) {
                continue;
            }

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos ground = feet.below();
            boolean solidGround = level.getFluidState(ground).isEmpty() && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP);
            if (!solidGround || !level.canSeeSky(feet)) {
                continue;
            }
            float width = rule.width();
            AABB body = new AABB(x + 0.5 - width / 2, y, z + 0.5 - width / 2, x + 0.5 + width / 2, y + rule.height(), z + 0.5 + width / 2);
            if (!level.noCollision(body)) {
                continue;
            }
            if (rule.needsView()) {
                // The player has to be able to see it: nothing solid between their eyes and its head.
                Vec3 target = new Vec3(x + 0.5, y + rule.height() * 0.8, z + 0.5);
                ClipContext sight = new ClipContext(player.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
                if (level.clip(sight).getType() != HitResult.Type.MISS) {
                    continue;
                }
            }

            if (rule.type().spawn(level, feet, EntitySpawnReason.EVENT) != null) {
                return true;
            }
        }
        return false;
    }
}
