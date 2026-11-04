package com.realarqade.thenothingmod.neoforge;

import com.realarqade.thenothingmod.platform.Net;
import com.realarqade.thenothingmod.platform.Platform;
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
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * NeoForge's side of {@link Platform}. Game events go on NeoForge's event bus right away. Things that NeoForge only accepts at a set
 * moment (creative tabs, attributes, network payloads) are collected here and handed over when its mod-bus events fire.
 */
public final class NeoForgePlatform implements Platform {
    private record TabHandler(ResourceKey<CreativeModeTab> tab, Consumer<CreativeModeTab.Output> handler) {
    }

    private record Attributes(EntityType<? extends LivingEntity> type, AttributeSupplier.Builder builder) {
    }

    /** One payload type; it may go to the client, to the server, or both ways (NeoForge wants both ways registered in one go). */
    private static final class PayloadEntry<T extends CustomPacketPayload> {
        final CustomPacketPayload.Type<T> type;
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;
        boolean toClient;
        BiConsumer<T, ServerPlayer> serverHandler;

        PayloadEntry(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            this.type = type;
            this.codec = codec;
        }

        void register(PayloadRegistrar registrar) {
            IPayloadHandler<T> clientHandler = (payload, context) -> Net.deliverToClient(payload);
            IPayloadHandler<T> toServer = (payload, context) -> {
                if (serverHandler != null && context.player() instanceof ServerPlayer player) {
                    serverHandler.accept(payload, player);
                }
            };
            if (toClient && serverHandler != null) {
                registrar.playBidirectional(type, codec, clientHandler, toServer);
            } else if (toClient) {
                registrar.playToClient(type, codec, clientHandler);
            } else {
                registrar.playToServer(type, codec, toServer);
            }
        }
    }

    private final List<TabHandler> tabs = new ArrayList<>();
    private final List<Attributes> attributes = new ArrayList<>();
    private final java.util.Map<CustomPacketPayload.Type<?>, PayloadEntry<?>> payloads = new java.util.LinkedHashMap<>();

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.getDist().isDedicatedServer();
    }

    @Override
    public <V, T extends V> T register(net.minecraft.core.Registry<V> registry, ResourceKey<V> key, T value) {
        return net.minecraft.core.Registry.register(registry, key, value);
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> handler.accept(event.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> handler.accept(event.getServer()));
    }

    @Override
    public void onServerTickEnd(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> handler.accept(event.getServer()));
    }

    @Override
    public void onChunkGenerate(BiConsumer<ServerLevel, LevelChunk> handler) {
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.isNewChunk() && event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
                handler.accept(level, chunk);
            }
        });
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> handler) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                handler.accept(player);
            }
        });
    }

    @Override
    public void modifyCreativeTab(ResourceKey<CreativeModeTab> tab, Consumer<CreativeModeTab.Output> handler) {
        tabs.add(new TabHandler(tab, handler));
    }

    @Override
    public void registerAttributes(EntityType<? extends LivingEntity> type, AttributeSupplier.Builder builder) {
        attributes.add(new Attributes(type, builder));
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        entry(type, codec).toClient = true;
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                    BiConsumer<T, ServerPlayer> handler) {
        entry(type, codec).serverHandler = handler;
    }

    @SuppressWarnings("unchecked")
    private <T extends CustomPacketPayload> PayloadEntry<T> entry(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return (PayloadEntry<T>) payloads.computeIfAbsent(type, key -> new PayloadEntry<>(type, codec));
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection.hasChannel(type);
    }

    @Override
    public void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    // ---- handed over by the mod bus ----
    void onBuildTab(BuildCreativeModeTabContentsEvent event) {
        for (TabHandler tab : tabs) {
            if (event.getTabKey().equals(tab.tab())) {
                tab.handler().accept(event);
            }
        }
    }

    void onAttributes(EntityAttributeCreationEvent event) {
        for (Attributes entry : attributes) {
            event.put(entry.type(), entry.builder().build());
        }
    }

    void onPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (PayloadEntry<?> entry : payloads.values()) {
            entry.register(registrar);
        }
    }
}
