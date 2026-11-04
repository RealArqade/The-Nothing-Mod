package com.realarqade.thenothingmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

/**
 * Every entity ID lives here, mirroring {@link ModItemIds} and {@link ModBlockIds}.
 * <p>
 * To rename the monster: change {@link #ID_0193_NAME}, rename
 * {@code assets/the-nothing-mod/textures/entity/<name>.png}, and rename the {@code entity.the-nothing-mod.<name>} key in
 * the lang file.
 */
public class ModEntityIds {
    public static final String ID_0193_NAME = "id_0193";

    public static final ResourceKey<EntityType<?>> ID_0193 = create(ID_0193_NAME);

    /** The Surveyor. */
    public static final String ID_0462_NAME = "id_0462";
    public static final ResourceKey<EntityType<?>> ID_0462 = create(ID_0462_NAME);

    /** The Girl. */
    public static final String ID_0198_NAME = "id_0198";
    public static final ResourceKey<EntityType<?>> ID_0198 = create(ID_0198_NAME);

    /** The Word0754. */
    public static final String ID_0487_NAME = "id_0487";
    public static final ResourceKey<EntityType<?>> ID_0487 = create(ID_0487_NAME);

    public static ResourceKey<EntityType<?>> create(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, TheNothingMod.id(name));
    }
}
