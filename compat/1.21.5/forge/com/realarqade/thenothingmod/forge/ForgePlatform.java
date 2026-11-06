package com.realarqade.thenothingmod.forge;

import com.realarqade.thenothingmod.TheNothingMod;
import com.realarqade.thenothingmod.platform.Net;
import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
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
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadFlow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Forge's side of {@link Platform}. Game events go on Forge's event buses right away. Creative tab entries, attributes and network
 * payloads are collected here and handed over when Forge asks for them.
 */
public final class ForgePlatform implements Platform {
    private record TabHandler(ResourceKey<CreativeModeTab> tab, Consumer<CreativeModeTab.Output> handler) {
    }

    private record Attributes(EntityType<? extends LivingEntity> type, java.util.function.Supplier<AttributeSupplier.Builder> builder) {
    }

    /** One payload type; it may go to the client, to the server, or both. */
    private static final class PayloadEntry<T extends CustomPacketPayload> {
        final CustomPacketPayload.Type<T> type;
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;
        boolean toClient;
        BiConsumer<T, ServerPlayer> serverHandler;

        PayloadEntry(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            this.type = type;
            this.codec = codec;
        }

        @SuppressWarnings("unchecked")
        void register(PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> clientbound, PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> serverbound,
                      PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> bidirectional) {
            StreamCodec<RegistryFriendlyByteBuf, T> typed = (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
            BiConsumer<T, net.minecraftforge.event.network.CustomPayloadEvent.Context> handler = (payload, context) -> {
                if (context.isClientSide()) {
                    context.enqueueWork(() -> Net.deliverToClient(payload));
                } else if (serverHandler != null) {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        context.enqueueWork(() -> serverHandler.accept(payload, sender));
                    }
                }
            };
            if (toClient && serverHandler != null) {
                bidirectional.add(type, typed, handler);
            } else if (toClient) {
                clientbound.add(type, typed, handler);
            } else {
                serverbound.add(type, typed, handler);
            }
        }
    }

    private final List<TabHandler> tabs = new ArrayList<>();
    private final List<Attributes> attributes = new ArrayList<>();
    private final Map<CustomPacketPayload.Type<?>, PayloadEntry<?>> payloads = new LinkedHashMap<>();
    private Channel<CustomPacketPayload> channel;

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
        return !FMLEnvironment.production;
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.dist.isDedicatedServer();
    }

    @Override
    public <V, T extends V> T register(net.minecraft.core.Registry<V> registry, ResourceKey<V> key, T value) {
        pending.add(new Pending(registry.key(), key, value));
        return value;
    }

    private record Pending(ResourceKey<? extends net.minecraft.core.Registry<?>> registry, ResourceKey<?> key, Object value) {
    }

    private final List<Pending> pending = new ArrayList<>();

    /** Forge only unlocks one registry at a time, so the content is created now and handed over in each registry's own register event. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    void onRegister(net.minecraftforge.registries.RegisterEvent event) {
        for (Pending entry : pending) {
            if (entry.registry().equals(event.getRegistryKey())) {
                event.register((ResourceKey) entry.registry(), ((ResourceKey<?>) entry.key()).location(), () -> entry.value());
            }
        }
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> handler) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> handler.accept(event.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> handler) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> handler.accept(event.getServer()));
    }

    @Override
    public void onServerTickEnd(Consumer<MinecraftServer> handler) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) -> handler.accept(event.getServer()));
    }

    @Override
    public void onChunkGenerate(BiConsumer<ServerLevel, LevelChunk> handler) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.isNewChunk() && event.getChunk() instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level) {
                handler.accept(level, chunk);
            }
        });
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> handler) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
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
        attributes.add(new Attributes(type, () -> builder));
    }

    @Override
    public void registerAttributes(EntityType<? extends LivingEntity> type, java.util.function.Supplier<AttributeSupplier.Builder> builder) {
        attributes.add(new Attributes(type, builder));
    }

    @SuppressWarnings("unchecked")
    private <T extends CustomPacketPayload> PayloadEntry<T> entry(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return (PayloadEntry<T>) payloads.computeIfAbsent(type, key -> new PayloadEntry<>(type, codec));
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

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return channel != null && channel.isRemotePresent(player.connection.getConnection());
    }

    @Override
    public void send(ServerPlayer player, CustomPacketPayload payload) {
        if (channel != null) {
            channel.send(payload, PacketDistributor.PLAYER.with(player));
        }
    }

    /** Client to server. */
    public boolean canSendToServer() {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        return channel != null && connection != null && channel.isRemotePresent(connection.getConnection());
    }

    public void sendToServer(CustomPacketPayload payload) {
        if (channel != null) {
            channel.send(payload, PacketDistributor.SERVER.noArg());
        }
    }

    // ---- handed over by the mod bus ----
    void buildChannel() {
        var play = ChannelBuilder.named(TheNothingMod.id("main")).networkProtocolVersion(1).optional().payloadChannel().play();
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> clientbound = play.clientbound();
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> serverbound = play.serverbound();
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> bidirectional = play.bidirectional();
        for (PayloadEntry<?> entry : payloads.values()) {
            entry.register(clientbound, serverbound, bidirectional);
        }
        channel = serverbound.build();
    }

    void onBuildTab(BuildCreativeModeTabContentsEvent event) {
        for (TabHandler tab : tabs) {
            if (event.getTabKey().equals(tab.tab())) {
                tab.handler().accept(event);
            }
        }
    }

    void onAttributes(EntityAttributeCreationEvent event) {
        for (Attributes entry : attributes) {
            event.put(entry.type(), entry.builder().get().build());
        }
    }
}
