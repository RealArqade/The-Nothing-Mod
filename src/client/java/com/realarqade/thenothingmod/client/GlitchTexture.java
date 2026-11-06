package com.realarqade.thenothingmod.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.realarqade.thenothingmod.ModEntityIds;
import com.realarqade.thenothingmod.TheNothingMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A live texture: a monster's single PNG, with its actual pixels corrupted again about 60 times a second and uploaded to the
 * graphics card. The PNG on disk never changes and doesn't need any animation frames. There is one of these per monster, found by
 * its name ({@code textures/entity/<name>.png}).
 * <p>
 * Each update starts from the clean picture and applies, in order: a red and cyan colour ghost, rows that tear sideways,
 * a band of distortion that slides down the picture, dropped-out rows, blocks that show the wrong part of the picture
 * (some inverted), single-pixel static, and thin scan bars. How much of each depends on {@code intensity} (0 to 1).
 * <p>
 * It is shared by every monster of the same kind, so several of them glitch in step. It reloads the PNG whenever resources
 * reload (F3+T), so redrawing the picture shows up without restarting.
 */
final class GlitchTexture {
    private static final Map<String, GlitchTexture> ALL = new HashMap<>();

    private final Identifier id;
    private final Identifier source;

    private ResourceManager loadedFrom;
    private DynamicTexture texture;
    private int width;
    private int height;
    private int[] base;   // the clean picture, as ARGB
    private int[] work;   // this frame's corrupted copy
    private long lastFrame = Long.MIN_VALUE;

    private GlitchTexture(String name) {
        this.id = TheNothingMod.id("dynamic/" + name + "_glitch");
        this.source = TheNothingMod.id("textures/entity/" + name + ".png");
    }

    /**
     * The live texture of the named monster for this moment, or null if the picture couldn't be loaded. Regenerates and uploads at
     * most once per ~60th of a second, however many times it is asked.
     */
    static Identifier get(String name, float age, float intensity) {
        return ALL.computeIfAbsent(name, GlitchTexture::new).update(age, intensity);
    }

    /** Pixel size of the named monster's picture once it has been loaded (by {@link #get}); {1, 1} before that. */
    static int[] size(String name) {
        GlitchTexture t = ALL.get(name);
        return t == null || t.width == 0 ? new int[]{1, 1} : new int[]{t.width, t.height};
    }

    /** The big one's texture (used by the jumpscare). */
    static Identifier get(float age, float intensity) {
        return get(ModEntityIds.ID_0193_NAME, age, intensity);
    }

