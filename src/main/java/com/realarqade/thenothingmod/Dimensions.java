package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The fifty extra dimensions (see {@code tools/GenerateDimensions.ps1} and the lore file for what each one means).
 * <ul>
 *   <li>Forty have a gate: walk into the matching {@link ThresholdBlock}. The gates stand in small archways that are generated in the world.</li>
 *   <li>Ten ({@code v01} to {@code v10}) have no gate. Fall into the void anywhere, even in another void dimension, and you land
 *   in a random one of them, on a small floor next to a Name 0440 that takes you to your spawn.</li>
 * </ul>
 * Nothing here hurts you. Falling teleports you before the void would kill you.
 */
public final class Dimensions {
    public record Dim(String id, String name, boolean isVoid, String entry, String top) {
    }

    public static final List<Dim> ALL = load();
    private static final Map<String, Dim> BY_ID = new HashMap<>();
    private static final List<Dim> VOIDS = new ArrayList<>();
    private static final Map<UUID, Long> cooldown = new HashMap<>();
    /** Height of the floor in the void dimensions. */
    private static final int VOID_FLOOR_Y = 64;

    static {
        for (Dim dim : ALL) {
            BY_ID.put(dim.id(), dim);
            if (dim.isVoid()) {
                VOIDS.add(dim);
            }
        }
    }

    private Dimensions() {
    }

    public static void register() {
        Platform.get().onServerTickEnd(Dimensions::tick);
    }

    public static boolean isOurs(ServerLevel level) {
        return level.dimension().identifier().getNamespace().equals(TheNothingMod.MOD_ID);
    }

    /** Our dimension's short id ("d07", "v03") or null. */
    public static String idOf(ServerLevel level) {
        return isOurs(level) ? level.dimension().identifier().getPath() : null;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0 || VOIDS.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = (ServerLevel) player.level();
            // Below the world, before the void kills: go somewhere else.
            if (player.getY() < level.getMinY() - 24 && !player.isSpectator()) {
                String here = idOf(level);
                Dim target;
                do {
                    target = VOIDS.get(player.getRandom().nextInt(VOIDS.size()));
                } while (VOIDS.size() > 1 && target.id().equals(here));
                player.resetFallDistance();
                enter(player, target, true);
            }
        }
    }

    /** Sends a player to a dimension by id. {@code arrivedByFall} puts them on the void floor; gates put them at the spawn point of that dimension. */
    public static void enter(ServerPlayer player, Dim dim, boolean arrivedByFall) {
        MinecraftServer server = player.level().getServer();
        ServerLevel target = server.getLevel(ResourceKey.create(Registries.DIMENSION, TheNothingMod.id(dim.id())));
        if (target == null) {
            TheNothingMod.LOGGER.warn("placeholder text", dim.id());
            return;
        }
        int floorY = dim.isVoid() ? VOID_FLOOR_Y : -1;
        BlockPos centre = dim.isVoid() ? new BlockPos(7, floorY, 7) : new BlockPos(0, floorY, 0);
        buildArrival(target, centre, dim);
        player.teleport(new TeleportTransition(target, new Vec3(centre.getX() + 0.5, floorY + 1.0, centre.getZ() + 0.5), Vec3.ZERO, player.getYRot(), 0.0f,
                TeleportTransition.DO_NOTHING));
        cooldown.put(player.getUUID(), (long) server.getTickCount() + 60);
        Compat.overlay(player, Component.literal(dim.name()));
        HauntEvents.desktopFile(player, 2); // the first time anyone goes through a gate or falls
    }

    /** True if this player just arrived somewhere and shouldn't be bounced by the gate they are standing next to. */
    public static boolean coolingDown(ServerPlayer player) {
        Long until = cooldown.get(player.getUUID());
        return until != null && until > player.level().getServer().getTickCount();
    }

    public static void enterById(ServerPlayer player, String id) {
        Dim dim = BY_ID.get(id);
        if (dim != null && !coolingDown(player)) {
            enter(player, dim, false);
        }
    }

    /** Takes the player to their own spawn (bed or world spawn), the way dying would. */
    public static void goHome(ServerPlayer player) {
        if (coolingDown(player)) {
            return;
        }
        TeleportTransition home = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        player.teleport(home);
        cooldown.put(player.getUUID(), (long) player.level().getServer().getTickCount() + 60);
    }

    /** A small floor (void dimensions) or just the Name 0440 (the others), so there is always a way back. */
    private static void buildArrival(ServerLevel level, BlockPos centre, Dim dim) {
        Block floor = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(TheNothingMod.id("id_0119"));
        if (dim.isVoid()) {
            // Make the surroundings first (the place this dimension is made of), then make sure there is room to stand.
            for (int cx = -1; cx <= 1; cx++) {
                for (int cz = -1; cz <= 1; cz++) {
                    VoidWorlds.ensure(level, cx, cz);
                }
            }
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = 1; y <= 4; y++) {
                        BlockPos above = centre.offset(x, y, z);
                        if (!level.getBlockState(above).isAir()) {
                            level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    BlockPos p = centre.offset(x, 0, z);
                    if (level.getBlockState(p).isAir()) {
                        level.setBlock(p, floor.defaultBlockState(), 3);
                    }
                }
            }
        }
        Block gate = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(TheNothingMod.id("id_0432"));
        BlockPos at = centre.offset(3, 1, 0);
        if (!level.getBlockState(at).is(gate)) {
            level.setBlock(at, gate.defaultBlockState(), 3);
            level.setBlock(at.above(), gate.defaultBlockState(), 3);
            level.setBlock(at.below(), dim.isVoid() ? floor.defaultBlockState() : Blocks.STONE.defaultBlockState(), 3);
        }
    }

    private static List<Dim> load() {
        List<Dim> list = new ArrayList<>();
        try (Reader reader = new InputStreamReader(
                Dimensions.class.getResourceAsStream("/the-nothing-mod.dimensions.json"), StandardCharsets.UTF_8)) {
            for (JsonElement element : new Gson().fromJson(reader, JsonElement.class).getAsJsonArray()) {
                JsonObject o = element.getAsJsonObject();
                list.add(new Dim(o.get("id").getAsString(), o.get("name").getAsString(), o.get("void").getAsBoolean(),
                        o.get("entry").getAsString(), o.get("top").getAsString()));
            }
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.error("placeholder text", e);
        }
        return List.copyOf(list);
    }
}
