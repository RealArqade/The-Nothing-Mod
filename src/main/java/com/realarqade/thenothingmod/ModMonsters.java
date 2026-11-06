package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code the-nothing-mod.monsters.json} and registers one entity type per entry, all of them {@link ApparitionEntity}.
 */
public final class ModMonsters {
    public static final List<MonsterDef> DEFS = load();
    public static final Map<String, EntityType<ApparitionEntity>> TYPES = new HashMap<>();
    private static final Map<EntityType<?>, MonsterDef> BY_TYPE = new HashMap<>();

    static {
        for (MonsterDef def : DEFS) {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, TheNothingMod.id(def.id()));
            EntityType<ApparitionEntity> type = Platform.get().register(BuiltInRegistries.ENTITY_TYPE, key,
                    EntityType.Builder.of(ApparitionEntity::new, MobCategory.MONSTER)
                            .sized(def.hitW(), def.hitH())
                            .clientTrackingRange(10)
                            .build(Compat.entityKey(key)));
            TYPES.put(def.id(), type);
            BY_TYPE.put(type, def);
            Platform.get().registerAttributes(type, () -> ApparitionEntity.createAttributes(def));
        }
    }

    private ModMonsters() {
    }

    public static MonsterDef defOf(EntityType<?> type) {
        return BY_TYPE.get(type);
    }

    private static List<MonsterDef> load() {
        List<MonsterDef> list = new ArrayList<>();
        // The first fifty, then the two per dimension.
        for (String file : new String[]{"/the-nothing-mod.monsters.json", "/the-nothing-mod.dimension_monsters.json"}) {
            try (Reader reader = new InputStreamReader(ModMonsters.class.getResourceAsStream(file), StandardCharsets.UTF_8)) {
                for (JsonElement element : new Gson().fromJson(reader, JsonElement.class).getAsJsonArray()) {
                    list.add(MonsterDef.parse((JsonObject) element));
                }
            } catch (IOException | RuntimeException e) {
                TheNothingMod.LOGGER.error("placeholder text", file, e);
            }
        }
        return List.copyOf(list);
    }

    public static void initialize() {
        // Touching this class runs the registrations above.
    }
}
