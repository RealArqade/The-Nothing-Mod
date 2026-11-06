package com.realarqade.thenothingmod.client.mixin;

import com.mojang.blaze3d.platform.Window;
import com.realarqade.thenothingmod.client.BsodState;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While the fake blue screen is up, a close Word0789 (Alt+F4, the window's close button) is counted instead of obeyed. The third one
 * goes through and the game closes as normal. Outside the blue screen this does nothing at all.
 */
@Mixin(Window.class)
public abstract class WindowMixin {
    @Shadow
    public abstract long handle();

    @Inject(method = "shouldClose", at = @At("HEAD"), cancellable = true)
    private void the_nothing_mod$countCloseRequests(CallbackInfoReturnable<Boolean> cir) {
        if (!BsodState.active) {
            return;
        }
        // this version keeps the "should close" flag inside GLFW instead of in a field of its own
        long window = this.handle();
        if (GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwSetWindowShouldClose(window, false);
            BsodState.lastRequestMillis = System.currentTimeMillis();
            if (++BsodState.closeRequests >= BsodState.CLOSE_REQUESTS_NEEDED) {
                BsodState.active = false;
                GLFW.glfwSetWindowShouldClose(window, true); // the third one: obey it
                return;
            }
        }
        cir.setReturnValue(false);
    }
}