package com.realarqade.thenothingmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItemIds {
    public static ResourceKey<Item> create(String name) {
        // Create the item key.
        return ResourceKey.create(Registries.ITEM, TheNothingMod.id(name));
    }
    public static final ResourceKey<Item> ID_0051 = create("id_0051");

}