package com.realarqade.thenothingmod.forge;

import com.mojang.serialization.MapCodec;
import com.realarqade.thenothingmod.TheNothingMod;
import com.realarqade.thenothingmod.client.ClientPlatform;
import com.realarqade.thenothingmod.client.DustTint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Forge's client side of {@link ClientPlatform}. Only ever loaded on the client. */
final class ForgeClientPlatform implements ClientPlatform {
    private interface Renderer {
        void register(EntityRenderersEvent.RegisterRenderers event);
    }

    private record Tint(BlockTintSource source, Block block) {
    }

    private final List<Renderer> renderers = new ArrayList<>();
    private final List<Tint> tints = new ArrayList<>();
    private final ForgePlatform platform;

    ForgeClientPlatform(ForgePlatform platform) {
        this.platform = platform;
    }

    @Override
    public <T extends Entity> void registerRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        renderers.add(event -> event.registerEntityRenderer(type, provider));
    }

    @Override
    public void registerTint(Block block) {
        tints.add(new Tint(new DustTint(), block));
    }

    /** Forge has no event for sprite sources, so the type is added to the game's own table of them (before the first atlas is built). */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerSpriteSource(Identifier id, MapCodec<? extends SpriteSource> codec) {
        try {
            Class<?> sources = Class.forName("net.minecraft.client.renderer.texture.atlas.SpriteSources");
            Field field = sources.getDeclaredField("ID_MAPPER");
            field.setAccessible(true);
            Object mapper = field.get(null);
            mapper.getClass().getMethod("put", Object.class, Object.class).invoke(mapper, id, codec);
        } catch (ReflectiveOperationException | RuntimeException e) {
            TheNothingMod.LOGGER.error("[The Nothing Mod] Could not register the dust texture source; the dust will look wrong.", e);
        }
    }

    @Override
    public void onScreenInit(BiConsumer<Minecraft, Screen> handler) {
        ScreenEvent.Init.Post.BUS.addListener(event -> handler.accept(Minecraft.getInstance(), event.getScreen()));
    }

    @Override
    public boolean canSend(CustomPacketPayload.Type<?> type) {
        return platform.canSendToServer();
    }

    @Override
    public void send(CustomPacketPayload payload) {
        platform.sendToServer(payload);
    }

    void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        renderers.forEach(renderer -> renderer.register(event));
    }

    void onTints(RegisterColorHandlersEvent.Block event) {
        for (Tint tint : tints) {
            event.register(List.of(tint.source()), tint.block());
        }
    }
}
