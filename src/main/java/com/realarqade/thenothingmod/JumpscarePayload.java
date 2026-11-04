package com.realarqade.thenothingmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from the server to one player: "placeholder text". The client side
 * ({@code JumpscareScreen}) does the drawing and the sound; this carries how long it lasts, which of the scare styles to use
 * (0 is The Nothing's own, 1 to 5 are the others, see {@link Jumpscares}) and whose picture to show.
 */
public record JumpscarePayload(int durationTicks, int style, String monster) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<JumpscarePayload> TYPE = new CustomPacketPayload.Type<>(TheNothingMod.id("jumpscare"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JumpscarePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JumpscarePayload::durationTicks,
            ByteBufCodecs.VAR_INT, JumpscarePayload::style,
            ByteBufCodecs.STRING_UTF8, JumpscarePayload::monster,
            JumpscarePayload::new);

    public JumpscarePayload(int durationTicks) {
        this(durationTicks, 0, ModEntityIds.ID_0193_NAME);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
