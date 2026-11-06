package com.realarqade.thenothingmod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.material.PushReaction;

/**
 * The few places where Minecraft's own names differ between versions. There is one copy of this file per Minecraft version, in
 * compat/<version>/java; the build picks the right one (see versions/versions.json), the rest of the mod never notices.
 */
public final class Compat {
    public static final EnumProperty<RedstoneSide> WIRE_NORTH = RedstoneWireBlock.NORTH;
    public static final EnumProperty<RedstoneSide> WIRE_EAST = RedstoneWireBlock.EAST;
    public static final EnumProperty<RedstoneSide> WIRE_SOUTH = RedstoneWireBlock.SOUTH;
    public static final EnumProperty<RedstoneSide> WIRE_WEST = RedstoneWireBlock.WEST;
    public static final IntegerProperty WIRE_POWER = RedstoneWireBlock.POWER;

    /** What a block that pops off when its support goes does to pistons. */
    public static final PushReaction PUSH_POPS = PushReaction.POPPED;

    /** Shows a short message above the hotbar. */
    public static void overlay(Player player, Component message) {
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(message);
        }
    }

    public static boolean hasTag(Entity entity, String tag) {
        return entity.entityTags().contains(tag);
    }

    public static int cx(ChunkPos pos) {
        return pos.x();
    }

    public static int cz(ChunkPos pos) {
        return pos.z();
    }

    public static ServerBossEvent newBossEvent(Component name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        return new ServerBossEvent(java.util.UUID.randomUUID(), name, color, overlay);
    }

    /** Moves a player to a position in another (or the same) dimension. */
    public static void teleport(ServerPlayer player, ServerLevel target, double x, double y, double z) {
        player.teleport(new net.minecraft.world.level.portal.TeleportTransition(target, new net.minecraft.world.phys.Vec3(x, y, z), net.minecraft.world.phys.Vec3.ZERO, player.getYRot(), 0.0f, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
    }

    /** Sends a player to their own spawn (bed or world spawn), the way dying would. */
    public static void sendHome(ServerPlayer player) {
        player.teleport(player.findRespawnPositionAndUseSpawnBlock(false, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
    }

    /** What EntityType.Builder#build wants as its id (a ResourceKey in newer versions, a String in 1.21.1). */
    public static net.minecraft.resources.ResourceKey<net.minecraft.world.entity.EntityType<?>> entityKey(net.minecraft.resources.ResourceKey<net.minecraft.world.entity.EntityType<?>> key) {
        return key;
    }

    private Compat() {
    }
}