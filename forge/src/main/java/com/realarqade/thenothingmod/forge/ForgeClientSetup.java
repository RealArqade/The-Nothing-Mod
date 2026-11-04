package com.realarqade.thenothingmod.forge;

import com.realarqade.thenothingmod.client.ClientPlatform;
import com.realarqade.thenothingmod.client.TheNothingModClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

/** Forge's client-side start-up. Only loaded on the client (the common entrypoint checks which side it is on). */
final class ForgeClientSetup {
    private ForgeClientSetup() {
    }

    static void init(ForgePlatform platform) {
        ForgeClientPlatform clientPlatform = new ForgeClientPlatform(platform);
        ClientPlatform.set(clientPlatform);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(clientPlatform::onRenderers);
        RegisterColorHandlersEvent.Block.BUS.addListener(clientPlatform::onTints);
        // Runs once the content exists (it needs the entity types and blocks).
        TheNothingModForge.AFTER_CONTENT.add(TheNothingModClient::init);
    }
}
