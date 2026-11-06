package com.realarqade.thenothingmod.client.fabric;

import com.mojang.serialization.MapCodec;
import com.realarqade.thenothingmod.client.ClientPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.BiConsumer;

public final class FabricClientPlatform implements ClientPlatform {
    @Override
    public <T extends Entity> void registerRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        EntityRendererRegistry.register(type, provider);
    }

    @Override
    public void registerTint(Block block) {
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(new com.realarqade.thenothingmod.client.DustTint(), block);
    }

    @Override
    public void registerSpriteSource(ResourceLocation id, MapCodec<? extends SpriteSource> codec) {
        com.realarqade.thenothingmod.client.mixin.SpriteSourcesAccessor.the_nothing_mod$types().put(id, new net.minecraft.client.renderer.texture.atlas.SpriteSourceType(codec));
    }

    @Override
    public void onScreenInit(BiConsumer<Minecraft, Screen> handler) {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> handler.accept(client, screen));
    }

    @Override
    public boolean canSend(CustomPacketPayload.Type<?> type) {
        return ClientPlayNetworking.canSend(type);
    }

    @Override
    public void send(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
