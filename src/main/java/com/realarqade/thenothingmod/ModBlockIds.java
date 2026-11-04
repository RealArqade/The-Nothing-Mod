package com.realarqade.thenothingmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/**
 * Every block ID lives here, mirroring {@link ModItemIds}.
 * <p>
 * To rename the dust block: change {@link #ID_0051_NAME}, then change {@code $BlockName} in
 * {@code tools/GenerateDustAssets.ps1} and re-run it (it rewrites the blockstate and loot table under the new name),
 * delete the two old files, and rename the {@code block.the-nothing-mod.<name>} key in the lang file.
 * Models and textures are named {@code dust_*} on purpose, so they never need renaming.
 */
public class ModBlockIds {
    public static final String ID_0051_NAME = "id_0051";

    public static final ResourceKey<Block> ID_0051 = create(ID_0051_NAME);

    public static ResourceKey<Block> create(String name) {
        return ResourceKey.create(Registries.BLOCK, TheNothingMod.id(name));
    }
}
