package com.realarqade.thenothingmod.neoforge;

import com.realarqade.thenothingmod.client.ClientPlatform;
import com.realarqade.thenothingmod.client.TheNothingModClient;
import net.neoforged.bus.api.IEventBus;

/** NeoForge's client-only start-up (called from the common entrypoint, only on the client). */
final class TheNothingModNeoForgeClient {
    private TheNothingModNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        NeoForgeClientPlatform platform = new NeoForgeClientPlatform();
        ClientPlatform.set(platform);
        modBus.addListener(platform::onRenderers);
        modBus.addListener(platform::onTints);
        modBus.addListener(platform::onSpriteSources);
        TheNothingModNeoForge.AFTER_CONTENT.add(TheNothingModClient::init);
    }
}