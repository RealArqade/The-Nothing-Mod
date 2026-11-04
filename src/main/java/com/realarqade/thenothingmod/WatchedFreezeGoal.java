package com.realarqade.thenothingmod;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;

/**
 * Stops a monster dead while a player is looking at it. It holds the MOVE flag, so as long as it is added with a higher priority
 * (a lower number) than the monster's movement goals, they can't run until nobody is watching.
 */
public final class WatchedFreezeGoal extends Goal {
    private final Mob mob;
    private final BooleanSupplier active;
    private final double range;
    private final double cone;

    /** @param active only freezes while this is true (e.g. "placeholder text") */
    public WatchedFreezeGoal(Mob mob, BooleanSupplier active, double range, double cone) {
        this.mob = mob;
        this.active = active;
        this.range = range;
        this.cone = cone;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return active.getAsBoolean() && mob.getTarget() != null && Watching.isWatched(mob, range, cone);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
    }
}
