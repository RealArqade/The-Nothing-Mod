package com.realarqade.thenothingmod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A gate. Walk into it and you are taken to the dimension it belongs to (see {@link Dimensions}); the Name 0440 ({@code target}
 * null) takes you to your own spawn. It has no collision, so you step straight through.
 */
public class ThresholdBlock extends Block {
    private final String target;

    /** @param target the dimension id this gate leads to, or null for the Name 0440 */
    public ThresholdBlock(Properties properties, String target) {
        super(properties);
        this.target = target;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (target == null) {
            Dimensions.goHome(player);
        } else {
            Dimensions.enterById(player, target);
        }
    }
}
