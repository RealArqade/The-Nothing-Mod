package com.realarqade.thenothingmod.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** The id of a payload and how to read one from a buffer. */
public record PayloadType<T extends Payload>(ResourceLocation id, Function<FriendlyByteBuf, T> reader) {
}