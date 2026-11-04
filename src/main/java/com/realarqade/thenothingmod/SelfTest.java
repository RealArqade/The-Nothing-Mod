package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * A development-only check, run when the environment variable THENOTHING_SELFTEST is set and the game is started from the dev
 * environment ({@code gradlew runServer}). It loads every extra dimension, places every story place and gate once, logs what
 * happened, and stops the server. It never runs in a release or without the variable.
 */
final class SelfTest {
    private SelfTest() {
    }

    static void registerIfRequested() {
        if (!Platform.get().isDevelopment() || System.getenv("THENOTHING_SELFTEST") == null
                || !Platform.get().isDedicatedServer()) {
            return;
        }
        Platform.get().onServerStarted(server -> {
            int placed = 0;
            int failed = 0;
            for (Dimensions.Dim dim : Dimensions.ALL) {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TheNothingMod.id(dim.id())));
                if (level == null) {
                    TheNothingMod.LOGGER.error("placeholder text", dim.id());
                    failed++;
                    continue;
                }
                level.getChunk(0, 0);
            }
            for (Dimensions.Dim dim : Dimensions.ALL) {
                if (dim.isVoid()) {
                    ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, TheNothingMod.id(dim.id())));
                    for (int a = -1; a <= 1; a++) {
                        for (int b = -1; b <= 1; b++) {
                            VoidWorlds.ensure(level, a, b);
                        }
                    }
                    int solid = 0;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = level.getMinY() + 1; y < 90; y++) {
                                if (!level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).isAir()) {
                                    solid++;
                                }
                            }
                        }
                    }
                    TheNothingMod.LOGGER.info("placeholder text", dim.id(), solid);
                }
            }
            int n = 0;
            for (StoryStructures.Scene scene : StoryStructures.SCENES) {
                String dim = scene.dim();
                ServerLevel level = dim.startsWith("dim_")
                        ? server.getLevel(ResourceKey.create(Registries.DIMENSION, TheNothingMod.id(dim.substring(4))))
                        : server.getLevel(dim.equals("nether") || dim.equals("gate_nether") ? Level.NETHER
                        : dim.equals("end") || dim.equals("gate_end") ? Level.END : Level.OVERWORLD);
                if (dim.startsWith("dim_v")) {
                    for (int a = -1; a <= 1; a++) {
                        for (int b = -1; b <= 1; b++) {
                            VoidWorlds.ensure(level, 40 + n * 2 + a, 40 + (n % 5) * 3 + b);
                        }
                    }
                }
                StoryStructures.Scene use = scene;
                if (dim.startsWith("gate_")) {
                    String real = dim.equals("gate_nether") ? "nether" : dim.equals("gate_end") ? "end" : dim.equals("gate_cellar") ? "cellar" : "overworld";
                    use = new StoryStructures.Scene(scene.id(), real, scene.w(), scene.d(), scene.h(), scene.decay(), scene.foundation(), scene.ops());
                }
                try {
                    boolean ok = StoryStructures.place(level, use, new ChunkPos(40 + n * 2, 40 + (n % 5) * 3), n % 4, RandomSource.create(n));
                    if (ok) {
                        placed++;
                    } else {
                        TheNothingMod.LOGGER.warn("placeholder text", scene.id());
                    }
                } catch (RuntimeException e) {
                    failed++;
                    TheNothingMod.LOGGER.error("placeholder text", scene.id(), e);
                }
                n++;
            }
            TheNothingMod.LOGGER.info("placeholder text", placed, failed,
                    Dimensions.ALL.size(), ModMonsters.DEFS.size());
            server.halt(false);
        });
    }
}
