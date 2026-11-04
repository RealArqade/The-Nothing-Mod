package com.realarqade.thenothingmod.mixin;

import com.realarqade.thenothingmod.SuspiciousDustBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two small hooks into vanilla's redstone plumbing, both so the dust can work diagonally.
 * <ul>
 *   <li>{@link SignalGetter#getSignal(BlockPos, Direction)} is the one place every component (pistons, lamps, doors,
 *   dispensers, ...) goes through to ask about power. {@code Level} doesn't override it, so this adds an override that
 *   does exactly what vanilla does, plus the signal from dust diagonally next to the asking component
 *   ({@link SuspiciousDustBlock#diagonalSignal}).</li>
 *   <li>A lever only sends neighbour updates to its six face neighbours. This tells dust diagonal to a lever too
 *   ({@link SuspiciousDustBlock#notifyDiagonalInputChanged}).</li>
 * </ul>
 */
@Mixin(Level.class)
public abstract class LevelSignalMixin implements SignalGetter {
    @Override
    public int getSignal(BlockPos pos, Direction direction) {
        BlockState state = getBlockState(pos);
        int signal = state.getSignal(this, pos, direction);
        if (state.isRedstoneConductor(this, pos)) {
            signal = Math.max(signal, getDirectSignalTo(pos));
        }
        return Math.max(signal, SuspiciousDustBlock.diagonalSignal(this, pos, direction));
    }

    @Inject(method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;)V", at = @At("HEAD"))
    private void thenothingmod$updateDiagonalDust(BlockPos pos, Block block, Orientation orientation, CallbackInfo ci) {
        if (block == Blocks.LEVER) {
            SuspiciousDustBlock.notifyDiagonalInputChanged((Level) (Object) this, pos);
        }
    }
}
