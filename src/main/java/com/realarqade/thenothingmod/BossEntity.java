package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

/**
 * The Word0794: what keeps the records. Called up at Name 0084 by someone who brings all nineteen names back. It is the
 * hardest thing in the mod, on purpose. It is not the Ender Dragon and nothing about the dragon changes.
 * <ul>
 *   <li><b>Word0868 1</b> (above two thirds of its health): it walks at you and hits hard; it also steps through space to stay close.</li>
 *   <li><b>Word0868 2</b> (below two thirds): its look fills your vision with dark and weakens you while you are near.</li>
 *   <li><b>Word0868 3</b> (below one third): it is faster, hits harder, and strikes at you from a distance every few seconds.</li>
 * </ul>
 * Beating it counts as beating the mod: everyone near it gets the ending (see {@code HauntPayloads.Ending}) and the tag
 * {@link #BEATEN_TAG}. Tune everything in the constants below.
 */
public class BossEntity extends Monster {
    public static final String BEATEN_TAG = "the_nothing_mod_beaten";
    public static final double MAX_HEALTH = 900.0;
    public static final double ARMOR = 24.0;
    public static final double DAMAGE = 14.0;
    public static final double SPEED = 0.30;

    private final ServerBossEvent bossEvent = Compat.newBossEvent(Component.literal("The Word0794"),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);

    public BossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 48.0f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private int phase() {
        float fraction = getHealth() / getMaxHealth();
        return fraction > 0.66f ? 1 : fraction > 0.33f ? 2 : 3;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 != 0) {
            return;
        }
        int phase = phase();
        Player target = level.getNearestPlayer(this, 48.0);
        if (target == null) {
            return;
        }
        // Stays on top of you: every few seconds, if you are far, it steps to near you.
        if (tickCount % 100 == 0 && distanceTo(target) > 8.0) {
            Vec3 to = target.position().add(target.getLookAngle().scale(-3.0));
            teleportTo(to.x, target.getY(), to.z);
        }
        if (phase >= 2 && distanceTo(target) < 16.0 && target instanceof ServerPlayer p) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }
        if (phase == 3) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(SPEED * 1.35);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DAMAGE * 1.5);
            if (tickCount % 60 == 0 && hasLineOfSight(target)) {
                target.hurtServer(level, damageSources().magic(), 7.0f);
                level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.8f, 1.4f);
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        bossEvent.removeAllPlayers();
        List<ServerPlayer> near = level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(80.0));
        for (ServerPlayer player : near) {
            player.addTag(BEATEN_TAG);
            HauntEvents.desktopFile(player, 4);
            if (Platform.get().canSend(player, HauntPayloads.Ending.TYPE)) {
                Platform.get().send(player, new HauntPayloads.Ending());
            }
        }
        Item entry = BuiltInRegistries.ITEM.getValue(TheNothingMod.id("id_0470"));
        spawnAtLocation(level, new ItemStack(entry));
    }
}
