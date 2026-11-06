package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.TheNothingMod;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.resources.ResourceLocation;
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
    public static final ResourceLocation ID = TheNothingMod.id("dust_textures");
    public static final com.mojang.serialization.Codec<DustTextureSource> CODEC = com.mojang.serialization.Codec.unit(DustTextureSource::new);
    public static final net.minecraft.client.renderer.texture.atlas.SpriteSourceType TYPE = new net.minecraft.client.renderer.texture.atlas.SpriteSourceType(CODEC);

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        String folder = DustConfig.get().highResTextures ? "dust_hi" : "dust_lo";
        new DirectoryLister(folder, "block/").run(resourceManager, output);
    }

    @Override
    public net.minecraft.client.renderer.texture.atlas.SpriteSourceType type() {
        return TYPE;
    }
}
