package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class ModBlocks {
    public static final SuspiciousDustProperties DUST_PROPERTIES = SuspiciousDustProperties.loadBaked();

    public static final Block ID_0051 = register(
            ModBlockIds.ID_0051,
            properties -> new SuspiciousDustBlock(properties, DUST_PROPERTIES),
            DUST_PROPERTIES.toBlockProperties());

    public static Block register(ResourceKey<Block> key, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties settings) {
        Block block = factory.apply(settings.setId(key));
        return Platform.get().register(BuiltInRegistries.BLOCK, key, block);
    }

    public static void initialize() {
        // Touching this class runs the static registrations above.
    }
}
