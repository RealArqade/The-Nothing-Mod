package com.realarqade.thenothingmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Every small monster. What a given one does is decided by its {@link MonsterDef} (see the monster list); this class holds the
 * code for each {@link MonsterDef.Behavior}. None of them damage you. They scare you, blind you, slow you, say things, and leave.
 */
public class ApparitionEntity extends PathfinderMob implements Apparition {
    private final MonsterDef def;
    private int lineIndex;
    private int unwatchedChecks;

    public ApparitionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.def = ModMonsters.defOf(type);
    }

    public MonsterDef def() {
        return def;
    }

    public static AttributeSupplier.Builder createAttributes(MonsterDef def) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, def.speed())
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 96.0);
    }

    @Override
    protected void registerGoals() {
        if (def == null) {
            return;
        }
        goalSelector.addGoal(0, new FloatGoal(this));
        switch (def.behavior()) {
            case HUNT, FOLLOW -> {
                goalSelector.addGoal(1, new WatchedFreezeGoal(this, () -> true, 64.0, 0.5));
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
                targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
            }
            case FLEE -> goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 40.0f, 1.0, 1.4));
            default -> {
            }
        }
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 96.0f));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (def == null || !(level() instanceof ServerLevel level) || tickCount % 10 != 0) {
            return;
        }
        Player nearest = level.getNearestPlayer(this, 160.0);
        if (nearest == null || tickCount > def.lifetime()) {
            discard();
            return;
        }
        double distance = distanceTo(nearest);
        if (def.behavior() != MonsterDef.Behavior.SPEAKER && def.behavior() != MonsterDef.Behavior.HUNT
                && def.behavior() != MonsterDef.Behavior.FOLLOW && !def.sound().isEmpty() && random.nextInt(14) == 0) {
            playSound(level, distance);
        }
        switch (def.behavior()) {
            case WATCH, CROWD -> {
                if (distance < 24.0) {
                    vanish(level);
                }
            }
            case LURK -> lurk(level, nearest, distance);
            case BLINK -> blink(level, nearest, distance);
            case SPEAKER -> speak(level, nearest, distance);
            case CORRUPT -> corrupt(level);
            default -> {
            }
        }
    }

    private void vanish(ServerLevel level) {
        discard();
    }

    private void lurk(ServerLevel level, Player player, double distance) {
        if (distance < 7.0) {
            vanish(level);
            return;
        }
        if (def.speed() > 0 && !Watching.isWatched(this, 80.0, 0.4)) {
            getNavigation().moveTo(player, 1.0);
        } else {
            getNavigation().stop();
        }
    }

    /** Each time you spend a moment not looking, it jumps a third of the way closer. */
    private void blink(ServerLevel level, Player player, double distance) {
        if (Watching.isWatched(this, 80.0, 0.5)) {
            unwatchedChecks = 0;
            return;
        }
        if (distance < 3.0) {
            touch(player);
            return;
        }
        if (++unwatchedChecks < 3) {
            return;
        }
        unwatchedChecks = 0;
        double fraction = 0.3;
        double x = getX() + (player.getX() - getX()) * fraction;
        double z = getZ() + (player.getZ() - getZ()) * fraction;
        BlockPos at = BlockPos.containing(x, getY(), z);
        for (int dy = 0; dy >= -3; dy--) {
            BlockPos feet = at.offset(0, dy, 0);
            if (level.noCollision(getBoundingBox().move(x - getX(), feet.getY() - getY(), z - getZ()))
                    && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)) {
                teleportTo(x, feet.getY(), z);
                return;
            }
        }
    }

    private void speak(ServerLevel level, Player player, double distance) {
        if (distance > 16.0 || def.lines().isEmpty() || tickCount % 80 != 0 || !(player instanceof ServerPlayer target)) {
            return;
        }
        if (lineIndex >= def.lines().size()) {
            vanish(level);
            return;
        }
        target.sendSystemMessage(Component.literal("<" + def.name() + "> " + def.lines().get(lineIndex++)));
    }

    private static final Map<String, Object> SOUNDS = Map.of(
            "BELL", SoundEvents.BELL_BLOCK,
            "NOTE", SoundEvents.NOTE_BLOCK_BELL,
            "SCRAPE", SoundEvents.AMBIENT_CAVE,
            "HUM", SoundEvents.NOTE_BLOCK_BASS);

    private void playSound(ServerLevel level, double distance) {
        Object raw = SOUNDS.get(def.sound());
        SoundEvent event = raw instanceof net.minecraft.core.Holder<?> holder ? (SoundEvent) holder.value() : (SoundEvent) raw;
        if (event != null && distance < 48.0) {
            level.playSound(null, blockPosition(), event, SoundSource.HOSTILE, 0.8f, 0.5f + random.nextFloat() * 0.3f);
        }
    }

    /** Takes out a plant or a light near it, now and then. */
    private void corrupt(ServerLevel level) {
        if (random.nextInt(2) != 0) {
            return;
        }
        BlockPos at = blockPosition().offset(random.nextInt(11) - 5, random.nextInt(5) - 2, random.nextInt(11) - 5);
        BlockState state = level.getBlockState(at);
        if (!state.isAir() && state.getDestroySpeed(level, at) == 0.0f && !state.is(ModBlocks.ID_0051)) {
            level.removeBlock(at, false);
        }
    }

    /** Called when it reaches you: an effect, Word0802 a jumpscare, and it is gone. */
    private void touch(Player player) {
        if (!(player instanceof ServerPlayer target) || !(level() instanceof ServerLevel level)) {
            return;
        }
        MobEffect effect = effectOf(def.effect());
        if (effect != null) {
            target.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), def.effectTicks()));
        }
        if (def.scare() > 0) {
            Jumpscares.send(target, def.scare(), def.id());
        } else {
            level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0f, 0.7f);
        }
        discard();
    }

    private static MobEffect effectOf(String name) {
        return switch (name) {
            case "SLOWNESS" -> MobEffects.SLOWNESS.value();
            case "BLINDNESS" -> MobEffects.BLINDNESS.value();
            case "DARKNESS" -> MobEffects.DARKNESS.value();
            case "NAUSEA" -> MobEffects.NAUSEA.value();
            case "HUNGER" -> MobEffects.HUNGER.value();
            case "WEAKNESS" -> MobEffects.WEAKNESS.value();
            case "MINING_FATIGUE" -> MobEffects.MINING_FATIGUE.value();
            default -> null;
        };
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof Player player) {
            touch(player);
            return true;
        }
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (def == null) {
            return;
        }
        for (MonsterDef.Drop drop : def.drops()) {
            Item item = BuiltInRegistries.ITEM.getValue(TheNothingMod.id(drop.item()));
            if (random.nextFloat() < drop.chance() * 4.0f) {
                spawnAtLocation(level, new ItemStack(item));
            }
        }
        if (def.slip() > 0 && random.nextFloat() < 0.5f) {
            spawnAtLocation(level, new ItemStack(BuiltInRegistries.ITEM.getValue(TheNothingMod.id("id_0482" + def.slip()))));
        }
    }
}
