package com.realarqade.thenothingmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.realarqade.thenothingmod.TheNothingMod;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** The settings file read and written with Gson only (same layout YetAnotherConfigLib writes, so the two can share a file). */
final class GsonConfigHandler<T> implements ConfigHandler<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.ID_0651, Modifier.TRANSIENT).create();

    private final Class<T> type;
    private final Path path;
    private T instance;

    GsonConfigHandler(Class<T> type, Path path) {
        this.type = type;
        this.path = path;
        this.instance = fresh();
    }

    private T fresh() {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public T instance() {
        return instance;
    }

    @Override
    public void load() {
        if (!Files.isRegularFile(path)) {
            save();
            return;
        }
        try {
            T read = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), type);
            if (read != null) {
                instance = read;
            }
        } catch (IOException | RuntimeException e) {
            TheNothingMod.LOGGER.warn("[The Nothing Mod] Could not read {}; using the defaults.", path, e);
        }
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            TheNothingMod.LOGGER.warn("[The Nothing Mod] Could not write {}.", path, e);
        }
    }
}
