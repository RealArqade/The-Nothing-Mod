package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Payload;
import com.realarqade.thenothingmod.platform.PayloadType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from the server to one player: "take over the screen and scare them now". The client side
 * ({@code JumpscareScreen}) does the drawing and the sound; this carries how long it lasts, which of the scare styles to use
 * (0 is The Nothing's own, 1 to 5 are the others, see {@link Jumpscares}) and whose picture to show.
 */
public record JumpscarePayload(int durationTicks, int style, String monster) implements Payload {
    public static final PayloadType<JumpscarePayload> TYPE = new PayloadType<>(TheNothingMod.id("jumpscare"),
            buf -> new JumpscarePayload(buf.readVarInt(), buf.readVarInt(), buf.readUtf()));
    public static final PayloadType<JumpscarePayload> CODEC = TYPE;

    public JumpscarePayload(int durationTicks) {
        this(durationTicks, 0, ModEntityIds.ID_0193_NAME);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(durationTicks);
        buf.writeVarInt(style);
        buf.writeUtf(monster);
    }

    @Override
    public ResourceLocation payloadId() {
        return TYPE.id();
    }
}
