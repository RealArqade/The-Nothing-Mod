package com.realarqade.thenothingmod.neoforge;

import com.mojang.serialization.MapCodec;
import com.realarqade.thenothingmod.client.ClientPlatform;
import com.realarqade.thenothingmod.client.DustTint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** NeoForge's client side of {@link ClientPlatform}. Registrations are collected and handed over when the mod-bus events fire. */
public final class NeoForgeClientPlatform implements ClientPlatform {
    private interface Renderer {
        void register(EntityRenderersEvent.RegisterRenderers event);
    }

    private record Tint(BlockColor source, Block block) {
    }

    private record Sprite(Identifier id, MapCodec<? extends SpriteSource> codec) {
    }

    private final List<Renderer> renderers = new ArrayList<>();
    private final List<Tint> tints = new ArrayList<>();
    private final List<Sprite> sprites = new ArrayList<>();

    @Override
    public <T extends Entity> void registerRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        renderers.add(event -> event.registerEntityRenderer(type, provider));
    }

    @Override
    public void registerTint(Block block) {
        tints.add(new Tint(new DustTint(), block));
    }

    @Override
    public void registerSpriteSource(Identifier id, MapCodec<? extends SpriteSource> codec) {
        sprites.add(new Sprite(id, codec));
    }

    @Override
    public void onScreenInit(BiConsumer<Minecraft, Screen> handler) {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> handler.accept(Minecraft.getInstance(), event.getScreen()));
    }

    @Override
    public boolean canSend(CustomPacketPayload.Type<?> type) {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(type);
    }

    @Override
    public void send(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        renderers.forEach(renderer -> renderer.register(event));
    }

    void onTints(RegisterColorHandlersEvent.Block event) {
        for (Tint tint : tints) {
            event.register(tint.source(), tint.block());
        }
    }

    void onSpriteSources(RegisterSpriteSourcesEvent event) {
        sprites.forEach(sprite -> event.register(sprite.id(), sprite.codec()));
    }
}
