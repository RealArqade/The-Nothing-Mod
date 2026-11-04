package com.realarqade.thenothingmod.client.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** This Fabric API has no way to add a texture-atlas source type, so the vanilla registry is reached through Mixin (remapped by Loom). */
@Mixin(SpriteSources.class)
public interface SpriteSourcesAccessor {
    @Accessor("ID_MAPPER")
    static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpriteSource>> the_nothing_mod$idMapper() {
        throw new AssertionError();
    }
}