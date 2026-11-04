package com.realarqade.thenothingmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One monster from {@code the-nothing-mod.monsters.json}. Everything about the small monsters is data: add an entry there, run
 * {@code tools/GenerateMonsterArt.ps1} for its picture and name, and it exists.
 *
 * @param behavior   what it does, see {@link Behavior}
 * @param dims       "overworld", "nether" and/or "end"
 * @param underground appears in caves and tunnels instead of on the surface (the nether and end always do)
 * @param scare      jumpscare style when it reaches you, 0 for none
 * @param slip       which name-slip (1 to 19) it leaves behind, 0 for none; see {@link ModLoreItems}
 */
public record MonsterDef(String id, String name, Behavior behavior, List<String> dims, int texW, int texH, float height,
                         float hitW, float hitH, double speed, int lifetime, int avgMinutes, int cooldown, int min, int max,
                         boolean needsView, boolean behind, boolean underground, boolean nightOnly, String effect,
                         int effectTicks, int scare, String sound, float glitch, List<String> lines, List<Drop> drops, int slip) {

    public enum Behavior {
        /** Stands there; creeps closer whenever nobody is looking; leaves if you get near. */
        LURK,
        /** Stands far off and watches, leaves if you approach. */
        WATCH,
        /** Same as WATCH, but several of them at once. */
        CROWD,
        /** Chases you, but only moves while you are not looking at it. */
        HUNT,
        /** Appears behind you and follows, frozen while watched. */
        FOLLOW,
        /** Jumps closer each time you look away. */
        BLINK,
        /** Says a few lines to you, then leaves. */
        SPEAKER,
        /** Slowly ruins the plants and lights around it. */
        CORRUPT,
        /** Runs away from you. */
        FLEE
    }

    public record Drop(String item, float chance) {
    }

    static MonsterDef parse(JsonObject o) {
        float[] tex = floats(o, "texture", 24, 48);
        float[] hit = floats(o, "hitbox", 0.6f, tex[1] / 24f);
        List<String> dims = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("dims")) {
            dims.add(e.getAsString());
        }
        List<String> lines = new ArrayList<>();
        if (o.has("lines")) {
            for (JsonElement e : o.getAsJsonArray("lines")) {
                lines.add(e.getAsString());
            }
        }
        List<Drop> drops = new ArrayList<>();
        if (o.has("drops")) {
            for (JsonElement e : o.getAsJsonArray("drops")) {
                JsonArray pair = e.getAsJsonArray();
                drops.add(new Drop(pair.get(0).getAsString(), pair.get(1).getAsFloat()));
            }
        }
        return new MonsterDef(
                o.get("id").getAsString(), o.get("name").getAsString(),
                Behavior.valueOf(o.get("behavior").getAsString()), dims,
                (int) tex[0], (int) tex[1], o.has("height") ? o.get("height").getAsFloat() : hit[1],
                hit[0], hit[1], num(o, "speed", 0.0), (int) num(o, "lifetime", 2400),
                (int) num(o, "avgMinutes", 15), (int) num(o, "cooldown", 6000),
                (int) num(o, "min", 30), (int) num(o, "max", 44),
                flag(o, "needsView"), flag(o, "behind"), flag(o, "underground"), flag(o, "nightOnly"),
                o.has("effect") ? o.get("effect").getAsString() : "NONE", (int) num(o, "effectTicks", 100),
                (int) num(o, "scare", 0), o.has("sound") ? o.get("sound").getAsString() : "",
                (float) num(o, "glitch", 0.1), lines, drops, (int) num(o, "slip", 0));
    }

    private static double num(JsonObject o, String key, double fallback) {
        return o.has(key) ? o.get(key).getAsDouble() : fallback;
    }

    private static boolean flag(JsonObject o, String key) {
        return o.has(key) && o.get(key).getAsBoolean();
    }

    private static float[] floats(JsonObject o, String key, float a, float b) {
        if (!o.has(key)) {
            return new float[]{a, b};
        }
        JsonArray array = o.getAsJsonArray(key);
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat()};
    }
}
