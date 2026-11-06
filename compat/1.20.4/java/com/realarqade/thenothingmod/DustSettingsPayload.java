package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Payload;
import com.realarqade.thenothingmod.platform.PayloadType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * The dust's tuning values, sent in both directions (see {@link DevSync}):
 * <ul>
 *   <li>server to client: "these are the settings on this server", sent when you join and whenever they change;</li>
 *   <li>client to server: a dev user asking to change them. The server checks who is asking before it does anything.</li>
 * </ul>
 */
public record DustSettingsPayload(int color, int poweredColor, boolean conductsRedstone, boolean powersDiagonally, boolean connectDiagonally)
        implements Payload {
    public static final PayloadType<DustSettingsPayload> TYPE = new PayloadType<>(TheNothingMod.id("dust_settings"),
            buf -> new DustSettingsPayload(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
    public static final PayloadType<DustSettingsPayload> CODEC = TYPE;

    public static DustSettingsPayload of(SuspiciousDustProperties properties) {
        return new DustSettingsPayload(properties.color(), properties.poweredColor(), properties.conductsRedstone(),
                properties.powersDiagonally(), properties.connectDiagonally());
    }

    /** Whether these are already the current values (so there is nothing to apply and nothing to redraw). */
    public boolean matches(SuspiciousDustProperties properties) {
        return equals(of(properties));
    }

    public void applyTo(SuspiciousDustProperties properties) {
        properties.setColor(color);
        properties.setPoweredColor(poweredColor);
        properties.setConductsRedstone(conductsRedstone);
        properties.setPowersDiagonally(powersDiagonally);
        properties.setConnectDiagonally(connectDiagonally);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(color);
        buf.writeVarInt(poweredColor);
        buf.writeBoolean(conductsRedstone);
        buf.writeBoolean(powersDiagonally);
        buf.writeBoolean(connectDiagonally);
    }

    @Override
    public ResourceLocation payloadId() {
        return TYPE.id();
    }
}
