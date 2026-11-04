package com.realarqade.thenothingmod.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Quaternionfc;

/** Client-side counterpart of Compat: Minecraft names that differ between versions (one copy per version in compat/<version>/client). */
public final class ClientCompat {
    private ClientCompat() {
    }
    public static void setFullscreen(Window window, boolean on) {
        if (window.isFullscreen() != on) {
            window.toggleFullScreen();
        }
    }

    public static void rotate(PoseStack stack, Quaternionfc rotation) {
        stack.mulPose(rotation);
    }

    /** The screen being shown (null in the world). */
    public static Screen currentScreen(Minecraft minecraft) {
        return minecraft.gui.screen();
    }

    /** Redraws all the chunks (so a changed block colour shows). */
    public static void reloadChunks(Minecraft minecraft) {
        minecraft.levelExtractor.allChanged();
    }
}