    private Identifier update(float age, float intensity) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager != loadedFrom) {
            load(manager);
        }
        if (texture == null) {
            return null;
        }
        long frame = (long) (age * 3.0f);
        if (frame != lastFrame) {
            lastFrame = frame;
            glitch(frame, age, intensity);
            upload();
        }
        return id;
    }

    private void load(ResourceManager manager) {
        loadedFrom = manager;
        try (InputStream stream = manager.getResource(source).orElseThrow(() -> new IOException("missing " + source)).open();
             NativeImage image = NativeImage.read(stream)) {
            int w = image.getWidth();
            int h = image.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pixels[y * w + x] = image.getPixel(x, y);
                }
            }
            base = pixels;
            work = new int[w * h];
            if (texture == null || w != width || h != height) {
                if (texture != null) {
                    Minecraft.getInstance().getTextureManager().release(id);
                }
                width = w;
                height = h;
                texture = ClientCompat.newDynamicTexture("placeholder text" + id, w, h);
                Minecraft.getInstance().getTextureManager().register(id, texture);
            }
            lastFrame = Long.MIN_VALUE;
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.warn("placeholder text", source, e);
        }
    }

    private void upload() {
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels.setPixel(x, y, work[y * width + x]);
            }
        }
        texture.upload();
    }

    private void glitch(long frame, float age, float intensity) {
        RandomSource random = RandomSource.create(frame * 6151L + 7013L);
        int w = width;
        int h = height;
        int[] px = work;
        System.arraycopy(base, 0, px, 0, base.length);

        // Colour split: a red and a cyan ghost of the picture, a few pixels either side. Drawn first, so they tear with it.
        int split = 1 + (int) (intensity * Math.max(2, w / 10) * (0.4f + random.nextFloat()));
        ghost(px, w, h, -split, argb(110, 237, 28, 60));
        ghost(px, w, h, split, argb(110, 40, 255, 255));

        // Rows that tear sideways.
        int maxShift = Math.max(2, (int) (w * 0.4f * intensity));
        int bands = 1 + (int) (8 * intensity);
        for (int i = 0; i < bands; i++) {
            int y0 = random.nextInt(h);
            int bandHeight = 1 + random.nextInt(2 + (int) (6 * intensity));
            int dx = random.nextInt(maxShift * 2 + 1) - maxShift;
            for (int y = y0; y < Math.min(h, y0 + bandHeight); y++) {
                shiftRow(px, w, y, dx);
            }
        }

        // A band of distortion that slides down the picture continuously and wobbles whatever it passes over.
        float center = (age * 0.035f) % 1.4f - 0.2f; // fraction of the height from the top, running a little past both ends
        float half = 0.06f + 0.05f * intensity;
        for (int y = 0; y < h; y++) {
            float distance = Math.abs((y + 0.5f) / h - center);
            if (distance < half) {
                int shift = (int) (Mth.sin(age * 2.3f + y * 0.35f) * 0.3f * w * intensity * (1.0f - distance / half));
                if (shift != 0) {
                    shiftRow(px, w, y, shift);
                }
            }
        }

        // Dropped-out rows.
        int drops = (int) (random.nextFloat() * 4 * intensity);
        for (int i = 0; i < drops; i++) {
            int y = random.nextInt(h);
            java.util.Arrays.fill(px, y * w, y * w + w, 0);
        }

        // Corrupted blocks: they show the wrong part of the picture, and some come out inverted.
        int blocks = (int) (1 + 9 * intensity * intensity + (random.nextFloat() < intensity ? 2 : 0));
        for (int i = 0; i < blocks; i++) {
            int blockWidth = 3 + random.nextInt(Math.max(1, w - 3));
            int blockHeight = 1 + random.nextInt(9);
            if (blockWidth > w || blockHeight > h) {
                continue; // tiny pictures
            }
            int sx = random.nextInt(w - blockWidth + 1);
            int sy = random.nextInt(h - blockHeight + 1);
            int dx = random.nextInt(w - blockWidth + 1);
            int dy = random.nextInt(h - blockHeight + 1);
            boolean invert = random.nextInt(6) == 0;
            for (int y = 0; y < blockHeight; y++) {
                for (int x = 0; x < blockWidth; x++) {
                    int pixel = base[(sy + y) * w + sx + x];
                    if (invert && (pixel >>> 24) != 0) {
                        pixel = (pixel & 0xFF000000) | (~pixel & 0x00FFFFFF);
                    }
                    px[(dy + y) * w + dx + x] = pixel;
                }
            }
        }

        // Single-pixel static.
        int noise = (int) (6 + 70 * intensity);
        for (int i = 0; i < noise; i++) {
            int index = random.nextInt(h) * w + random.nextInt(w);
            px[index] = switch (random.nextInt(7)) {
                case 0, 1 -> argb(255, 237, 28, 36);   // red
                case 2 -> argb(255, 255, 255, 255);
                case 3 -> argb(255, 139, 18, 24);
                case 4 -> argb(255, 160, 160, 160);
                case 5 -> 0;                           // a hole
                default -> argb(255, 205, 205, 205);
            };
        }

        // Thin scan bars that flicker across.
        if (random.nextFloat() < 0.5f * intensity) {
            int y0 = random.nextInt(h);
            int barHeight = 1 + random.nextInt(2);
            int x0 = random.nextInt(Math.max(1, w / 2));
            int x1 = Math.min(w, x0 + w / 2 + random.nextInt(Math.max(1, w / 2)));
            int colour = random.nextBoolean() ? argb(130, 237, 28, 36) : argb(90, 255, 255, 255);
            for (int y = y0; y < Math.min(h, y0 + barHeight); y++) {
                for (int x = x0; x < x1; x++) {
                    px[y * w + x] = colour;
                }
            }
        }
    }

    /** Paints a tinted copy of the picture, moved {@code dx} pixels sideways, but only into empty space. */
    private void ghost(int[] px, int w, int h, int dx, int colour) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((base[y * w + x] >>> 24) < 200) {
                    continue;
                }
                int nx = x + dx;
                if (nx >= 0 && nx < w && (px[y * w + nx] >>> 24) == 0) {
                    px[y * w + nx] = colour;
                }
            }
        }
    }

    /** Slides a row sideways. What slides out of the picture is lost; what slides in from the edge is empty. */
    private static void shiftRow(int[] px, int w, int y, int dx) {
        int[] row = new int[w];
        System.arraycopy(px, y * w, row, 0, w);
        for (int x = 0; x < w; x++) {
            int sx = x - dx;
            px[y * w + x] = (sx >= 0 && sx < w) ? row[sx] : 0;
        }
    }

    private static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
