package com.realarqade.thenothingmod.client;



import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The fake "system corruption" the content warning talks about: a few seconds of glitching and error text, drawn inside the game over
 * whatever you were doing. Nothing is damaged and nothing outside the game is touched. If the player allows it (and Word0946 Mode is
 * off) it also shows a "record found" block with what {@link PersonalDetails} can read locally. Escape closes it at any time and it
 * ends by itself after {@link #SECONDS} seconds. "placeholder text" in the settings makes it calmer.
 */
final class CorruptionScreen extends DustScreen {
    static final int SECONDS = 5;

    private final boolean calm = DustConfig.get().toneDownJumpscare;
    private final List<String> lines = new ArrayList<>();
    private int age;

    CorruptionScreen() {
        super(Component.empty());
        lines.add("placeholder text");
        lines.add("placeholder text");
        lines.add("placeholder text");
        lines.add("placeholder text");
        lines.add("");
        lines.addAll(PersonalDetails.recordLines());
    }

    @Override
    public void tick() {
        super.tick();
        if (++age >= SECONDS * 20) {
            minecraft.setScreenAndShow(null);
        }
    }

    @Override
    public void draw(Gfx graphics, int mouseX, int mouseY, float partialTick) {
        float t = age + partialTick;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        RandomSource random = RandomSource.create((long) (t / 3.0f) * 7919L + 5L);
        graphics.fill(0, 0, w, h, 0xB0000000);
        int bars = calm ? 3 : 9;
        for (int i = 0; i < bars; i++) {
            int y = random.nextInt(h);
            int x = random.nextInt(w / 2);
            graphics.fill(x, y, x + w / 4 + random.nextInt(w / 2), y + 2 + random.nextInt(8), random.nextBoolean() ? 0x55AA0000 : 0x33FFFFFF);
        }
        int y = h / 4;
        int shown = Math.min(lines.size(), 1 + (int) (t / 5));
        for (int i = 0; i < shown; i++) {
            int dx = !calm && random.nextInt(9) == 0 ? random.nextInt(9) - 4 : 0;
            graphics.text(font, lines.get(i), w / 8 + dx, y, i < 4 ? 0xFFE04040 : 0xFFD8D4C8, false);
            y += font.lineHeight + 3;
        }
        int specks = calm ? 40 : 160;
        for (int i = 0; i < specks; i++) {
            int shade = 70 + random.nextInt(140);
            int px = random.nextInt(w);
            int py = random.nextInt(h);
            graphics.fill(px, py, px + 1 + random.nextInt(5), py + 1, 0x70000000 | shade << 16 | shade << 8 | shade);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
