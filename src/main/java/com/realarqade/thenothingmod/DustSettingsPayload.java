package com.realarqade.thenothingmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The dust's tuning values, sent in both directions (see {@link DevSync}):
 * <ul>
 *   <li>server to client: "placeholder text", sent when you join and whenever they change;</li>
 *   <li>client to server: a dev user asking to change them. The server checks who is asking before it does anything.</li>
 * </ul>
 */
public record DustSettingsPayload(int color, int poweredColor, boolean conductsRedstone, boolean powersDiagonally, boolean connectDiagonally)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DustSettingsPayload> TYPE = new CustomPacketPayload.Type<>(TheNothingMod.id("dust_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DustSettingsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DustSettingsPayload::color,
            ByteBufCodecs.VAR_INT, DustSettingsPayload::poweredColor,
            ByteBufCodecs.BOOL, DustSettingsPayload::conductsRedstone,
            ByteBufCodecs.BOOL, DustSettingsPayload::powersDiagonally,
            ByteBufCodecs.BOOL, DustSettingsPayload::connectDiagonally,
            DustSettingsPayload::new);

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
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
