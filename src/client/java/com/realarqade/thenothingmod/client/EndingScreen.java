package com.realarqade.thenothingmod.client;



import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** The ending, shown when the Word0794 falls. Plain lines, one at a time. Word0942 or Space closes it once it has all been shown. */
final class EndingScreen extends DustScreen {
    private static final String[] LINES = {
            "placeholder text",
            "placeholder text",
            "placeholder text",
            "placeholder text",
            "placeholder text",
            "",
            "placeholder text"
    };
    private static final int TICKS_PER_LINE = 80;

    private int age;

    EndingScreen() {
        super(Component.empty());
    }

    @Override
    public void tick() {
        super.tick();
        age++;
    }

    private boolean finished() {
        return age > LINES.length * TICKS_PER_LINE + 60;
    }

    @Override
    public void draw(Gfx graphics, int mouseX, int mouseY, float partialTick) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        graphics.fill(0, 0, w, h, 0xFF000000);
        int y = h / 2 - LINES.length * 7;
        for (int i = 0; i < LINES.length; i++) {
            float shown = (age + partialTick - i * TICKS_PER_LINE) / 40.0f;
            if (shown > 0 && !LINES[i].isEmpty()) {
                int alpha = (int) (Math.min(1.0f, shown) * 230);
                graphics.centeredText(font, LINES[i], w / 2, y, alpha << 24 | 0xD8D4C8);
            }
            y += 14;
        }
        if (finished()) {
            graphics.centeredText(font, "placeholder text", w / 2, h - 30, 0xFF5A5850);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (finished() && (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_SPACE
                || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE)) {
            minecraft.setScreenAndShow(null);
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
