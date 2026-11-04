package com.realarqade.thenothingmod.neoforge;

import com.realarqade.thenothingmod.TheNothingMod;
import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;

/** NeoForge's entrypoint (both sides). */
@Mod(TheNothingModNeoForge.MOD_ID)
public final class TheNothingModNeoForge {
    /** NeoForge mod ids cannot contain hyphens, so this differs from {@link TheNothingMod#MOD_ID} (which is the namespace of all the content and assets). */
    public static final String MOD_ID = "the_nothing_mod";

    /** Things the client entrypoint wants to run once all the content exists (its entity types and blocks are needed). */
    static final List<Runnable> AFTER_CONTENT = new ArrayList<>();

    private boolean contentRegistered;

    public TheNothingModNeoForge(IEventBus modBus) {
        NeoForgePlatform platform = new NeoForgePlatform();
        Platform.set(platform);

        modBus.addListener(platform::onBuildTab);
        modBus.addListener(platform::onAttributes);
        modBus.addListener(platform::onPayloads);
        // NeoForge only lets mods register blocks, items and entities while it fires its register event, so all content goes in here,
        // and everything that needs that content to exist (the rest of the start-up, the client's renderers) follows right after.
        modBus.addListener(RegisterEvent.class, event -> {
            if (!contentRegistered && event.getRegistryKey().equals(Registries.BLOCK)) {
                contentRegistered = true;
                TheNothingMod.registerContent();
                TheNothingMod.init();
                AFTER_CONTENT.forEach(Runnable::run);
            }
        });
    }
}
