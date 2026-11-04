package com.realarqade.thenothingmod;

/**
 * Everything you might want to tweak about the monster, in one place.
 * <p>
 * Sprite size note: the 2D sprite is drawn {@link #SPRITE_WIDTH} x {@link #SPRITE_HEIGHT} blocks, so the texture needs that
 * same width:height ratio (currently 1 : 4, e.g. 48 x 192 pixels) or it will look stretched. If you redraw the texture
 * at a different shape, change {@link #TEXTURE_WIDTH} / {@link #TEXTURE_HEIGHT} (just their ratio matters) and the
 * sprite width follows.
 */
public final class StalkerProperties {
    private StalkerProperties() {
    }

    // --- Stats ---
    public static final double MAX_HEALTH = 40.0;
    public static final double MOVEMENT_SPEED = 0.30;   // a zombie is 0.23
    public static final double ATTACK_DAMAGE = 6.0;     // 3 hearts
    public static final double FOLLOW_RANGE = 128.0;    // how far away it notices a player

    // --- Hitbox (what you can hit and bump into) ---
    public static final float HITBOX_WIDTH = 0.8f;
    public static final float HITBOX_HEIGHT = 3.75f;

    // --- Sprite (how big the drawn picture is, in blocks) ---
    /** The texture's pixel size. Only the ratio matters. */
    public static final float TEXTURE_WIDTH = 48f;
    public static final float TEXTURE_HEIGHT = 192f;
    /** How tall he looks, in blocks. A player is 1.8, an Enderman 2.9. The width follows from the texture's shape. */
    public static final float SPRITE_HEIGHT = 3.75f;
    public static final float SPRITE_WIDTH = SPRITE_HEIGHT * TEXTURE_WIDTH / TEXTURE_HEIGHT;

    // --- Glitch effect (drawn live by GlitchStalkerRenderer, not part of the picture) ---
    /** Multiplies how fast everything glitches. 1.0 is the default; try 0.5 for a calmer look or 2.0 for a frantic one. */
    public static final float GLITCH_SPEED = 1.0f;
    /** Multiplies how strong the glitching is, 0 (none) to about 2. It is always capped so it stays readable. */
    public static final float GLITCH_STRENGTH = 1.0f;

    // --- Appearing: it turns up at random, far away, and just stands there watching ---
    /** Turn this off to stop it appearing by itself (you can still /summon it). */
    public static final boolean SPAWNING_ENABLED = true;

    /** Whether the story places (see {@code StoryStructures}) generate in new chunks, and about 1 chunk in this many gets one. */
    public static final boolean STRUCTURES_ENABLED = true;
    public static final int STRUCTURE_CHANCE_OVERWORLD = 45;
    public static final int STRUCTURE_CHANCE_NETHER = 60;
    public static final int STRUCTURE_CHANCE_END = 30;
    /** About 1 chunk in this many gets a gate to one of the forty gated dimensions (or a hole to the void). */
    public static final int GATE_CHANCE = 35;
    /** About 1 chunk in this many, inside one of the fifty dimensions, gets one of that dimension's own landmarks. */
    public static final int LANDMARK_CHANCE = 12;
    /** On average, one appearance per player every this many minutes. It is random, so it can come sooner or later. */
    public static final int SPAWN_AVERAGE_MINUTES = 10;
    /** A player is left alone for this long after joining (20 ticks = 1 second). */
    public static final int SPAWN_GRACE_TICKS = 3600;
    /** After one appears for a player, no new one for this long. */
    public static final int SPAWN_COOLDOWN_TICKS = 6000;
    /** It appears between this many and that many blocks away, somewhere the player can see. */
    public static final int SPAWN_DISTANCE_MIN = 40;
    public static final int SPAWN_DISTANCE_MAX = 56;

    // --- Hunting ---
    /** It stands still for this long after appearing (2400 ticks = 2 minutes), then chases the nearest player. */
    public static final int CHASE_AFTER_TICKS = 2400;
    /** Chase speed, as a multiple of its movement speed. */
    public static final double CHASE_SPEED_MODIFIER = 1.3;
    /** How long the jumpscare plays before the kick (or the death) happens: 50 ticks = 2.5 seconds. */
    public static final int JUMPSCARE_TICKS = 50;
    /** It gives up and Word0802 this long after the chase starts (6000 ticks = 5 minutes). */
    public static final int GIVE_UP_AFTER_TICKS = 6000;
    /** It Word0802 if no player is within this many blocks. */
    public static final double VANISH_DISTANCE = 160.0;
    /** While chasing, it stops dead whenever a player is looking at it. Off: it simply runs you down. */
    public static final boolean FREEZE_WHEN_WATCHED = false;
    /** Only used if {@link #FREEZE_WHEN_WATCHED} is on: a player within this many blocks can see it. */
    public static final double WATCH_RANGE = 48.0;
    /** How central it must be on screen to count as "looked at": 1.0 is dead centre, 0.0 is 90 degrees off to the side. */
    public static final double WATCH_CONE = 0.55;
}
