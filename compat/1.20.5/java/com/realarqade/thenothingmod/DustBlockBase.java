package com.realarqade.thenothingmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Base of the dust block: this Minecraft version's block hooks, forwarded to version-neutral methods. */
public abstract class DustBlockBase extends Block {
    protected DustBlockBase(Properties properties) {
        super(properties);
    }

    protected abstract void onDustRemoved(ServerLevel level, BlockPos pos, boolean movedByPiston);

    protected abstract BlockState dustUpdateShape(BlockState state, LevelReader level, BlockPos pos);

    protected abstract void dustNeighborChanged(BlockState state, Level level, BlockPos pos, boolean movedByPiston);

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            onDustRemoved(serverLevel, pos, movedByPiston);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dustUpdateShape(state, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        dustNeighborChanged(state, level, pos, movedByPiston);
    }

    protected static void neighborShapeChangedCompat(LevelAccessor level, Direction direction, BlockPos pos, BlockPos neighborPos, BlockState neighborState, int flags, int depth) {
        level.neighborShapeChanged(direction, neighborState, pos, neighborPos, flags, depth);
    }
}
