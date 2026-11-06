package com.realarqade.thenothingmod.fabric;

import com.realarqade.thenothingmod.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
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

public final class FabricPlatform implements Platform {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public <V, T extends V> T register(net.minecraft.core.Registry<V> registry, ResourceKey<V> key, T value) {
        return net.minecraft.core.Registry.register(registry, key, value);
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> handler) {
        ServerLifecycleEvents.SERVER_STARTED.register(handler::accept);
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> handler) {
        ServerLifecycleEvents.SERVER_STOPPED.register(handler::accept);
    }

    @Override
    public void onServerTickEnd(Consumer<MinecraftServer> handler) {
        ServerTickEvents.END_SERVER_TICK.register(handler::accept);
    }

    @Override
    public void onChunkGenerate(BiConsumer<ServerLevel, LevelChunk> handler) {
        ServerChunkEvents.CHUNK_GENERATE.register(handler::accept);
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> handler) {
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> handler.accept(listener.getPlayer()));
    }

    @Override
    public void modifyCreativeTab(ResourceKey<CreativeModeTab> tab, Consumer<CreativeModeTab.Output> handler) {
        ItemGroupEvents.modifyEntriesEvent(tab).register(handler::accept);
    }

    @Override
    public void registerAttributes(EntityType<? extends LivingEntity> type, AttributeSupplier.Builder attributes) {
        FabricDefaultAttributeRegistry.register(type, attributes);
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playS2C().register(type, codec);
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                    BiConsumer<T, ServerPlayer> handler) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(payload, context.player()));
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ServerPlayNetworking.canSend(player, type);
    }

    @Override
    public void send(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
