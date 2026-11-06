package com.realarqade.thenothingmod.platform;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Where the client side says what to do when the server's payloads arrive. Kept apart from the loader so that every loader can look a
 * handler up when a payload comes in, whichever order the registrations happened in.
 */
public final class Net {
    private static final Map<ResourceLocation, Consumer<Object>> CLIENT = new HashMap<>();
    private static final Map<ResourceLocation, PayloadType<?>> CLIENT_TYPES = new HashMap<>();

    private Net() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Payload> void onClient(PayloadType<T> type, Consumer<T> handler) {
        CLIENT.put(type.id(), payload -> handler.accept((T) payload));
        CLIENT_TYPES.put(type.id(), type);
    }

    /** The payload types the client has handlers for (the loader registers its own receiver for each). */
    public static java.util.Collection<PayloadType<?>> clientTypes() {
        return java.util.Collections.unmodifiableCollection(CLIENT_TYPES.values());
    }

    /** Called by a loader (on the client's main thread) when a payload arrives. */
    public static void deliverToClient(Payload payload) {
        Consumer<Object> handler = CLIENT.get(payload.payloadId());
        if (handler != null) {
            handler.accept(payload);
        }
    }
}