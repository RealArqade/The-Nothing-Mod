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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes the monsters from the monster list turn up, in the dimension each belongs to. Same rules as the first four
 * ({@link StalkerEvents}): once a second each player has a small chance, only one monster is ever around a player, never on
 * Peaceful, and not in the first minutes after joining. Its numbers come from each monster's entry in the list.
 */
public final class MonsterEvents {
    private static final Map<String, Long> noSpawnBefore = new HashMap<>();

    private MonsterEvents() {
    }

    public static void register() {
        Platform.get().onServerTickEnd(MonsterEvents::tick);
        Platform.get().onServerStopped(server -> noSpawnBefore.clear());
    }

    private static String dimensionName(ServerLevel level) {
        if (Dimensions.isOurs(level)) {
            return Dimensions.idOf(level);
        }
        if (level.dimension() == Level.OVERWORLD) {
            return "overworld";
        }
        if (level.dimension() == Level.NETHER) {
            return "nether";
        }
        return level.dimension() == Level.END ? "end" : "";
    }

    private static void tick(MinecraftServer server) {
        if (!StalkerProperties.SPAWNING_ENABLED || server.getTickCount() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            maybeSpawnFor(server, player);
        }
    }

    private static void maybeSpawnFor(MinecraftServer server, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)
                || level.getDifficulty() == Difficulty.PEACEFUL
                || !player.isAlive()
                || player.isSpectator()
                || player.tickCount < StalkerProperties.SPAWN_GRACE_TICKS) {
            return;
        }
        String dimension = dimensionName(level);
        if (dimension.isEmpty()
                || !level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(StalkerProperties.VANISH_DISTANCE),
                entity -> entity instanceof Apparition).isEmpty()) {
            return;
        }
        for (MonsterDef def : ModMonsters.DEFS) {
            if (!def.dims().contains(dimension)) {
                continue;
            }
            String key = player.getUUID() + "/" + def.id();
            if (noSpawnBefore.getOrDefault(key, 0L) > server.getTickCount()
                    || (def.nightOnly() && dimension.equals("overworld") && !level.isDarkOutside())
                    // Once a second, so this is the chance per second; many monsters share a dimension, so thin it out.
                    || player.getRandom().nextDouble() > 1.0 / (def.avgMinutes() * 60.0 * 3.0)) {
                continue;
            }
            if (spawn(level, player, def, dimension)) {
                noSpawnBefore.put(key, (long) server.getTickCount() + def.cooldown());
                return;
            }
        }
    }

    private static boolean spawn(ServerLevel level, ServerPlayer player, MonsterDef def, String dimension) {
        boolean caves = def.underground() || !(dimension.equals("overworld") || dimension.startsWith("d"));
        boolean voidFloor = dimension.startsWith("v");
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle;
            if (def.behind()) {
                double yaw = Math.toRadians(player.getYRot());
                angle = Math.atan2(-Math.cos(yaw), Math.sin(yaw)) + (player.getRandom().nextDouble() - 0.5) * Math.toRadians(80);
            } else {
                angle = player.getRandom().nextDouble() * Math.PI * 2;
            }
            double distance = def.min() + player.getRandom().nextDouble() * (def.max() - def.min());
            if (voidFloor) {
                distance = 1.0 + player.getRandom().nextDouble() * 2.0; // the floor is only a few blocks wide
            } else if (caves) {
                distance = Math.min(distance, 22.0); // tunnels are tight; stay close enough to be in the same cave
            }
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z))) {
                continue;
            }
            int y = voidFloor ? voidFloor(level, x, z, player.getBlockY()) : caves ? findCaveFloor(level, x, player.getBlockY(), z, def) : surface(level, x, z);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            float width = def.hitW();
            AABB body = new AABB(x + 0.5 - width / 2, y, z + 0.5 - width / 2, x + 0.5 + width / 2, y + def.hitH(), z + 0.5 + width / 2);
            if (!level.noCollision(body)) {
                continue;
            }
            if (def.needsView()) {
                Vec3 target = new Vec3(x + 0.5, y + def.hitH() * 0.8, z + 0.5);
                ClipContext sight = new ClipContext(player.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
                if (level.clip(sight).getType() != HitResult.Type.MISS) {
                    continue;
                }
            }
            BlockPos feet = new BlockPos(x, y, z);
            if (ModMonsters.TYPES.get(def.id()).spawn(level, feet, EntitySpawnReason.EVENT) == null) {
                continue;
            }
            if (def.behavior() == MonsterDef.Behavior.CROWD) {
                for (int i = 0; i < 3; i++) {
                    BlockPos next = feet.offset(player.getRandom().nextInt(7) - 3, 0, player.getRandom().nextInt(7) - 3);
                    if (level.noCollision(body.move(next.getX() - x, 0, next.getZ() - z))) {
                        ModMonsters.TYPES.get(def.id()).spawn(level, next, EntitySpawnReason.EVENT);
                    }
                }
            }
            return true;
        }
        return false;
    }

    /** In the void dimensions the only floor is the small platform: stand on it if it is there. */
    private static int voidFloor(ServerLevel level, int x, int z, int playerY) {
        BlockPos ground = new BlockPos(x, playerY - 1, z);
        return level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP) && level.getBlockState(ground.above()).isAir()
                ? playerY : Integer.MIN_VALUE;
    }

    private static int surface(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos ground = feet.below();
        boolean solid = level.getFluidState(ground).isEmpty() && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP);
        return solid && level.canSeeSky(feet) ? y : Integer.MIN_VALUE;
    }

    /** Looks up and down from the player's height for floor with room above it, and (underground) no sky. */
    private static int findCaveFloor(ServerLevel level, int x, int playerY, int z, MonsterDef def) {
        List<Integer> candidates = new ArrayList<>();
        int top = Math.min(level.getMaxY() - 2, playerY + 8);
        for (int y = top; y >= Math.max(level.getMinY() + 1, playerY - 12); y--) {
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos ground = feet.below();
            if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                    && level.getFluidState(ground).isEmpty() && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
                    && (level.dimension() != Level.OVERWORLD || !level.canSeeSky(feet))) {
                candidates.add(y);
            }
        }
        return candidates.isEmpty() ? Integer.MIN_VALUE : candidates.get(level.getRandom().nextInt(candidates.size()));
    }
}
