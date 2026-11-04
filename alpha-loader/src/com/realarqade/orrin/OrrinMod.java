package com.realarqade.orrin;

/**
 * What a mod for the Orrin loader looks like. Put a jar in the loader's {@code mods} folder with a file {@code orrin.mod} in its root
 * whose only line is the full name of a class that implements this. The loader calls {@link #onLaunch} once, inside the game's
 * process, before the game starts. From there a mod can use reflection on the game's classes (they are the game's own, with the
 * names Mojang gave them in this version: short, meaningless ones).
 */
public interface OrrinMod {
    /** @param game the class loader the game's own classes are loaded from */
    void onLaunch(ClassLoader game) throws Exception;
}
