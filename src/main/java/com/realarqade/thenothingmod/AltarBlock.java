package com.realarqade.thenothingmod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Name 0084. Bring it all nineteen name slips (one of each, see {@link ModLoreItems#NINETEEN}), use it, and the Word0794
 * answers. Fewer, and it only tells you how many it is missing. The slips come from the monsters that wear those people's outlines
 * (about one kill in two drops one), so finishing the mod means finding and putting down all of them.
 */
public class AltarBlock extends Block {
    public AltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer)) {
            return InteractionResult.SUCCESS;
        }
        int have = 0;
        Item[] slips = new Item[ModLoreItems.NINETEEN];
        for (int n = 1; n <= ModLoreItems.NINETEEN; n++) {
            slips[n - 1] = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(TheNothingMod.id("id_0482" + n));
            if (has(player, slips[n - 1])) {
                have++;
            }
        }
        if (have < ModLoreItems.NINETEEN) {
            Compat.overlay(player, Component.literal("placeholder text" + have + "."));
            return InteractionResult.CONSUME;
        }
        if (!server.getEntitiesOfClass(BossEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(64.0)).isEmpty()) {
            Compat.overlay(player, Component.literal("placeholder text"));
            return InteractionResult.CONSUME;
        }
        for (Item slip : slips) {
            take(player, slip);
        }
        ModEntities.REGISTRAR.spawn(server, pos.above(2), EntitySpawnReason.EVENT);
        HauntEvents.desktopFile((ServerPlayer) player, 3);
        server.playSound(null, pos, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.5f, 0.6f);
        return InteractionResult.CONSUME;
    }

    private static boolean has(Player player, Item item) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void take(Player player, Item item) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
    }
}
