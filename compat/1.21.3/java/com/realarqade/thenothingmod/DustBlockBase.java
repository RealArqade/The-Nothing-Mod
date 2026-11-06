package com.realarqade.thenothingmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

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
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return dustUpdateShape(state, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean movedByPiston) {
        dustNeighborChanged(state, level, pos, movedByPiston);
    }

    protected static void neighborShapeChangedCompat(LevelAccessor level, Direction direction, BlockPos pos, BlockPos neighborPos, BlockState neighborState, int flags, int depth) {
        level.neighborShapeChanged(direction, pos, neighborPos, neighborState, flags, depth);
    }
}
