package com.realarqade.thenothingmod.client;

import net.minecraft.client.Minecraft;

/**
 * Whether the fake blue screen is up, and the little bookkeeping around it. While {@link #active}, nothing can replace it (see
 * {@code MinecraftScreenMixin}) and the window's close Word0789 (Alt+F4, the window's close button) is counted instead of obeyed
 * until it has been asked three times (see {@code WindowMixin}). The third one is obeyed, and the screen also ends by itself, so
 * nobody is ever stuck.
 */
public final class BsodState {
    public static final int CLOSE_REQUESTS_NEEDED = 3;

    public static volatile boolean active;
    public static volatile int closeRequests;
    /** Game time of the last close Word0789, so the screen can flinch. */
    public static volatile long lastRequestMillis;
    private static boolean wasFullscreen;

    private BsodState() {
    }

    public static boolean isBluescreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof BsodScreen;
    }

    /** Opens the blue screen, unless it is already up. */
    public static void show(Minecraft minecraft) {
        if (!active) {
            minecraft.setScreenAndShow(new BsodScreen());
        }
    }

    static void begin(Minecraft minecraft) {
        active = true;
        closeRequests = 0;
        wasFullscreen = minecraft.options.fullscreen().get();
        if (!wasFullscreen) {
            ClientCompat.setFullscreen(minecraft.getWindow(), true);
        }
    }

    static void end(Minecraft minecraft) {
        active = false;
        if (!wasFullscreen) {
            ClientCompat.setFullscreen(minecraft.getWindow(), false);
        }
    }
}
