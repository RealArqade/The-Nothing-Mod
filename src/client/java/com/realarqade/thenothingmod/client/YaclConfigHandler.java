package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.TheNothingMod;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;

import java.nio.file.Path;

/** The settings store backed by YetAnotherConfigLib (only loaded when that is installed; see {@link ConfigHandler#create}). */
final class YaclConfigHandler<T> implements ConfigHandler<T> {
    private static ConfigClassHandler<?> last;

    private final ConfigClassHandler<T> handler;

    YaclConfigHandler(Class<T> type, Path path) {
        this.handler = ConfigClassHandler.createBuilder(type)
                .id(TheNothingMod.id("config"))
                .serializer(config -> GsonConfigSerializerBuilder.create(config).setPath(path).build())
                .build();
        last = handler;
    }

    /** The underlying handler, for building the settings screen. */
    @SuppressWarnings("unchecked")
    static <T> ConfigClassHandler<T> classHandler() {
        return (ConfigClassHandler<T>) last;
    }

    @Override
    public T instance() {
        return handler.instance();
    }

    @Override
    public void load() {
        handler.load();
    }

    @Override
    public void save() {
        handler.save();
    }
}
