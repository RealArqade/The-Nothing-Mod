package com.realarqade.thenothingmod.mixin;

import com.realarqade.thenothingmod.PlayRestrictions;
import net.minecraft.world.level.LevelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A world is never "allow cheats", whatever the world creation screen or an old save says (see {@link PlayRestrictions}). */
@Mixin(LevelSettings.class)
public abstract class LevelSettingsMixin {
    @Inject(method = "allowCommands", at = @At("HEAD"), cancellable = true)
    private void thenothingmod$noCheats(CallbackInfoReturnable<Boolean> cir) {
        // A dev user playing on their own computer can still have cheats in their own worlds.
        if (PlayRestrictions.active() && !PlayRestrictions.localUserIsDev()) {
            cir.setReturnValue(false);
        }
    }
}
