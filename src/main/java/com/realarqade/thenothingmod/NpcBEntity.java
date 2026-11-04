package com.realarqade.thenothingmod;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/**
 * Walks from light to light and puts them out: torches and lanterns, one a second once it reaches them. It doesn't care about
 * you, and leaves if you get right up to it (or after about two minutes). See {@link ApparitionProperties}. Summon one with
 * {@code /summon the-nothing-mod:id_0487}.
 */
public class NpcBEntity extends PathfinderMob implements Apparition {
    /** The blocks it goes for. Add to this to make it go after more. */
    private static final Set<Block> LIGHTS = Set.of(
            Blocks.TORCH, Blocks.WALL_TORCH,
            Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
            Blocks.LANTERN, Blocks.SOUL_LANTERN);

    public NpcBEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, ApparitionProperties.LAMPLIGHTER_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 20 != 0) {
            return;
        }
        Player nearest = level().getNearestPlayer(this, 160.0);
        if (nearest == null
                || tickCount > ApparitionProperties.LAMPLIGHTER_LIFETIME_TICKS
                || nearest.distanceTo(this) < ApparitionProperties.LAMPLIGHTER_VANISH_DISTANCE) {
            discard();
            return;
        }

        BlockPos light = findNearestLight();
        if (light == null) {
            getNavigation().stop();
        } else if (light.distSqr(blockPosition()) <= 6.0) {
            level().removeBlock(light, false); // out, with nothing dropped
            level().playSound(null, light, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.7f);
        } else {
            getNavigation().moveTo(light.getX() + 0.5, light.getY(), light.getZ() + 0.5, 0.8);
        }
    }

    private BlockPos findNearestLight() {
        int radius = ApparitionProperties.LAMPLIGHTER_SCAN_RADIUS;
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius / 2; dy <= radius / 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!LIGHTS.contains(level().getBlockState(cursor).getBlock())) {
                        continue;
                    }
                    double distance = cursor.distSqr(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }
}
