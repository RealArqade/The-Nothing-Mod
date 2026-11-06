package com.realarqade.thenothingmod.forge;

import com.realarqade.thenothingmod.TheNothingMod;
import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;

/** Forge's entrypoint (both sides). */
@Mod(TheNothingModForge.MOD_ID)
public final class TheNothingModForge {
    /** Forge mod ids cannot contain hyphens, so this differs from {@link TheNothingMod#MOD_ID} (the namespace of all content and assets). */
    public static final String MOD_ID = "the_nothing_mod";

    private boolean contentCreated;

    /** Things the client entrypoint wants to run once all the content exists. */
    static final List<Runnable> AFTER_CONTENT = new ArrayList<>();

    private boolean contentRegistered;

    public TheNothingModForge() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        var modBus = context.getModEventBus();
        ForgePlatform platform = new ForgePlatform();
        Platform.set(platform);
        Holder.platform = platform;

        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            ForgeClientSetup.init(platform, modBus);
        }
        modBus.addListener(platform::onBuildTab);
        modBus.addListener(platform::onAttributes);
        modBus.addListener((FMLCommonSetupEvent event) -> platform.buildChannel());
        // Blocks can only be created while Forge has the block registry open, so all the content is created in the block register event
        // (Forge puts blocks first). Registering is handed over per registry: each registry takes its own entries in its own event.
        modBus.addListener((RegisterEvent event) -> {
            if (!contentCreated && event.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK)) {
                contentCreated = true;
                TheNothingMod.registerContent();
                TheNothingMod.init();
                AFTER_CONTENT.forEach(Runnable::run);
            }
            platform.onRegister(event);
        });
    }

    /** Lets the client entrypoint reach the platform. */
    static final class Holder {
        static ForgePlatform platform;

        private Holder() {
        }
    }
}
