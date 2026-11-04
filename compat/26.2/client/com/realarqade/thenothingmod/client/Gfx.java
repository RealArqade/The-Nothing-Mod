package com.realarqade.thenothingmod.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/** Thin wrapper over this Minecraft version's GUI drawing object, so the screens do not depend on its name. */
final class Gfx {
    private final GuiGraphicsExtractor g;

    Gfx(GuiGraphicsExtractor g) {
        this.g = g;
    }

    int guiWidth() { return g.guiWidth(); }

    int guiHeight() { return g.guiHeight(); }

    void fill(int x1, int y1, int x2, int y2, int color) { g.fill(x1, y1, x2, y2, color); }

    Matrix3x2fStack pose() { return g.pose(); }

    void text(Font font, String s, int x, int y, int color) { g.text(font, s, x, y, color); }

    void text(Font font, String s, int x, int y, int color, boolean shadow) { g.text(font, s, x, y, color, shadow); }

    void centeredText(Font font, String s, int x, int y, int color) { g.centeredText(font, s, x, y, color); }

    void blit(Identifier tex, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th, color);
    }
}