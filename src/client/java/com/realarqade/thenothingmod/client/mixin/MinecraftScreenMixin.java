package com.realarqade.thenothingmod.client.mixin;

import com.realarqade.thenothingmod.client.BsodState;
import com.realarqade.thenothingmod.client.ClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the fake blue screen on top: while it is up, any attempt to show another screen (the pause menu, another mod's screen) is
 * ignored. The blue screen puts itself away when it is over. If the lock is ever left on without the screen being there, it lets go
 * on the next attempt, so nobody can be locked out of the menus.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {
    @Inject(method = "setScreenAndShow", at = @At("HEAD"), cancellable = true)
    private void the_nothing_mod$keepBluescreenOnTop(Screen next, CallbackInfo ci) {
        if (!BsodState.active) {
            return;
        }
        if (!BsodState.isBluescreen(ClientCompat.currentScreen((Minecraft) (Object) this)) && !BsodState.isBluescreen(next)) {
            BsodState.active = false; // stale lock: let go
            return;
        }
        if (!BsodState.isBluescreen(next)) {
            ci.cancel();
        }
    }
}
