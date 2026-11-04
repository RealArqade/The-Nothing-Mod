package com.realarqade.thenothingmod;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The plain building blocks used by the structures, the lore and the monsters' places: wallpaper, Name 0230, slate, ash, the
 * registry's stone and so on. They come from {@code the-nothing-mod.blocks.json}, one row each:
 * {@code [id, name, texture pattern, colour 1, colour 2, light level, hardness, sound, flags]} ("t" in the flags: you can see through
 * the edges). {@code tools/GenerateBlockAssets.ps1} turns the list into textures, models, blockstates, loot tables and lang entries.
 */
public final class ModStoryBlocks {
    public record Row(String id, String name, int light, float strength, String sound, boolean see) {
    }

    public static final List<Row> ROWS = load();
    /** Every block here by its id, in list order. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final List<Item> ITEMS = new ArrayList<>();

    static {
        for (Row row : ROWS) {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .strength(row.strength(), Math.max(row.strength() * 3, 3.0f))
                    .sound(soundOf(row.sound()))
                    .lightLevel(state -> row.light());
            if (row.see()) {
                properties = properties.noOcclusion();
            }
            if (row.id().startsWith("gate_")) {
                properties = properties.noCollision();
                String target = row.id().equals("id_0432") ? null : row.id().substring("gate_".length());
                Block gate = ModBlocks.register(ModBlockIds.create(row.id()), p -> new ThresholdBlock(p, target), properties);
                BLOCKS.put(row.id(), gate);
                ITEMS.add(ModItems.register(ModItemIds.create(row.id()), p -> new BlockItem(gate, p), new Item.Properties()));
                continue;
            }
            Block block = ModBlocks.register(ModBlockIds.create(row.id()),
                    row.id().equals("id_0204") ? AltarBlock::new : Block::new, properties);
            BLOCKS.put(row.id(), block);
            ITEMS.add(ModItems.register(ModItemIds.create(row.id()), p -> new BlockItem(block, p), new Item.Properties()));
        }
    }

    private ModStoryBlocks() {
    }

    private static SoundType soundOf(String name) {
        return switch (name) {
            case "WOOD" -> SoundType.WOOD;
            case "GRAVEL" -> SoundType.GRAVEL;
            case "SAND" -> SoundType.SAND;
            case "GLASS" -> SoundType.GLASS;
            case "METAL" -> SoundType.METAL;
            case "WOOL" -> SoundType.WOOL;
            case "GRASS" -> SoundType.GRASS;
            default -> SoundType.STONE;
        };
    }

    private static List<Row> load() {
        List<Row> rows = new ArrayList<>();
        try (Reader reader = new InputStreamReader(
                ModStoryBlocks.class.getResourceAsStream("/the-nothing-mod.blocks.json"), StandardCharsets.UTF_8)) {
            for (JsonElement element : new Gson().fromJson(reader, JsonElement.class).getAsJsonArray()) {
                JsonArray r = element.getAsJsonArray();
                rows.add(new Row(r.get(0).getAsString(), r.get(1).getAsString(), r.get(5).getAsInt(), r.get(6).getAsFloat(),
                        r.get(7).getAsString(), r.get(8).getAsString().contains("t")));
            }
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.error("placeholder text", e);
        }
        return List.copyOf(rows);
    }

    public static void initialize() {
        // Touching this class runs the registrations above.
    }
}
