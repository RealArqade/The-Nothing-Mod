package com.realarqade.thenothingmod.client;



import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The fake blue screen. Everything about it is drawn here, inside the game: no real window, no operating-system message. It forces
 * fullscreen while it is up (and puts the window back after), nothing can replace it (see {@link BsodState}), and the window's close
 * Word0789 has to come three times before it is obeyed. It also ends by itself after {@link #SECONDS} seconds.
 * <p>
 * It is meant to look wrong, not like the real thing: dark, slightly warped, shaking, with static. The "stop code" line, where the
 * real one has a code name, is a line of the story. Hidden in the static is a small square in the bottom corner that brightens in
 * morse code and spells {@link #MORSE_TEXT}, and the sound has a sentence in its spectrogram (see {@link SpectrogramAudio}).
 * Everything is slow and low-contrast, there are no flashes, and "placeholder text" in the settings calms it further.
 */
final class BsodScreen extends DustScreen {
    static final int SECONDS = 24;
    static final String MORSE_TEXT = "placeholder text";
    private static final String STOP_CODE = "NINETEEN_PEOPLE_WERE_NEVER_COUNTED";

    private static final java.util.Map<Character, String> MORSE = java.util.Map.ofEntries(
            java.util.Map.entry('A', ".-"), java.util.Map.entry('B', "-..."), java.util.Map.entry('C', "-.-."),
            java.util.Map.entry('D', "-.."), java.util.Map.entry('E', "."), java.util.Map.entry('F', "..-."),
            java.util.Map.entry('G', "--."), java.util.Map.entry('H', "...."), java.util.Map.entry('I', ".."),
            java.util.Map.entry('J', ".---"), java.util.Map.entry('K', "-.-"), java.util.Map.entry('L', ".-.."),
            java.util.Map.entry('M', "--"), java.util.Map.entry('N', "-."), java.util.Map.entry('O', "---"),
            java.util.Map.entry('P', ".--."), java.util.Map.entry('Q', "--.-"), java.util.Map.entry('R', ".-."),
            java.util.Map.entry('S', "..."), java.util.Map.entry('T', "-"), java.util.Map.entry('U', "..-"),
            java.util.Map.entry('V', "...-"), java.util.Map.entry('W', ".--"), java.util.Map.entry('X', "-..-"),
            java.util.Map.entry('Y', "-.--"), java.util.Map.entry('Z', "--.."));

    private final boolean calm = DustConfig.get().toneDownJumpscare;
    private final boolean[] morseOn;
    private int age;
    private boolean started;

    BsodScreen() {
        super(Component.empty());
        this.morseOn = buildMorse(calm ? 5 : 3);
    }

    /** One entry per time unit: true while the light is on. Dot = 1 unit, dash = 3, gap 1, letter gap 3, word gap 7, then a long pause. */
    private static boolean[] buildMorse(int unitTicks) {
        java.util.List<Boolean> units = new java.util.ArrayList<>();
        for (char c : MORSE_TEXT.toCharArray()) {
            if (c == ' ') {
                for (int i = 0; i < 4; i++) {
                    units.add(false); // 3 from the letter gap plus 4 here is the word gap of 7
                }
                continue;
            }
            for (char symbol : MORSE.get(c).toCharArray()) {
                int length = symbol == '.' ? 1 : 3;
                for (int i = 0; i < length; i++) {
                    units.add(true);
                }
                units.add(false);
            }
            for (int i = 0; i < 2; i++) {
                units.add(false);
            }
        }
        for (int i = 0; i < 20; i++) {
            units.add(false);
        }
        boolean[] expanded = new boolean[units.size() * unitTicks];
        for (int i = 0; i < expanded.length; i++) {
            expanded[i] = units.get(i / unitTicks);
        }
        return expanded;
    }

    @Override
    protected void init() {
        if (!started) {
            started = true;
            BsodState.begin(minecraft);
            SpectrogramAudio.play(minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * (calm ? 0.5f : 0.9f));
        }
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (age >= SECONDS * 20) {
            dismiss();
        }
    }

    private void dismiss() {
        BsodState.end(minecraft);
        minecraft.setScreenAndShow(null);
    }

    @Override
    public void removed() {
        // However it ended, put the window and the lock back.
        if (BsodState.active) {
            BsodState.end(minecraft);
        }
    }

    @Override
    public void draw(Gfx graphics, int mouseX, int mouseY, float partialTick) {
        float t = age + partialTick;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        // A new random pattern a few times a second, never faster.
        RandomSource random = RandomSource.create((long) (t / 6.0f) * 7919L + 31L);
        boolean flinch = System.currentTimeMillis() - BsodState.lastRequestMillis < 400;

        graphics.fill(0, 0, w, h, 0xFF040714);
        // Darker towards the edges, as if the glass were curved.
        int edge = Math.max(8, w / 14);
        graphics.fill(0, 0, edge, h, 0x66000000);
        graphics.fill(w - edge, 0, w, h, 0x66000000);
        graphics.fill(0, 0, w, edge / 2, 0x66000000);
        graphics.fill(0, h - edge / 2, w, h, 0x66000000);

        int left = w / 8;
        int shakeX = flinch ? random.nextInt(9) - 4 : (int) (Math.sin(t * 0.13) * 1.5);
        int top = h / 6;

        // The sad face, huge and wrong.
        graphics.pose().pushMatrix();
        graphics.pose().translate(left + shakeX, top);
        graphics.pose().scale(6.0f, 6.0f);
        graphics.text(font, ":(", 0, 0, 0xFFB8C0D8);
        graphics.pose().popMatrix();

        int y = top + 80;
        String[] lines = {
                "placeholder text",
                "placeholder text",
                "",
                "19% complete",
                "",
                "placeholder text" + STOP_CODE,
                "placeholder text"
        };
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                int dx = (random.nextInt(14) == 0 ? random.nextInt(13) - 6 : 0) + shakeX;
                // A pale red and a pale cyan ghost behind the text, a pixel or two off.
                graphics.text(font, lines[i], left + dx - 1, y + 1, 0x55FF2040, false);
                graphics.text(font, lines[i], left + dx + 1, y - 1, 0x4040FFFF, false);
                graphics.text(font, lines[i], left + dx, y, i >= 5 ? 0xFFE8ECFF : 0xFFB8C0D8, false);
            }
            y += font.lineHeight + 5;
        }

        // A dim note at the bottom, so nobody is ever unsure what this is.
        graphics.text(font, "placeholder text", left, h - h / 10, 0xFF4A5068, false);

        // Id0651, low and slow.
        int specks = calm ? 40 : 150;
        for (int i = 0; i < specks; i++) {
            int x = random.nextInt(w);
            int sy = random.nextInt(h);
            int shade = 60 + random.nextInt(120);
            graphics.fill(x, sy, x + 1 + random.nextInt(6), sy + 1, 0x30000000 | shade << 16 | shade << 8 | shade);
        }
        for (int i = 0; i < 3; i++) {
            int by = random.nextInt(h);
            graphics.fill(0, by, w, by + 1 + random.nextInt(3), 0x14FFFFFF);
        }

        // The morse square: static on a dark patch, a little brighter while the light is "on".
        int size = Math.max(18, w / 40);
        int mx = w - size - w / 16;
        int my = h - size - h / 12;
        boolean on = morseOn[age % morseOn.length];
        graphics.fill(mx, my, mx + size, my + size, 0xFF0A0E1E);
        RandomSource patch = RandomSource.create((long) (t / 3.0f) * 104729L);
        int cell = Math.max(2, size / 8);
        for (int cx = 0; cx < size; cx += cell) {
            for (int cy = 0; cy < size; cy += cell) {
                int level = on ? 70 + patch.nextInt(60) : 18 + patch.nextInt(24);
                graphics.fill(mx + cx, my + cy, mx + cx + cell, my + cy + cell, 0xFF000000 | level << 16 | level << 8 | (level + 10));
            }
        }
    }

    /** No keys do anything here. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
