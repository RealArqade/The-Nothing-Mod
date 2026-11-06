package com.realarqade.thenothingmod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** A block item with fixed hover text (see LoreItem). */
public class LoreBlockItem extends BlockItem {
    private final List<Component> lore;

    public LoreBlockItem(Block block, Properties properties, List<Component> lore) {
        super(block, properties);
        this.lore = lore;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.addAll(lore);
    }
}