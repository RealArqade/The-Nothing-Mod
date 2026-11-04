package com.realarqade.thenothingmod.fabric;

import com.realarqade.thenothingmod.TheNothingMod;
import com.realarqade.thenothingmod.platform.Platform;
import net.fabricmc.api.ModInitializer;

/** Fabric's entrypoint: says which loader this is, then runs the shared start-up. */
public final class TheNothingModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Platform.set(new FabricPlatform());
        TheNothingMod.registerContent();
        TheNothingMod.init();
    }
}
