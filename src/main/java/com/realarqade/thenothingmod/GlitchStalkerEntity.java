package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The big one, "The Nothing". It appears at random far from a player (see {@link StalkerEvents}) and stands there watching.
 * After {@link StalkerProperties#CHASE_AFTER_TICKS} (2 minutes) it chases the nearest player, and if it catches them, it
 * ends their game: a jumpscare, then they are kicked (unless that is switched off in {@link ServerSettings}) and are dead when
 * they come back, or they simply die on the spot. It Word0800 after the catch, or if it gives up. Stats, timings and sizes are
 * in {@link StalkerProperties}. You can also summon one: {@code /summon the-nothing-mod:id_0193}.
 */
public class GlitchStalkerEntity extends Monster implements Apparition {
    public GlitchStalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, StalkerProperties.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, StalkerProperties.MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, StalkerProperties.ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, StalkerProperties.FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (StalkerProperties.FREEZE_WHEN_WATCHED) {
            goalSelector.addGoal(1, new WatchedFreezeGoal(this, this::isChasing, StalkerProperties.WATCH_RANGE, StalkerProperties.WATCH_CONE));
        }
        goalSelector.addGoal(2, new ChaseGoal(this));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, (float) StalkerProperties.FOLLOW_RANGE));
        // It notices players through walls too: the waiting is the point, it isn't hunting yet.
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Waiting is over: it is hunting. Counted in ticks since it appeared. */
    public boolean isChasing() {
        return tickCount >= StalkerProperties.CHASE_AFTER_TICKS;
    }

    /** It stays until it Word0800 by itself (see {@link #tick()}), rather than being cleaned up by vanilla's despawning. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel && isAlive() && isChasing() && tickCount % 10 == 0) {
            hunt(serverLevel);
        }
        if (!level().isClientSide() && tickCount % 40 == 0) {
            boolean nobodyNear = level().getNearestPlayer(this, StalkerProperties.VANISH_DISTANCE) == null;
            boolean gaveUp = tickCount > StalkerProperties.CHASE_AFTER_TICKS + StalkerProperties.GIVE_UP_AFTER_TICKS;
            if (nobodyNear || gaveUp) {
                discard();
            }
        }
    }

    /**
     * Once the waiting is over this does not depend on vanilla's target and path goals alone (they give up on creative players and on
     * awkward paths, and it then just stands there): every half second it picks the nearest player who is not a spectator, walks at
     * them, and catches them when it is close enough with nothing in between.
     */
    private void hunt(ServerLevel level) {
        ServerPlayer nearest = null;
        double best = StalkerProperties.FOLLOW_RANGE * StalkerProperties.FOLLOW_RANGE;
        for (ServerPlayer p : level.players()) {
            double d = distanceToSqr(p);
            if (p.isAlive() && !p.isSpectator() && d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest == null) {
            return;
        }
        if (getTarget() == null) {
            setTarget(nearest);
        }
        if (best < 2.4 * 2.4 && hasLineOfSight(nearest)) {
            catchPlayer(level, nearest);
        } else if (getNavigation().isDone()) {
            getNavigation().moveTo(nearest, StalkerProperties.CHASE_SPEED_MODIFIER);
        }
    }

    /** Called when it reaches its target. A player is caught; anything else just takes a normal hit. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof ServerPlayer player) {
            catchPlayer(level, player);
            return true;
        }
        return super.doHurtTarget(level, target);
    }

    private void catchPlayer(ServerLevel level, ServerPlayer player) {
        // Marked from the moment they're caught. The tag is saved with the player, so even quitting during the
        // jumpscare doesn't save them: StalkerEvents makes them dead when they rejoin.
        player.addTag(StalkerEvents.DOOMED_TAG);

        // The jumpscare plays first (only players whose game has the mod can see it), then the kick or the death.
        boolean scared = Platform.get().canSend(player, JumpscarePayload.TYPE);
        if (scared) {
            Platform.get().send(player, new JumpscarePayload(StalkerProperties.JUMPSCARE_TICKS));
        }
        StalkerEvents.runLater(player, scared ? StalkerProperties.JUMPSCARE_TICKS : 0, caught -> {
            if (ServerSettings.kickPlayers()) {
                caught.connection.disconnect(Component.translatable("message.the-nothing-mod.kicked"));
            } else {
                caught.removeTag(StalkerEvents.DOOMED_TAG);
                kill(level, caught);
            }
        });
        discard();
    }

    /** Kills for certain: if something (a totem of undying) saves them from the hit, the second step doesn't care. */
    private void kill(ServerLevel level, ServerPlayer player) {
        DamageSource source = level.damageSources().mobAttack(this);
        player.hurtServer(level, source, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.kill(level);
        }
    }

    /** The chase itself: the vanilla melee goal, but it only runs once the waiting is over. */
    private static final class ChaseGoal extends MeleeAttackGoal {
        private final GlitchStalkerEntity monster;

        ChaseGoal(GlitchStalkerEntity monster) {
            super(monster, StalkerProperties.CHASE_SPEED_MODIFIER, true);
            this.monster = monster;
        }

        @Override
        public boolean canUse() {
            return monster.isChasing() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return monster.isChasing() && super.canContinueToUse();
        }
    }
}
