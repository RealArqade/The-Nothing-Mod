package com.realarqade.thenothingmod.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3x2fStack;

/** Thin wrapper over this Minecraft version's GUI drawing object, so the screens do not depend on its name. */
final class Gfx {
    private final GuiGraphics g;

    Gfx(GuiGraphics g) {
        this.g = g;
    }

    int guiWidth() { return g.guiWidth(); }

    int guiHeight() { return g.guiHeight(); }

    void fill(int x1, int y1, int x2, int y2, int color) { g.fill(x1, y1, x2, y2, color); }

    Matrix3x2fStack pose() { return g.pose(); }
    void push() { g.pose().pushMatrix(); }

    void translate(float x, float y) { g.pose().translate(x, y); }

    void scale(float x, float y) { g.pose().scale(x, y); }

    void pop() { g.pose().popMatrix(); }


    void text(Font font, String s, int x, int y, int color) { g.drawString(font, s, x, y, color); }

    void text(Font font, String s, int x, int y, int color, boolean shadow) { g.drawString(font, s, x, y, color, shadow); }

    void centeredText(Font font, String s, int x, int y, int color) { g.drawCenteredString(font, s, x, y, color); }

    void blit(ResourceLocation tex, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, rw, rh, tw, th, color);
    }
}