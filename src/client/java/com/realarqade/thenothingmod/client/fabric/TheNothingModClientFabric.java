package com.realarqade.thenothingmod.client.fabric;

import com.realarqade.thenothingmod.client.ClientPlatform;
import com.realarqade.thenothingmod.client.TheNothingModClient;
import com.realarqade.thenothingmod.platform.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Fabric's client entrypoint. */
public final class TheNothingModClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlatform.set(new FabricClientPlatform());
        TheNothingModClient.init();
        // The shared code said which payloads it wants to hear; hand each one to Fabric.
        for (var type : Net.clientTypes()) {
            registerReceiver(type);
        }
    }

    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void registerReceiver(
            net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<T> type) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> Net.deliverToClient(payload));
    }
}
