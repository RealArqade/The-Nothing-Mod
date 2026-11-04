package com.realarqade.thenothingmod.mixin;

import com.mojang.brigadier.ParseResults;
import com.realarqade.thenothingmod.PlayRestrictions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes player commands. Both entry points that run a command are blocked for players, and players are never sent the
 * list of commands, so there is nothing to tab-complete either (see {@link PlayRestrictions}).
 */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Inject(method = "performPrefixedCommand", at = @At("HEAD"), cancellable = true)
    private void thenothingmod$blockPrefixed(CommandSourceStack source, String command, CallbackInfo ci) {
        if (PlayRestrictions.blocksCommandsFrom(source)) {
            ci.cancel();
        }
    }

    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void thenothingmod$blockParsed(ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        if (PlayRestrictions.blocksCommandsFrom(parse.getContext().getSource())) {
            ci.cancel();
        }
    }

    @Inject(method = "sendCommands", at = @At("HEAD"), cancellable = true)
    private void thenothingmod$hideCommandList(ServerPlayer player, CallbackInfo ci) {
        if (PlayRestrictions.hidesCommandsFrom(player)) {
            ci.cancel();
        }
    }
}
