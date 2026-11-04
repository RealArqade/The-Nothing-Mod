package com.realarqade.thenothingmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A small girl who shows up behind you and follows. She only moves while you are not looking at her: turn around and she stops.
 * When she reaches you she does no damage; everything goes dark and slow for a few seconds, a heartbeat, and she is gone.
 * See {@link ApparitionProperties}. Summon one with {@code /summon the-nothing-mod:id_0198}.
 */
public class NpcAEntity extends PathfinderMob implements Apparition {
    public NpcAEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, ApparitionProperties.GIRL_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 96.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Higher priority than the walking, so while she's watched, she can't move.
        goalSelector.addGoal(1, new WatchedFreezeGoal(this, () -> true,
                ApparitionProperties.GIRL_WATCH_RANGE, ApparitionProperties.GIRL_WATCH_CONE));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 96.0f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 40 == 0) {
            boolean nobodyNear = level().getNearestPlayer(this, 160.0) == null;
            if (nobodyNear || tickCount > ApparitionProperties.GIRL_LIFETIME_TICKS) {
                discard();
            }
        }
    }

    /** Called when she reaches her target. No damage: darkness, slowness and a heartbeat, then she's gone. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ApparitionProperties.GIRL_DARKNESS_TICKS));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ApparitionProperties.GIRL_SLOWNESS_TICKS, 1));
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0f, 0.7f);
            discard();
            return true;
        }
        return false;
    }
}
