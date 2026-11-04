package com.realarqade.thenothingmod.client;

import com.mojang.serialization.MapCodec;
import com.realarqade.thenothingmod.TheNothingMod;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Feeds the dust textures into the block atlas, from {@code textures/dust_hi} or {@code textures/dust_lo} depending on
 * the config. Both folders hold the same file names, and either way they become the sprites {@code block/dust_*}, which
 * is what the models refer to.
 * <p>
 * The atlas runs its sources again on every resource reload, so the choice is made fresh each time. That is why switching
 * the setting only needs a reload (the config screen triggers one), not a game restart. It is hooked in through
 * {@code assets/minecraft/atlases/blocks.json}.
 */
public final class DustTextureSource implements SpriteSource {
    public static final Identifier ID = TheNothingMod.id("dust_textures");
    public static final MapCodec<DustTextureSource> CODEC = MapCodec.unit(DustTextureSource::new);

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        String folder = DustConfig.get().highResTextures ? "dust_hi" : "dust_lo";
        new DirectoryLister(folder, "block/").run(resourceManager, output);
    }

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return CODEC;
    }
}
