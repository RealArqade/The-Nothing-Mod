package com.realarqade.thenothingmod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** An item with fixed hover text (this Minecraft version has no lore data component, so the item adds the lines itself). */
public class LoreItem extends Item {
    private final List<Component> lore;

    public LoreItem(Properties properties, List<Component> lore) {
        super(properties);
        this.lore = lore;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(lore);
    }
}