package com.realarqade.thenothingmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Everything you might want to tweak about the dust block, in one place.
 * <p>
 * The "tuning" values ({@link #color()}, {@link #poweredColor()}, {@link #conductsRedstone()},
 * {@link #connectDiagonally()}) ship in {@value #BAKED_FILE_NAME} (a resource in the jar). The code defaults below are
 * only the fallback if that file, or a field in it, is missing. While developing, the config screen's "Dev tools" tab
 * changes them live and writes them back into that file, so whatever you settle on is what the next build ships. You can
 * also just edit the JSON by hand.
 */
public final class SuspiciousDustProperties {
    public static final String BAKED_FILE_NAME = "the-nothing-mod.dust.json";
    public static final int DEFAULT_COLOR = 0x9E0B14;
    public static final boolean DEFAULT_CONDUCTS_REDSTONE = false;
    public static final boolean DEFAULT_POWERS_DIAGONALLY = true;
    public static final boolean DEFAULT_CONNECT_DIAGONALLY = true;

    private volatile int color;
    private volatile int poweredColor;
    private volatile boolean conductsRedstone;
    private volatile boolean powersDiagonally;
    private volatile boolean connectDiagonally;
    private final SoundType soundType;

    public SuspiciousDustProperties(int color, int poweredColor, boolean conductsRedstone, boolean powersDiagonally,
                                    boolean connectDiagonally, SoundType soundType) {
        this.color = color & 0xFFFFFF;
        this.poweredColor = poweredColor & 0xFFFFFF;
        this.conductsRedstone = conductsRedstone;
        this.powersDiagonally = powersDiagonally;
        this.connectDiagonally = connectDiagonally;
        this.soundType = soundType;
    }

    /** Code defaults, overridden by the baked JSON in the jar if there is one. */
    public static SuspiciousDustProperties loadBaked() {
        SuspiciousDustProperties properties = new SuspiciousDustProperties(
                DEFAULT_COLOR, brighten(DEFAULT_COLOR), DEFAULT_CONDUCTS_REDSTONE, DEFAULT_POWERS_DIAGONALLY,
                DEFAULT_CONNECT_DIAGONALLY, SoundType.STONE);
        try (InputStream stream = SuspiciousDustProperties.class.getResourceAsStream("/" + BAKED_FILE_NAME)) {
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("color")) {
                        properties.color = parseColor(json.get("color").getAsString());
                    }
                    // No powered colour in the file: derive it from the normal colour, like the reset button does.
                    properties.poweredColor = json.has("powered_color")
                            ? parseColor(json.get("powered_color").getAsString())
                            : brighten(properties.color);
                    if (json.has("conducts_redstone")) {
                        properties.conductsRedstone = json.get("conducts_redstone").getAsBoolean();
                    }
                    if (json.has("powers_diagonally")) {
                        properties.powersDiagonally = json.get("powers_diagonally").getAsBoolean();
                    }
                    if (json.has("connect_diagonally")) {
                        properties.connectDiagonally = json.get("connect_diagonally").getAsBoolean();
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.warn("placeholder text", BAKED_FILE_NAME, e);
        }
        return properties;
    }

    private static int parseColor(String hex) {
        return Integer.parseInt(hex.replace("#", ""), 16) & 0xFFFFFF;
    }

    /** The JSON that gets baked into the jar. */
    public String toJson() {
        return String.format(
                "placeholder text",
                color, poweredColor, conductsRedstone, powersDiagonally, connectDiagonally);
    }

    /** Tint colour as 0xRRGGBB. The textures are grayscale, so this is the only place the colour comes from. */
    public int color() {
        return color;
    }

    public void setColor(int rgb) {
        this.color = rgb & 0xFFFFFF;
    }

    /** Colour at full signal strength (only used when {@link #conductsRedstone()} is on). */
    public int poweredColor() {
        return poweredColor;
    }

    public void setPoweredColor(int rgb) {
        this.poweredColor = rgb & 0xFFFFFF;
    }

    /** Whether the dust carries a redstone signal like vanilla dust. Off means it is purely decorative. */
    public boolean conductsRedstone() {
        return conductsRedstone;
    }

    public void setConductsRedstone(boolean conductsRedstone) {
        this.conductsRedstone = conductsRedstone;
    }

    /**
     * Whether powered dust also powers components (pistons, lamps, ...) diagonally next to it. Only has an effect while
     * {@link #conductsRedstone()} is on. A dust only powers the diagonals it isn't already pointing along: an isolated dot
     * powers all four, a straight line powers none, a corner powers the open outside diagonal.
     */
    public boolean powersDiagonally() {
        return powersDiagonally;
    }

    public void setPowersDiagonally(boolean powersDiagonally) {
        this.powersDiagonally = powersDiagonally;
    }

    /** Whether neighbouring dust on the diagonal links up (this is what makes rings and diamonds possible). */
    public boolean connectDiagonally() {
        return connectDiagonally;
    }

    public void setConnectDiagonally(boolean connectDiagonally) {
        this.connectDiagonally = connectDiagonally;
    }

    public SoundType soundType() {
        return soundType;
    }

    /**
     * The tint for a given signal strength: the normal colour at 0, fading to {@link #poweredColor()} at 15.
     * Strength 1 is already clearly "on", like vanilla dust.
     */
    public int colorForPower(int power) {
        if (!conductsRedstone || power <= 0) {
            return color;
        }
        float t = 0.35f + 0.65f * (Math.min(power, 15) - 1) / 14f;
        return mix(color, poweredColor, t);
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * A brighter version of a colour, same hue: most of the way to full brightness, slightly less saturated.
     * Used for the default powered colour and by the dev tools' "reset" button.
     */
    public static int brighten(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue;
        if (delta == 0) {
            hue = 0;
        } else if (max == r) {
            hue = ((g - b) / delta) % 6;
        } else if (max == g) {
            hue = (b - r) / delta + 2;
        } else {
            hue = (r - g) / delta + 4;
        }
        if (hue < 0) {
            hue += 6;
        }
        float saturation = (max == 0 ? 0 : delta / max) * 0.9f;
        float value = max + (1 - max) * 0.7f;

        float chroma = value * saturation;
        float x = chroma * (1 - Math.abs(hue % 2 - 1));
        float m = value - chroma;
        float r1, g1, b1;
        switch ((int) hue) {
            case 0 -> { r1 = chroma; g1 = x; b1 = 0; }
            case 1 -> { r1 = x; g1 = chroma; b1 = 0; }
            case 2 -> { r1 = 0; g1 = chroma; b1 = x; }
            case 3 -> { r1 = 0; g1 = x; b1 = chroma; }
            case 4 -> { r1 = x; g1 = 0; b1 = chroma; }
            default -> { r1 = chroma; g1 = 0; b1 = x; }
        }
        return (Math.round((r1 + m) * 255) << 16) | (Math.round((g1 + m) * 255) << 8) | Math.round((b1 + m) * 255);
    }

    /** The vanilla block settings: flat, walk-through, breaks instantly. */
    public BlockBehaviour.Properties toBlockProperties() {
        return BlockBehaviour.Properties.of()
                .noCollision()
                .instabreak()
                .sound(soundType)
                .pushReaction(Compat.PUSH_POPS);
    }
}
