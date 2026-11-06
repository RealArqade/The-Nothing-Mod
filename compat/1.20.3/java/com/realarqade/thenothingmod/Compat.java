package com.realarqade.thenothingmod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.material.PushReaction;

/**
 * The few places where Minecraft's own names differ between versions. There is one copy of this file per Minecraft version, in
 * compat/<version>/java; the build picks the right one (see versions/versions.json), the rest of the mod never notices.
 */
public final class Compat {
    public static final EnumProperty<RedstoneSide> WIRE_NORTH = RedStoneWireBlock.NORTH;
    public static final EnumProperty<RedstoneSide> WIRE_EAST = RedStoneWireBlock.EAST;
    public static final EnumProperty<RedstoneSide> WIRE_SOUTH = RedStoneWireBlock.SOUTH;
    public static final EnumProperty<RedstoneSide> WIRE_WEST = RedStoneWireBlock.WEST;
    public static final IntegerProperty WIRE_POWER = RedStoneWireBlock.POWER;

    /** What a block that pops off when its support goes does to pistons. */
    public static final PushReaction PUSH_POPS = PushReaction.DESTROY;

    /** Shows a short message above the hotbar. */
    public static void overlay(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static boolean hasTag(Entity entity, String tag) {
        return entity.getTags().contains(tag);
    }

    public static int cx(ChunkPos pos) {
        return pos.x;
    }

    public static int cz(ChunkPos pos) {
        return pos.z;
    }

    public static ServerBossEvent newBossEvent(Component name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        return new ServerBossEvent(name, color, overlay);
    }

    /** Moves a player to a position in another (or the same) dimension. */
    public static void teleport(ServerPlayer player, ServerLevel target, double x, double y, double z) {
        player.teleportTo(target, x, y, z, player.getYRot(), 0.0f);
    }

    /** Sends a player to their own spawn (bed or world spawn), the way dying would. */
    public static void sendHome(ServerPlayer player) {
        ServerLevel level = player.server.getLevel(player.getRespawnDimension());
        if (level == null) {
            level = player.server.overworld();
        }
        BlockPos pos = player.getRespawnPosition();
        if (pos == null) {
            pos = level.getSharedSpawnPos();
        }
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getRespawnAngle(), 0.0f);
    }

    /** What EntityType.Builder#build wants as its id (a ResourceKey in newer versions, a String in 1.21.1). */
    public static String entityKey(net.minecraft.resources.ResourceKey<net.minecraft.world.entity.EntityType<?>> key) {
        return key.location().getPath();
    }

    private Compat() {
    }
}