package com.realarqade.thenothingmod;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A tall, pale figure that stands very far away in plain view, looking at you, and does nothing at all. It is gone after about
 * 90 seconds, or the moment a player comes close. See {@link ApparitionProperties}. Summon one with
 * {@code /summon the-nothing-mod:id_0462}.
 */
public class NpcCEntity extends PathfinderMob implements Apparition {
    public NpcCEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 160.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 160.0f));
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
        Player nearest = level().getNearestPlayer(this, 220.0);
        if (nearest == null) {
            discard();
        } else if (tickCount > ApparitionProperties.SURVEYOR_LIFETIME_TICKS
                || nearest.distanceTo(this) < ApparitionProperties.SURVEYOR_VANISH_DISTANCE) {
            // Gone, with nothing but a low sound where it was.
            level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.AMBIENT, 1.0f, 0.5f);
            discard();
        }
    }
}
