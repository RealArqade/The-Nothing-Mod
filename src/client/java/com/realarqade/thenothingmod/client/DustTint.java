package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.ModBlocks;
import com.realarqade.thenothingmod.SuspiciousDustBlock;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;

/**
 * Tint index 0 of the dust models. The textures are grayscale, so this is where the colour comes from: the normal
 * colour, fading to the powered colour as the signal strength rises (only when the dust conducts redstone).
 * Both colours live in {@code the-nothing-mod.dust.json}.
 */
public final class DustTint implements BlockTintSource {
    @Override
    public int color(BlockState state) {
        return 0xFF000000 | ModBlocks.DUST_PROPERTIES.colorForPower(state.getValue(SuspiciousDustBlock.POWER));
    }

    /** Tells the renderer to redraw the block when the power changes, not just when the shape does. */
    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(SuspiciousDustBlock.POWER);
    }
}
