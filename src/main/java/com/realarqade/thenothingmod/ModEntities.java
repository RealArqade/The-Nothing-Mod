package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    /** The big one, "The Nothing". */
    public static final EntityType<GlitchStalkerEntity> ID_0193 = Platform.get().register(
            BuiltInRegistries.ENTITY_TYPE,
            ModEntityIds.ID_0193,
            EntityType.Builder.of(GlitchStalkerEntity::new, MobCategory.MONSTER)
                    .sized(StalkerProperties.HITBOX_WIDTH, StalkerProperties.HITBOX_HEIGHT)
                    .clientTrackingRange(10)
                    .build(Compat.entityKey(ModEntityIds.ID_0193)));

    /** "The Surveyor": stands far away and watches. */
    public static final EntityType<NpcCEntity> ID_0462 = Platform.get().register(
            BuiltInRegistries.ENTITY_TYPE,
            ModEntityIds.ID_0462,
            EntityType.Builder.of(NpcCEntity::new, MobCategory.MONSTER)
                    .sized(ApparitionProperties.SURVEYOR_HITBOX_WIDTH, ApparitionProperties.SURVEYOR_HITBOX_HEIGHT)
                    .clientTrackingRange(10)
                    .build(Compat.entityKey(ModEntityIds.ID_0462)));

    /** "The Girl": follows you, only when you aren't looking. */
    public static final EntityType<NpcAEntity> ID_0198 = Platform.get().register(
            BuiltInRegistries.ENTITY_TYPE,
            ModEntityIds.ID_0198,
            EntityType.Builder.of(NpcAEntity::new, MobCategory.MONSTER)
                    .sized(ApparitionProperties.GIRL_HITBOX_WIDTH, ApparitionProperties.GIRL_HITBOX_HEIGHT)
                    .clientTrackingRange(10)
                    .build(Compat.entityKey(ModEntityIds.ID_0198)));

    /** "The Word0754": puts your lights out. */
    public static final EntityType<NpcBEntity> ID_0487 = Platform.get().register(
            BuiltInRegistries.ENTITY_TYPE,
            ModEntityIds.ID_0487,
            EntityType.Builder.of(NpcBEntity::new, MobCategory.MONSTER)
                    .sized(ApparitionProperties.LAMPLIGHTER_HITBOX_WIDTH, ApparitionProperties.LAMPLIGHTER_HITBOX_HEIGHT)
                    .clientTrackingRange(10)
                    .build(Compat.entityKey(ModEntityIds.ID_0487)));

    /** "The Word0794": the Word0869 fight, only ever called up at Name 0084. */
    public static final EntityType<BossEntity> REGISTRAR = Platform.get().register(
            BuiltInRegistries.ENTITY_TYPE,
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, TheNothingMod.id("the_registrar")),
            EntityType.Builder.of(BossEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 4.5f)
                    .clientTrackingRange(16)
                    .build(Compat.entityKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, TheNothingMod.id("the_registrar")))));

    public static void initialize() {
        Platform.get().registerAttributes(REGISTRAR, () -> BossEntity.createAttributes());
        Platform.get().registerAttributes(ID_0193, () -> GlitchStalkerEntity.createAttributes());
        Platform.get().registerAttributes(ID_0462, () -> NpcCEntity.createAttributes());
        Platform.get().registerAttributes(ID_0198, () -> NpcAEntity.createAttributes());
        Platform.get().registerAttributes(ID_0487, () -> NpcBEntity.createAttributes());
    }
}
