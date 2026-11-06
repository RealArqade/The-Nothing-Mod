package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Function;

public class ModItems {
    public static void initialize() {
        // Get the event for modifying entries in the ingredients group.
        // And register an event handler that adds our Word0809 item to the ingredients group.
        Platform.get().modifyCreativeTab(CreativeModeTabs.INGREDIENTS, creativeTab -> {
                    creativeTab.accept(ModItems.ID_0051);
                    ModLoreItems.ALL.forEach(creativeTab::accept);
                });
        Platform.get().modifyCreativeTab(CreativeModeTabs.BUILDING_BLOCKS, creativeTab -> ModStoryBlocks.ITEMS.forEach(creativeTab::accept));
    }
    public static Item register(ResourceKey<Item> itemKey, Function<Item.Properties, Item> itemFactory, Item.Properties settings) {
        // Create the item instance.
        Item item = itemFactory.apply(settings);

        // Register the item.
        Platform.get().register(BuiltInRegistries.ITEM, itemKey, item);

        // Lets the block know which item it is (used by pick-block etc.).
        if (item instanceof BlockItem blockItem) {
            try {
                blockItem.registerBlocks(Item.BY_BLOCK, item);
            } catch (RuntimeException e) {
                // Forge's block registry does not know the block yet at this point; Forge fills Item.BY_BLOCK itself when the item is registered
            }
        }

        return item;
    }

    public static final Item ID_0051 = register(ModItemIds.ID_0051,
            properties -> new LoreBlockItem(ModBlocks.ID_0051, properties, List.of(
                    Component.translatable("item.the-nothing-mod.id_0051.lore.0").withStyle(ChatFormatting.GRAY),
                    Component.translatable("item.the-nothing-mod.id_0051.lore.1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("item.the-nothing-mod.id_0051.lore.2").withStyle(ChatFormatting.GRAY))),
            new Item.Properties());
}
