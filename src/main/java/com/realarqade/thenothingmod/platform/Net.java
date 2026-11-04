package com.realarqade.thenothingmod.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Where the client side says what to do when the server's payloads arrive. Kept apart from the loader so that every loader can look a
 * handler up when a payload comes in, whichever order the registrations happened in.
 */
public final class Net {
    private static final Map<CustomPacketPayload.Type<?>, Consumer<Object>> CLIENT = new HashMap<>();

    private Net() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void onClient(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
        CLIENT.put(type, payload -> handler.accept((T) payload));
    }

    /** The payload types the client has handlers for (the loader registers its own receiver for each). */
    public static java.util.Set<CustomPacketPayload.Type<?>> clientTypes() {
        return java.util.Collections.unmodifiableSet(CLIENT.keySet());
    }

    /** Called by a loader (on the client's main thread) when a payload arrives. */
    public static void deliverToClient(CustomPacketPayload payload) {
        Consumer<Object> handler = CLIENT.get(payload.type());
        if (handler != null) {
            handler.accept(payload);
        }
    }
}
