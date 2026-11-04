package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.ModBlocks;
import com.realarqade.thenothingmod.SuspiciousDustBlock;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Tint index 0 of the dust models (1.21.11 form: a BlockColor). Same colours as the newer BlockTintSource version. */
public final class DustTint implements BlockColor {
    @Override
    public int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        return 0xFF000000 | ModBlocks.DUST_PROPERTIES.colorForPower(state.getValue(SuspiciousDustBlock.POWER));
    }
}