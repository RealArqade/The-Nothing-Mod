package com.realarqade.thenothingmod.neoforge;

import com.realarqade.thenothingmod.platform.Net;
import com.realarqade.thenothingmod.platform.Platform;
import com.realarqade.thenothingmod.platform.Payload;
import com.realarqade.thenothingmod.platform.PayloadType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

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

    /** The vanilla-facing message: wraps one of the mod's payloads so NeoForge can write it and tell what it is. */
    record Wrapper(Payload inner) implements CustomPacketPayload {
        @Override
        public void write(FriendlyByteBuf buf) {
            inner.write(buf);
        }

        @Override
        public ResourceLocation id() {
            return inner.payloadId();
        }
    }

    /** One payload type; it may go to the client, to the server, or both ways. */
    private static final class PayloadEntry<T extends Payload> {
        final PayloadType<T> type;
        boolean toClient;
        BiConsumer<T, ServerPlayer> serverHandler;

        PayloadEntry(PayloadType<T> type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        void register(IPayloadRegistrar registrar) {
            IPlayPayloadHandler<Wrapper> handler = (Wrapper payload, PlayPayloadContext context) -> {
                if (context.flow() == net.minecraft.network.protocol.PacketFlow.CLIENTBOUND) {
                    context.workHandler().execute(() -> Net.deliverToClient(payload.inner()));
                } else if (serverHandler != null && context.player().orElse(null) instanceof ServerPlayer player) {
                    context.workHandler().execute(() -> serverHandler.accept((T) payload.inner(), player));
                }
            };
            registrar.play(type.id(), buf -> new Wrapper(type.reader().apply(buf)), handler);
        }
    }
    private final List<TabHandler> tabs = new ArrayList<>();
    private final List<Attributes> attributes = new ArrayList<>();
    private final java.util.Map<ResourceLocation, PayloadEntry<?>> payloads = new java.util.LinkedHashMap<>();

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
        NeoForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> { if (event.phase == TickEvent.Phase.END) { handler.accept(event.getServer()); } });
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
    public <T extends Payload> void registerClientbound(PayloadType<T> type, PayloadType<T> codec) {
        entry(type).toClient = true;
    }

    @Override
    public <T extends Payload> void registerServerbound(PayloadType<T> type, PayloadType<T> codec, BiConsumer<T, ServerPlayer> handler) {
        entry(type).serverHandler = handler;
    }

    @SuppressWarnings("unchecked")
    private <T extends Payload> PayloadEntry<T> entry(PayloadType<T> type) {
        return (PayloadEntry<T>) payloads.computeIfAbsent(type.id(), key -> new PayloadEntry<>(type));
    }

    @Override
    public boolean canSend(ServerPlayer player, PayloadType<?> type) {
        return player.connection.isConnected(type.id());
    }

    @Override
    public void send(ServerPlayer player, Payload payload) {
        PacketDistributor.PLAYER.with(player).send(new Wrapper(payload));
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

    void onPayloads(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(com.realarqade.thenothingmod.TheNothingMod.MOD_ID).versioned("1");
        for (PayloadEntry<?> entry : payloads.values()) {
            entry.register(registrar);
        }
    }
}
