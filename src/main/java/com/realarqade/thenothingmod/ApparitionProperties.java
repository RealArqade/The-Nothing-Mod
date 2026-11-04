package com.realarqade.thenothingmod;

/**
 * Everything you might want to tweak about the three smaller monsters, in one place. (The big one, The Nothing, is in
 * {@link StalkerProperties}.) Timings are in ticks, 20 per second.
 * <p>
 * Each monster is a flat picture. The picture's shape comes from its {@link Sprite}: draw the texture with the same
 * width:height ratio as the pixel size given there, or it looks stretched.
 */
public final class ApparitionProperties {
    private ApparitionProperties() {
    }

    /** A texture's pixel size (only the ratio matters) and how tall it is drawn, in blocks. The width follows. */
    public record Sprite(float textureWidth, float textureHeight, float height) {
        public float width() {
            return height * textureWidth / textureHeight;
        }
    }

    /** The big one, so the renderer can treat all four the same way. */
    public static final Sprite NOTHING_SPRITE = new Sprite(
            StalkerProperties.TEXTURE_WIDTH, StalkerProperties.TEXTURE_HEIGHT, StalkerProperties.SPRITE_HEIGHT);

    // --- The Surveyor: stands very far away on open ground, watching, and never does anything ---
    public static final Sprite SURVEYOR_SPRITE = new Sprite(32, 128, 4.0f);
    public static final float SURVEYOR_HITBOX_WIDTH = 0.7f;
    public static final float SURVEYOR_HITBOX_HEIGHT = 4.0f;
    public static final int SURVEYOR_AVERAGE_MINUTES = 6;
    public static final int SURVEYOR_COOLDOWN_TICKS = 3600;
    public static final int SURVEYOR_SPAWN_DISTANCE_MIN = 70;
    public static final int SURVEYOR_SPAWN_DISTANCE_MAX = 90;
    /** It disappears after this long (1800 ticks = 90 seconds)... */
    public static final int SURVEYOR_LIFETIME_TICKS = 1800;
    /** ...or as soon as a player gets this close. */
    public static final double SURVEYOR_VANISH_DISTANCE = 28.0;

    // --- The Girl: appears behind you and follows. She only moves while you aren't looking at her ---
    public static final Sprite GIRL_SPRITE = new Sprite(20, 48, 1.2f);
    public static final float GIRL_HITBOX_WIDTH = 0.5f;
    public static final float GIRL_HITBOX_HEIGHT = 1.2f;
    public static final double GIRL_MOVEMENT_SPEED = 0.25;
    public static final int GIRL_AVERAGE_MINUTES = 12;
    public static final int GIRL_COOLDOWN_TICKS = 6000;
    public static final int GIRL_SPAWN_DISTANCE_MIN = 28;
    public static final int GIRL_SPAWN_DISTANCE_MAX = 36;
    public static final int GIRL_LIFETIME_TICKS = 3600;
    /** What happens when she reaches you: everything goes dark and slow for a while. She does no damage. */
    public static final int GIRL_DARKNESS_TICKS = 200;
    public static final int GIRL_SLOWNESS_TICKS = 100;
    public static final double GIRL_WATCH_RANGE = 64.0;
    public static final double GIRL_WATCH_CONE = 0.5;

    // --- The Word0754: walks from torch to torch at night and puts them out ---
    public static final Sprite LAMPLIGHTER_SPRITE = new Sprite(28, 64, 2.0f);
    public static final float LAMPLIGHTER_HITBOX_WIDTH = 0.7f;
    public static final float LAMPLIGHTER_HITBOX_HEIGHT = 2.0f;
    public static final double LAMPLIGHTER_MOVEMENT_SPEED = 0.22;
    public static final int LAMPLIGHTER_AVERAGE_MINUTES = 8;
    public static final int LAMPLIGHTER_COOLDOWN_TICKS = 4800;
    public static final int LAMPLIGHTER_SPAWN_DISTANCE_MIN = 18;
    public static final int LAMPLIGHTER_SPAWN_DISTANCE_MAX = 26;
    public static final int LAMPLIGHTER_LIFETIME_TICKS = 2400;
    /** How far from itself it looks for a light to put out. */
    public static final int LAMPLIGHTER_SCAN_RADIUS = 14;
    /** It leaves if a player gets this close. */
    public static final double LAMPLIGHTER_VANISH_DISTANCE = 3.0;
}
