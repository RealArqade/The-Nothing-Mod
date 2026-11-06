package com.realarqade.thenothingmod.client;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import com.realarqade.thenothingmod.platform.Payload;
import com.realarqade.thenothingmod.platform.PayloadType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.function.BiConsumer;

/** The client's counterpart of {@code Platform}: what the shared client code needs from the loader. */
public interface ClientPlatform {
    <T extends Entity> void registerRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider);

    void registerTint(Block block);

    void registerSpriteSource(ResourceLocation id, com.mojang.serialization.Codec<? extends SpriteSource> codec);

    /** Called every time a screen is set up (after its widgets exist). */
    void onScreenInit(BiConsumer<Minecraft, Screen> handler);

    /** Whether the server we are connected to can receive this payload. */
    boolean canSend(PayloadType<?> type);

    void send(Payload payload);

    final class Holder {
        private static ClientPlatform instance;

        private Holder() {
        }
    }

    static void set(ClientPlatform platform) {
        Holder.instance = platform;
    }

    static ClientPlatform get() {
        if (Holder.instance == null) {
            throw new IllegalStateException("The Nothing Mod: no client platform set yet");
        }
        return Holder.instance;
    }
}
