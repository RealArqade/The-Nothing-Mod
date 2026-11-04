package com.realarqade.thenothingmod.neoforge;

import com.realarqade.thenothingmod.client.ClientPlatform;
import com.realarqade.thenothingmod.client.DustConfig;
import com.realarqade.thenothingmod.client.TheNothingModClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** NeoForge's client-only entrypoint. */
@Mod(value = TheNothingModNeoForge.MOD_ID, dist = Dist.CLIENT)
public final class TheNothingModNeoForgeClient {
    public TheNothingModNeoForgeClient(IEventBus modBus, ModContainer container) {
        NeoForgeClientPlatform platform = new NeoForgeClientPlatform();
        ClientPlatform.set(platform);
        modBus.addListener(platform::onRenderers);
        modBus.addListener(platform::onTints);
        modBus.addListener(platform::onSpriteSources);
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> DustConfig.createScreen(parent));
        TheNothingModNeoForge.AFTER_CONTENT.add(() -> {
            TheNothingModClient.init();
            // The server's payloads that the client has a handler for were registered by the common code (they are delivered through Net).
        });
    }
}
