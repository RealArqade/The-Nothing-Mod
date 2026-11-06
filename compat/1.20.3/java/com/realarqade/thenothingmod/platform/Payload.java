package com.realarqade.thenothingmod.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** A network message of the mod (this Minecraft version has no common payload codec, so the mod keeps its own small interface). */
public interface Payload {
    void write(FriendlyByteBuf buf);

    ResourceLocation payloadId();
}