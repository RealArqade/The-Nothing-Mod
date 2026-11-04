package com.realarqade.thenothingmod.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.chunk.LevelChunk;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Everything the shared code needs from the mod loader (Fabric, NeoForge or Forge). Each loader provides one implementation and sets it
 * with {@link #set} before anything else runs. The shared code never imports a loader's API directly.
 */
public interface Platform {
    Path configDir();

    Path gameDir();

    /** True when running from a development environment (an IDE run), false for a normal install. */
    boolean isDevelopment();

    /** True on a dedicated server (no client code at all). */
    boolean isDedicatedServer();

    // ---- events (all run on the server thread) ----
    void onServerStarted(Consumer<MinecraftServer> handler);

    void onServerStopped(Consumer<MinecraftServer> handler);

    void onServerTickEnd(Consumer<MinecraftServer> handler);

    void onChunkGenerate(BiConsumer<ServerLevel, LevelChunk> handler);

    void onPlayerJoin(Consumer<ServerPlayer> handler);

    // ---- content ----
    /** Registers a block, item, entity type... and returns it. Some loaders only accept this at a set moment and take it then. */
    <V, T extends V> T register(net.minecraft.core.Registry<V> registry, ResourceKey<V> key, T value);

    // ---- items, entities ----
    /** Adds items to a vanilla creative tab. */
    void modifyCreativeTab(ResourceKey<CreativeModeTab> tab, Consumer<CreativeModeTab.Output> handler);

    void registerAttributes(EntityType<? extends LivingEntity> type, AttributeSupplier.Builder attributes);

    // ---- networking ----
    <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

    /** Registers a payload that clients may send, and what the server does when one arrives. */
    <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                             BiConsumer<T, ServerPlayer> handler);

    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

    void send(ServerPlayer player, CustomPacketPayload payload);

    // ---- holder ----
    final class Holder {
        private static Platform instance;

        private Holder() {
        }
    }

    static void set(Platform platform) {
        Holder.instance = platform;
    }

    static Platform get() {
        if (Holder.instance == null) {
            throw new IllegalStateException("The Nothing Mod: no platform set yet (the loader entrypoint must call Platform.set first)");
        }
        return Holder.instance;
    }
}
