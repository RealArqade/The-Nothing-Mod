package com.realarqade.thenothingmod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * The collectible lore items: plain items whose hover text tells a bit of the story. The monsters drop them (see the loot tables in
 * {@code data/the-nothing-mod/loot_table/entities}). To add one: add a line to {@link #DEFINITIONS}, then add its name and lore lines to
 * the lang file ({@code item.the-nothing-mod.<name>} and {@code item.the-nothing-mod.<name>.lore.0}, {@code .1}, ...) and its
 * texture and model files ({@code tools/GenerateLoreArt.ps1} makes them).
 */
public final class ModLoreItems {
    /** The item's name (also its ID) and how many lines of hover text it has. */
    private record Definition(String name, int loreLines) {
    }

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("id_0455", 3),
            new Definition("id_0171", 3),
            new Definition("id_0267", 3),
            new Definition("id_0605", 3),
            new Definition("id_0471", 3),
            new Definition("id_0456", 3),
            new Definition("id_0292", 2),
            new Definition("id_0224", 3),
            new Definition("id_0586", 2),
            new Definition("id_0485", 2),
            new Definition("id_0470", 3));

    /** Every lore item, in the order above. */
    public static final List<Item> ALL = registerAll();

    private ModLoreItems() {
    }

    private static List<Item> registerAll() {
        List<Item> items = new ArrayList<>();
        for (Definition definition : DEFINITIONS) {
            items.add(ModItems.register(ModItemIds.create(definition.name()), props -> new LoreItem(props, lore(definition)), properties()));
        }
        // The nineteen name slips: one per erased person, dropped by the monster that wears their outline.
        for (int n = 1; n <= NINETEEN; n++) {
            Definition slip = new Definition("id_0482" + n, 2);
            items.add(ModItems.register(ModItemIds.create("id_0482" + n), props -> new LoreItem(props, lore(slip)), properties()));
        }
        return List.copyOf(items);
    }

    /** How many people were taken out of the records. Collecting every slip is part of beating the mod. */
    public static final int NINETEEN = 19;

    private static List<Component> lore(Definition definition) {
        List<Component> lore = new ArrayList<>();
        for (int line = 0; line < definition.loreLines(); line++) {
            lore.add(Component.translatable("item.the-nothing-mod." + definition.name() + ".lore." + line).withStyle(ChatFormatting.GRAY));
        }
        return lore;
    }

    private static Item.Properties properties() {
        return new Item.Properties().stacksTo(1);
    }

    public static void initialize() {
        // Touching this class runs the registrations above.
    }
}
