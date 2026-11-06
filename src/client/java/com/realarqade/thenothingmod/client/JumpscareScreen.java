package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.ModEntityIds;
import com.realarqade.thenothingmod.StalkerProperties;


import net.minecraft.client.gui.screens.Screen;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The jumpscare, shown when the monster catches you (the server sends {@code JumpscarePayload}; see
 * {@code GlitchStalkerEntity#catchPlayer}). It takes over the whole screen and can't be dismissed. It ends by itself, and the
 * server's kick (or death) lands right after.
 * <ul>
 *   <li><b>Picture</b>: the live-glitching monster, zoomed from big to huge, shaking harder and harder, with dim red echoes
 *   either side, static, and tearing bars.</li>
 *   <li><b>Flashes</b>: a few quick red/white flashes at fixed moments (never more than 4 in the whole scare).</li>
 *   <li><b>Sound</b>: several loud vanilla monster sounds, pitched down and stacked, with more hits as it goes.</li>
 * </ul>
 * "placeholder text" in the config (for photosensitivity or sensitive ears) removes the flashes, calms the shaking and
 * static, and plays the sound at 40% volume.
 */
final class JumpscareScreen extends DustScreen {
    /** The ticks (20 per second) at which the screen flashes, each fading out over 3 ticks. */
    private static final int[] FLASH_TICKS = {0, 14, 30, 42};

    private final int duration;
    private final Screen returnTo;
    private final boolean toneDown = DustConfig.get().toneDownJumpscare;
    private int age;
    private boolean started;

    /** @param returnTo the screen to show afterwards. Null in real play (back to the game); the dev test button passes the config screen. */
    JumpscareScreen(int duration, Screen returnTo) {
        this(duration, 0, ModEntityIds.ID_0193_NAME, returnTo);
    }

    /** @param style 0 is The Nothing's scare; 1 to 5 are the smaller monsters' (see {@code Jumpscares}) */
    JumpscareScreen(int duration, int style, String monster, Screen returnTo) {
        super(Component.empty());
        this.duration = Math.max(1, duration);
        this.style = style;
        this.monster = monster;
        this.returnTo = returnTo;
    }

    private final int style;
    private final String monster;

    @Override
    protected void init() {
        if (!started && style != 0) {
            started = true;
            layer(SoundEvents.WARDEN_SONIC_BOOM, 0.7f);
            layer(SoundEvents.GHAST_SCREAM, 0.45f + 0.05f * style);
            layer(SoundEvents.ENDERMAN_SCREAM, 0.5f);
        }
        if (!started) {
            started = true;
            // The Word0778 hit: everything at once.
            layer(SoundEvents.WARDEN_ROAR, 0.6f);
            layer(SoundEvents.GHAST_SCREAM, 0.5f);
            layer(SoundEvents.ENDERMAN_SCREAM, 0.55f);
            layer(SoundEvents.ELDER_GUARDIAN_CURSE, 0.7f);
            layer(SoundEvents.ENDER_DRAGON_GROWL, 0.6f);
            layer(SoundEvents.WARDEN_SONIC_BOOM, 0.9f);
        }
    }

    private void layer(SoundEvent sound, float pitch) {
        float volume = toneDown ? 0.4f : 1.0f;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (style != 0) {
            if (age >= duration) {
                minecraft.setScreenAndShow(returnTo);
            }
            return;
        }
        // More hits as it goes on, so it never settles.
        if (age == 12) {
            layer(SoundEvents.GHAST_SCREAM, 0.4f);
            layer(SoundEvents.WARDEN_ROAR, 0.5f);
        } else if (age == 24) {
            layer(SoundEvents.ENDERMAN_SCREAM, 0.4f);
            layer(SoundEvents.WARDEN_SONIC_BOOM, 0.8f);
        } else if (age == 36) {
            layer(SoundEvents.ELDER_GUARDIAN_CURSE, 0.5f);
            layer(SoundEvents.ENDER_DRAGON_GROWL, 0.45f);
        }
        if (age >= duration) {
            minecraft.setScreenAndShow(returnTo);
        }
    }

    @Override
    public void draw(Gfx graphics, int mouseX, int mouseY, float partialTick) {
        float t = age + partialTick;
        float progress = Mth.clamp(t / duration, 0.0f, 1.0f);
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        // A new random pattern about 60 times a second.
        RandomSource random = RandomSource.create((long) (t * 3.0f) * 92821L + 17L);

        graphics.fill(0, 0, w, h, 0xFF000000);

        if (style != 0) {
            extractStyle(graphics, t, progress, w, h, random);
            return;
        }

        Identifier texture = GlitchTexture.get(t, 1.0f);
        if (texture != null) {
            int texW = (int) StalkerProperties.TEXTURE_WIDTH;
            int texH = (int) StalkerProperties.TEXTURE_HEIGHT;
            float zoom = 0.95f + 0.9f * progress * progress;
            int drawHeight = (int) (h * zoom);
            int drawWidth = drawHeight * texW / texH;
            float shake = toneDown ? 3.0f : 6.0f + 14.0f * progress;
            int centre = w / 2 + (int) ((random.nextFloat() - 0.5f) * 2 * shake);
            int top = (h - drawHeight) / 2 + (int) ((random.nextFloat() - 0.5f) * 2 * shake);

            // Dim red echoes either side, then the figure itself.
            int echo = (int) (drawWidth * 1.7f);
            graphics.blit(texture, centre - echo - drawWidth / 2, top, 0.0f, 0.0f,
                    drawWidth, drawHeight, texW, texH, texW, texH, 0x66FF3030);
            graphics.blit(texture, centre + echo - drawWidth / 2, top, 0.0f, 0.0f,
                    drawWidth, drawHeight, texW, texH, texW, texH, 0x66FF3030);
            graphics.blit(texture, centre - drawWidth / 2, top, 0.0f, 0.0f,
                    drawWidth, drawHeight, texW, texH, texW, texH, 0xFFFFFFFF);
        }

        // Id0651 and tearing bars.
        int specks = toneDown ? 25 : 90 + (int) (120 * progress);
        for (int i = 0; i < specks; i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            int shade = 90 + random.nextInt(140);
            graphics.fill(x, y, x + 2 + random.nextInt(10), y + 1 + random.nextInt(3), 0x90000000 | shade << 16 | shade << 8 | shade);
        }
        int bars = toneDown ? 2 : 6;
        for (int i = 0; i < bars; i++) {
            int y = random.nextInt(h);
            graphics.fill(0, y, w, y + 2 + random.nextInt(6), random.nextBoolean() ? 0x55FF0000 : 0x33FFFFFF);
        }

        // A few quick flashes at fixed moments (none at all when toned down).
        if (!toneDown) {
            for (int i = 0; i < FLASH_TICKS.length; i++) {
                float since = t - FLASH_TICKS[i];
                if (since >= 0.0f && since < 3.0f) {
                    int alpha = (int) ((1.0f - since / 3.0f) * 255);
                    int colour = i == 0 ? 0xFFFFFF : 0xC00000;
                    graphics.fill(0, 0, w, h, alpha << 24 | colour);
                }
            }
        }

        // Fade to black at the very end, just before the kick.
        if (progress > 0.85f) {
            int alpha = (int) (((progress - 0.85f) / 0.15f) * 255);
            graphics.fill(0, 0, w, h, alpha << 24);
        }
    }

    /** The smaller monsters' scares, one look per style. All flash at most twice. */
    private void extractStyle(Gfx graphics, float t, float progress, int w, int h, RandomSource random) {
        Identifier texture = GlitchTexture.get(monster, t, 0.8f);
        int[] size = GlitchTexture.size(monster);
        if (texture == null) {
            return;
        }
        float ratio = (float) size[0] / size[1];
        float shake = toneDown ? 2.0f : 5.0f;
        int jx = (int) ((random.nextFloat() - 0.5f) * 2 * shake);
        int jy = (int) ((random.nextFloat() - 0.5f) * 2 * shake);
        switch (style) {
            case 1 -> { // slam: black for a beat, then the figure fills the screen
                if (t > 8) {
                    int dh = (int) (h * (1.1f + 0.15f * progress));
                    int dw = (int) (dh * ratio);
                    graphics.blit(texture, (w - dw) / 2 + jx, (h - dh) / 2 + jy, 0f, 0f,
                            dw, dh, size[0], size[1], size[0], size[1], 0xFFFFFFFF);
                }
            }
            case 2 -> { // close-up of the top third, creeping bigger, then a snap
                float zoom = progress < 0.8f ? 2.2f + 1.2f * progress : 6.0f;
                int dh = (int) (h * zoom);
                int dw = (int) (dh * ratio);
                // Draw the full picture so its head sits in the middle of the screen.
                graphics.blit(texture, (w - dw) / 2 + jx, (int) (h * 0.5f - dh * 0.12f) + jy, 0f, 0f,
                        dw, dh, size[0], size[1], size[0], size[1], 0xFFFFFFFF);
            }
            case 3 -> { // two pale eyes open in the dark, then it is there
                if (t < 14) {
                    int ex = w / 2;
                    int ey = h / 2;
                    int open = Math.min(10, (int) t);
                    int gap = Math.max(20, w / 14);
                    graphics.fill(ex - gap - 8, ey - open / 2, ex - gap + 8, ey + open / 2 + 1, 0xFFE8E4D8);
                    graphics.fill(ex + gap - 8, ey - open / 2, ex + gap + 8, ey + open / 2 + 1, 0xFFE8E4D8);
                } else {
                    int dh = (int) (h * 1.15f);
                    int dw = (int) (dh * ratio);
                    graphics.blit(texture, (w - dw) / 2 + jx, (h - dh) / 2 + jy, 0f, 0f,
                            dw, dh, size[0], size[1], size[0], size[1], 0xFFFFFFFF);
                }
            }
            case 4 -> { // five of it, flickering in one by one
                int dh = (int) (h * 0.9f);
                int dw = (int) (dh * ratio);
                int shown = Math.min(5, 1 + (int) (t / 4));
                for (int i = 0; i < shown; i++) {
                    int x = (int) (w * (i + 0.5f) / 5.0f) - dw / 2;
                    if (random.nextInt(8) != 0) {
                        graphics.blit(texture, x + jx, (h - dh) / 2 + jy, 0f, 0f,
                                dw, dh, size[0], size[1], size[0], size[1], 0xFFFFFFFF);
                    }
                }
            }
            default -> { // tear: horizontal strips sliding apart
                int dh = (int) (h * 1.05f);
                int dw = (int) (dh * ratio);
                int strips = 16;
                for (int i = 0; i < strips; i++) {
                    int y0 = i * dh / strips;
                    int y1 = (i + 1) * dh / strips;
                    int off = (int) ((i % 2 == 0 ? 1 : -1) * progress * w * 0.25f * (0.4f + random.nextFloat()));
                    graphics.blit(texture, (w - dw) / 2 + off, (h - dh) / 2 + y0,
                            0f, (float) y0 * size[1] / dh, dw, y1 - y0, size[0], Math.max(1, (y1 - y0) * size[1] / dh),
                            size[0], size[1], 0xFFFFFFFF);
                }
            }
        }
        if (!toneDown && t < 3) {
            graphics.fill(0, 0, w, h, (int) ((1.0f - t / 3.0f) * 255) << 24 | 0xFFFFFF);
        }
        if (progress > 0.9f) {
            graphics.fill(0, 0, w, h, (int) (((progress - 0.9f) / 0.1f) * 255) << 24);
        }
    }

    /** Nothing gets you out of it: no Escape, no keys. It ends by itself. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean onKey(int key) {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
