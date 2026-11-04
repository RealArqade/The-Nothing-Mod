package com.realarqade.thenothingmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base for the mod's drawn screens: this Minecraft version's render method, forwarded to draw(). */
abstract class DustScreen extends Screen {
    protected DustScreen(Component title) {
        super(title);
    }

    protected abstract void draw(Gfx graphics, int mouseX, int mouseY, float partialTick);

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Gfx(graphics), mouseX, mouseY, partialTick);
    }
}