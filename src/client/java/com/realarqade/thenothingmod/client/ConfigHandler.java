package com.realarqade.thenothingmod.client;

import java.nio.file.Path;

/** What the mod needs from a settings store. */
public interface ConfigHandler<T> {
    T instance();

    void load();

    void save();

    private static boolean has(String className) {
        try {
            Class.forName(className, false, ConfigHandler.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** Whether YetAnotherConfigLib is installed (it builds the settings screen and can also store the settings). */
    static boolean hasScreenLibrary() {
        return has("dev.isxander.yacl3.config.v2.api.ConfigClassHandler");
    }

    @SuppressWarnings("unchecked")
    static <T> ConfigHandler<T> create(Class<T> type, Path path) {
        if (hasScreenLibrary()) {
            try {
                // Loaded by name so that nothing refers to YetAnotherConfigLib's classes when it is missing.
                return (ConfigHandler<T>) Class.forName("com.realarqade.thenothingmod.client.YaclConfigHandler")
                        .getDeclaredConstructor(Class.class, Path.class).newInstance(type, path);
            } catch (ReflectiveOperationException | LinkageError e) {
                // fall through to the plain store
            }
        }
        return new GsonConfigHandler<>(type, path);
    }
}
