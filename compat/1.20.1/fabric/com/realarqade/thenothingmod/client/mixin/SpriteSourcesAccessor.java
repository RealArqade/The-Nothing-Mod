package com.realarqade.thenothingmod.client.mixin;

import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;
import com.google.common.collect.BiMap;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** This Fabric API has no way to add a texture-atlas source type, so the vanilla registry is reached through Mixin (remapped by Loom). */
@Mixin(SpriteSources.class)
public interface SpriteSourcesAccessor {
    @Accessor("TYPES")
    static BiMap<ResourceLocation, SpriteSourceType> the_nothing_mod$types() {
        throw new AssertionError();
    }
}