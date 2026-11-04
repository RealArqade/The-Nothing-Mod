package com.realarqade.thenothingmod.mixin;

import com.realarqade.thenothingmod.PlayRestrictions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * {@code setGameModeForPlayer(current, previous)} is where every game mode change ends up: the /gamemode command, the
 * F3+F4 switcher, a world's saved mode on login, hardcore death turning you into a spectator. Turning creative and
 * spectator into survival here covers all of them at once, except for dev users (see {@link PlayRestrictions}).
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @ModifyVariable(method = "setGameModeForPlayer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private GameType thenothingmod$filterCurrent(GameType type) {
        return PlayRestrictions.filter(type, player);
    }

    @ModifyVariable(method = "setGameModeForPlayer", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private GameType thenothingmod$filterPrevious(GameType type) {
        return PlayRestrictions.filter(type, player);
    }
}
