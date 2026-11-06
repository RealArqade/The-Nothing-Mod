package com.realarqade.thenothingmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Looks and connects like redstone dust. By default it is purely decorative: it never emits, receives or conducts a
 * signal. If {@link SuspiciousDustProperties#conductsRedstone()} is on, it carries a signal like vanilla dust instead.
 * It only connects to other dust of this same block, and can also link diagonally (see {@link SuspiciousDustProperties}).
 */
public class SuspiciousDustBlock extends DustBlockBase {
    public static final EnumProperty<RedstoneSide> NORTH = Compat.WIRE_NORTH;
    public static final EnumProperty<RedstoneSide> EAST = Compat.WIRE_EAST;
    public static final EnumProperty<RedstoneSide> SOUTH = Compat.WIRE_SOUTH;
    public static final EnumProperty<RedstoneSide> WEST = Compat.WIRE_WEST;
    /** Signal strength. Always 0 while the dust doesn't conduct redstone. */
    public static final IntegerProperty POWER = Compat.WIRE_POWER;
    public static final BooleanProperty NORTH_EAST = BooleanProperty.create("north_east");
    public static final BooleanProperty SOUTH_EAST = BooleanProperty.create("south_east");
    public static final BooleanProperty SOUTH_WEST = BooleanProperty.create("south_west");
    public static final BooleanProperty NORTH_WEST = BooleanProperty.create("north_west");

    private static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST);

    /** The four diagonal neighbours: the property that stores the link and the two straight directions that make it up. */
    private enum Diagonal {
        NORTH_EAST(SuspiciousDustBlock.NORTH_EAST, Direction.NORTH, Direction.EAST, Block.box(8, 0, 0, 16, 1, 8)),
        SOUTH_EAST(SuspiciousDustBlock.SOUTH_EAST, Direction.SOUTH, Direction.EAST, Block.box(8, 0, 8, 16, 1, 16)),
        SOUTH_WEST(SuspiciousDustBlock.SOUTH_WEST, Direction.SOUTH, Direction.WEST, Block.box(0, 0, 8, 8, 1, 16)),
        NORTH_WEST(SuspiciousDustBlock.NORTH_WEST, Direction.NORTH, Direction.WEST, Block.box(0, 0, 0, 8, 1, 8));

        final BooleanProperty property;
        final Direction first;
        final Direction second;
        final VoxelShape shape;

        Diagonal(BooleanProperty property, Direction first, Direction second, VoxelShape shape) {
            this.property = property;
            this.first = first;
            this.second = second;
            this.shape = shape;
        }

        BlockPos offset(BlockPos pos) {
            return pos.relative(first).relative(second);
        }

        Diagonal opposite() {
            return values()[(ordinal() + 2) % 4];
        }
    }

    private static final VoxelShape DOT = Block.box(3, 0, 3, 13, 1, 13);
    private static final Map<Direction, VoxelShape> SIDE_SHAPES = Map.of(
            Direction.NORTH, Block.box(3, 0, 0, 13, 1, 13),
            Direction.SOUTH, Block.box(3, 0, 3, 13, 1, 16),
            Direction.EAST, Block.box(3, 0, 3, 16, 1, 13),
            Direction.WEST, Block.box(0, 0, 3, 13, 1, 13));
    private static final Map<Direction, VoxelShape> UP_SHAPES = Map.of(
            Direction.NORTH, Shapes.or(SIDE_SHAPES.get(Direction.NORTH), Block.box(3, 0, 0, 13, 16, 1)),
            Direction.SOUTH, Shapes.or(SIDE_SHAPES.get(Direction.SOUTH), Block.box(3, 0, 15, 13, 16, 16)),
            Direction.EAST, Shapes.or(SIDE_SHAPES.get(Direction.EAST), Block.box(15, 0, 3, 16, 16, 13)),
            Direction.WEST, Shapes.or(SIDE_SHAPES.get(Direction.WEST), Block.box(0, 0, 3, 1, 16, 13)));

    private final SuspiciousDustProperties dustProperties;
    /**
     * Same trick as vanilla wire: switched off while reading the neighbours' signal so we don't read our own.
     * Id0651 because the diagonal signal hook ({@link #diagonalSignal}) needs to see it too.
     */
    private static boolean shouldSignal = true;

    public SuspiciousDustBlock(Properties properties, SuspiciousDustProperties dustProperties) {
        super(properties);
        this.dustProperties = dustProperties;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(WEST, RedstoneSide.NONE)
                .setValue(NORTH_EAST, false)
                .setValue(SOUTH_EAST, false)
                .setValue(SOUTH_WEST, false)
                .setValue(NORTH_WEST, false)
                .setValue(POWER, 0));
    }

    public SuspiciousDustProperties dustProperties() {
        return dustProperties;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST, POWER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = DOT;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide side = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (side == RedstoneSide.SIDE) {
                shape = Shapes.or(shape, SIDE_SHAPES.get(direction));
            } else if (side == RedstoneSide.UP) {
                shape = Shapes.or(shape, UP_SHAPES.get(direction));
            }
        }
        for (Diagonal diagonal : Diagonal.values()) {
            if (state.getValue(diagonal.property)) {
                shape = Shapes.or(shape, diagonal.shape);
            }
        }
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return logged(context.getClickedPos(), getConnectionState(context.getLevel(), defaultBlockState(), context.getClickedPos()));
    }

    private static BlockState logged(BlockPos pos, BlockState state) {
        if (DustDebug.logConnections) {
            TheNothingMod.LOGGER.info("placeholder text", pos.toShortString(), state);
        }
        return state;
    }

    @Override
    protected BlockState dustUpdateShape(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return logged(pos, getConnectionState(level, state, pos));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    // Vanilla only tells the six face neighbours when a block changes. Diagonal dust has to be told by hand,
    // otherwise a diagonal link would only appear once something else nudged the other piece.
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        refreshDiagonalNeighbors(level, pos);
        if (!level.isClientSide() && dustProperties.conductsRedstone()) {
            updatePowerStrength(level, pos, state);
            notifyAround(level, pos);
        }
    }

    @Override
    protected void onDustRemoved(ServerLevel level, BlockPos pos, boolean movedByPiston) {
        refreshDiagonalNeighbors(level, pos);
        if (dustProperties.conductsRedstone() && !movedByPiston) {
            notifyAround(level, pos); // neighbours work out their signal again, now without us
            updateDiagonalPower(level, pos);
        }
    }

    private void refreshDiagonalNeighbors(Level level, BlockPos pos) {
        if (level.isClientSide() || !dustProperties.connectDiagonally()) {
            return;
        }
        for (Diagonal diagonal : Diagonal.values()) {
            BlockPos otherPos = diagonal.offset(pos);
            BlockState other = level.getBlockState(otherPos);
            if (other.is(this)) {
                BlockState updated = getConnectionState(level, other, otherPos);
                if (updated != other) {
                    level.setBlock(otherPos, updated, Block.UPDATE_CLIENTS);
                    if (dustProperties.conductsRedstone()) {
                        updatePowerStrength(level, otherPos, updated);
                    }
                }
            }
        }
    }

    // --- Redstone (only does anything while SuspiciousDustProperties.conductsRedstone() is on) ---

    @Override
    protected void dustNeighborChanged(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!level.isClientSide() && dustProperties.conductsRedstone()) {
            updatePowerStrength(level, pos, state);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return shouldSignal && dustProperties.conductsRedstone();
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return shouldSignal ? getSignal(state, level, pos, direction) : 0;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!shouldSignal || !dustProperties.conductsRedstone() || direction == Direction.DOWN) {
            return 0;
        }
        int power = state.getValue(POWER);
        if (power == 0) {
            return 0;
        }
        // Like vanilla dust, it only powers the sides it points at (and whatever is above).
        boolean pointsAtAsker = getConnectionState(level, state, pos).getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite())).isConnected();
        return direction == Direction.UP || pointsAtAsker ? power : 0;
    }

    private void updatePowerStrength(Level level, BlockPos pos, BlockState state) {
        int target = calculateTargetStrength(level, pos);
        if (state.getValue(POWER) == target) {
            return;
        }
        if (level.getBlockState(pos) == state) {
            level.setBlock(pos, state.setValue(POWER, target), Block.UPDATE_CLIENTS);
        }
        notifyAround(level, pos);
        updateDiagonalPower(level, pos);
    }

    /** The strongest signal reaching this dust: from redstone components next to it, or from neighbouring dust minus one. */
    private int calculateTargetStrength(Level level, BlockPos pos) {
        shouldSignal = false;
        int external = Math.max(level.getBestNeighborSignal(pos), diagonalInputSignal(level, pos));
        shouldSignal = true;

        int fromDust = 0;
        if (external < 15) {
            BlockPos abovePos = pos.above();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos sidePos = pos.relative(direction);
                BlockState sideState = level.getBlockState(sidePos);
                fromDust = Math.max(fromDust, dustPower(sideState));
                if (sideState.isRedstoneConductor(level, sidePos) && !level.getBlockState(abovePos).isRedstoneConductor(level, abovePos)) {
                    fromDust = Math.max(fromDust, dustPower(level.getBlockState(sidePos.above())));
                } else if (!sideState.isRedstoneConductor(level, sidePos)) {
                    fromDust = Math.max(fromDust, dustPower(level.getBlockState(sidePos.below())));
                }
            }
            if (dustProperties.connectDiagonally()) {
                for (Diagonal diagonal : Diagonal.values()) {
                    if (isDiagonalConnected(level, pos, diagonal)) {
                        fromDust = Math.max(fromDust, dustPower(level.getBlockState(diagonal.offset(pos))));
                    }
                }
            }
        }
        return Math.max(external, fromDust - 1);
    }

    private int dustPower(BlockState state) {
        return state.is(this) ? state.getValue(POWER) : 0;
    }

    /** Tells this position and everything around it that the signal changed, so dust can pass through blocks like vanilla. */
    private void notifyAround(Level level, BlockPos pos) {
        Set<BlockPos> positions = new HashSet<>();
        positions.add(pos);
        for (Direction direction : Direction.values()) {
            positions.add(pos.relative(direction));
        }
        for (BlockPos position : positions) {
            level.updateNeighborsAt(position, this);
        }
        notifyDiagonalComponents(level, pos);
    }

    /**
     * Pistons, lamps and friends only get a "neighbour changed" for their six face neighbours, so a component diagonally
     * next to the dust has to be told by hand that its power may have changed.
     */
    private void notifyDiagonalComponents(Level level, BlockPos pos) {
        if (!dustProperties.powersDiagonally()) {
            return;
        }
        for (Diagonal diagonal : Diagonal.values()) {
            BlockPos componentPos = diagonal.offset(pos);
            if (!level.getBlockState(componentPos).is(this)) {
                level.neighborChanged(componentPos, this, null);
            }
        }
    }

    /**
     * The signal powered dust gives to a component diagonally next to it. Called from {@code LevelSignalMixin}, which
     * is how vanilla asks "placeholder text": it only ever asks about the six face neighbours, as
     * {@code getSignal(neighbour, direction)}. The dust diagonal to the asking component is a neighbour of that
     * neighbour, sideways, so this looks there.
     *
     * @param pos       the face neighbour being asked about
     * @param direction from the asking component towards {@code pos}
     */
    public static int diagonalSignal(BlockGetter level, BlockPos pos, Direction direction) {
        if (!shouldSignal || direction.getAxis().isVertical()) {
            return 0;
        }
        int best = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side.getAxis() == direction.getAxis()) {
                continue;
            }
            BlockState state = level.getBlockState(pos.relative(side));
            if (state.getBlock() instanceof SuspiciousDustBlock dust) {
                best = Math.max(best, dust.signalToDiagonal(state, direction, side));
            }
        }
        return best;
    }

    /**
     * The asking component sits behind the asked block (opposite {@code askDirection}) and behind this dust (opposite
     * {@code dustSide}). Dust that already points along either of those two sides keeps its signal for that side, the
     * way vanilla dust doesn't power blocks beside a straight line.
     */
    private int signalToDiagonal(BlockState state, Direction askDirection, Direction dustSide) {
        if (!dustProperties.conductsRedstone() || !dustProperties.powersDiagonally()) {
            return 0;
        }
        int power = state.getValue(POWER);
        if (power == 0) {
            return 0;
        }
        boolean pointsAlongOneSide = state.getValue(PROPERTY_BY_DIRECTION.get(dustSide.getOpposite())).isConnected()
                || state.getValue(PROPERTY_BY_DIRECTION.get(askDirection.getOpposite())).isConnected();
        return pointsAlongOneSide ? 0 : power;
    }

    /** Diagonal dust isn't a face neighbour, so vanilla's neighbour updates never reach it. */
    private void updateDiagonalPower(Level level, BlockPos pos) {
        if (!dustProperties.connectDiagonally()) {
            return;
        }
        for (Diagonal diagonal : Diagonal.values()) {
            BlockPos otherPos = diagonal.offset(pos);
            BlockState other = level.getBlockState(otherPos);
            if (other.is(this)) {
                updatePowerStrength(level, otherPos, other);
            }
        }
    }

    /** Lets dust one block up or down (on slopes) refresh its shape when this dust changes. Mirrors vanilla wire. */
    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int depth) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) == RedstoneSide.NONE
                    || level.getBlockState(mutable.setWithOffset(pos, direction)).is(this)) {
                continue;
            }
            mutable.move(Direction.DOWN);
            notifyDust(level, mutable, direction, flags, depth);
            mutable.setWithOffset(pos, direction).move(Direction.UP);
            notifyDust(level, mutable, direction, flags, depth);
        }
    }

    private void notifyDust(LevelAccessor level, BlockPos.MutableBlockPos dustPos, Direction direction, int flags, int depth) {
        if (level.getBlockState(dustPos).is(this)) {
            BlockPos towardsUs = dustPos.relative(direction.getOpposite());
            neighborShapeChangedCompat(level, direction.getOpposite(), dustPos, towardsUs, level.getBlockState(towardsUs), flags, depth);
        }
    }

    private BlockState getConnectionState(BlockGetter level, BlockState state, BlockPos pos) {
        int connections = 0;
        Direction loneStraight = null;
        Diagonal loneDiagonal = null;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide side = getConnectingSide(level, pos, direction);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), side);
            if (side != RedstoneSide.NONE) {
                connections++;
                loneStraight = direction;
            }
        }
        for (Diagonal diagonal : Diagonal.values()) {
            boolean connected = dustProperties.connectDiagonally() && isDiagonalConnected(level, pos, diagonal);
            state = state.setValue(diagonal.property, connected);
            if (connected) {
                connections++;
                loneDiagonal = diagonal;
            }
        }

        // A lone connection draws a straight line through the block, like redstone does.
        if (connections == 1) {
            if (loneStraight != null) {
                state = state.setValue(PROPERTY_BY_DIRECTION.get(loneStraight.getOpposite()), RedstoneSide.SIDE);
            } else {
                state = state.setValue(loneDiagonal.opposite().property, true);
            }
        }
        // Switched redstone off while dust is still powered? Drop the leftover power so it stops glowing.
        if (!dustProperties.conductsRedstone() && state.getValue(POWER) != 0) {
            state = state.setValue(POWER, 0);
        }
        return state;
    }

    private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos sidePos = pos.relative(direction);
        BlockState sideState = level.getBlockState(sidePos);
        BlockPos abovePos = pos.above();
        boolean roomAbove = !level.getBlockState(abovePos).isRedstoneConductor(level, abovePos);

        if (roomAbove && sideState.isFaceSturdy(level, sidePos, Direction.UP) && level.getBlockState(sidePos.above()).is(this)) {
            return sideState.isFaceSturdy(level, sidePos, direction.getOpposite()) ? RedstoneSide.UP : RedstoneSide.SIDE;
        }
        if (sideState.is(this)) {
            return RedstoneSide.SIDE;
        }
        if (!sideState.isRedstoneConductor(level, sidePos) && level.getBlockState(sidePos.below()).is(this)) {
            return RedstoneSide.SIDE;
        }
        return RedstoneSide.NONE;
    }

    /**
     * Links to dust on a diagonal, unless one of the two blocks between us is dust already (that would be a normal
     * straight connection). The check is the same from both ends, so both pieces always agree.
     */
    private boolean isDiagonalConnected(BlockGetter level, BlockPos pos, Diagonal diagonal) {
        BlockState target = level.getBlockState(diagonal.offset(pos));
        boolean linkable = target.is(this) || (dustProperties.conductsRedstone() && isDiagonalInput(target));
        return linkable
                && !level.getBlockState(pos.relative(diagonal.first)).is(this)
                && !level.getBlockState(pos.relative(diagonal.second)).is(this);
    }

    // --- Diagonal inputs: a lever diagonally next to the dust draws a diagonal link and powers it ---

    /** Redstone components that can power the dust from a diagonal. To allow more, add them here and in {@link #diagonalInputSignal}. */
    private static boolean isDiagonalInput(BlockState state) {
        return state.is(Blocks.LEVER);
    }

    private int diagonalInputSignal(BlockGetter level, BlockPos pos) {
        if (!dustProperties.connectDiagonally()) {
            return 0;
        }
        for (Diagonal diagonal : Diagonal.values()) {
            BlockState input = level.getBlockState(diagonal.offset(pos));
            if (isDiagonalInput(input) && input.getValue(BlockStateProperties.POWERED) && isDiagonalConnected(level, pos, diagonal)) {
                return 15;
            }
        }
        return 0;
    }

    /**
     * Called (from {@code LevelSignalMixin}) when a lever sends its neighbour updates. Levers only tell their six face
     * neighbours, so dust diagonal to one has to be told by hand to redraw its link and re-read its power.
     */
    public static void notifyDiagonalInputChanged(Level level, BlockPos inputPos) {
        if (level.isClientSide()) {
            return;
        }
        for (Diagonal diagonal : Diagonal.values()) {
            BlockPos dustPos = diagonal.offset(inputPos);
            if (level.getBlockState(dustPos).getBlock() instanceof SuspiciousDustBlock dust) {
                dust.refreshAfterInputChange(level, dustPos);
            }
        }
    }

    private void refreshAfterInputChange(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (dustProperties.connectDiagonally()) {
            BlockState updated = getConnectionState(level, state, pos);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
        if (dustProperties.conductsRedstone()) {
            updatePowerStrength(level, pos, level.getBlockState(pos));
        }
    }
}
