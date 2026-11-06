package com.realarqade.thenothingmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Base for the mod's drawn screens: this Minecraft version's render and key methods, forwarded to draw() and onKey(). */
abstract class DustScreen extends Screen {
    protected DustScreen(Component title) {
        super(title);
    }

    protected void draw(Gfx graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** False for a screen that keeps the vanilla drawing (background and widgets) instead of draw(). */
    protected boolean customDraw() {
        return true;
    }

    /** A key was pressed; return true if handled. */
    protected boolean onKey(int key) {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (customDraw()) {
            draw(new Gfx(graphics), mouseX, mouseY, partialTick);
        } else {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return onKey(event.key()) || super.keyPressed(event);
    }
}