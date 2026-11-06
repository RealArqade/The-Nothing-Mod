package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Payload;
import com.realarqade.thenothingmod.platform.PayloadType;
import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Field-less (and one tiny) messages from the server to one player: show the fake blue screen, crash the game on purpose, show the
 * ending, write a Desktop file, or show the fake corruption effect. What each does is client-side.
 */
public final class HauntPayloads {
    private HauntPayloads() {
    }

    public record Bsod() implements Payload {
        public static final PayloadType<Bsod> TYPE = new PayloadType<>(TheNothingMod.id("bluescreen"), buf -> new Bsod());
        public static final PayloadType<Bsod> CODEC = TYPE;

        @Override
        public void write(FriendlyByteBuf buf) {
        }

        @Override
        public ResourceLocation payloadId() {
            return TYPE.id();
        }
    }

    public record Crash() implements Payload {
        public static final PayloadType<Crash> TYPE = new PayloadType<>(TheNothingMod.id("crash"), buf -> new Crash());
        public static final PayloadType<Crash> CODEC = TYPE;

        @Override
        public void write(FriendlyByteBuf buf) {
        }

        @Override
        public ResourceLocation payloadId() {
            return TYPE.id();
        }
    }

    public record Ending() implements Payload {
        public static final PayloadType<Ending> TYPE = new PayloadType<>(TheNothingMod.id("ending"), buf -> new Ending());
        public static final PayloadType<Ending> CODEC = TYPE;

        @Override
        public void write(FriendlyByteBuf buf) {
        }

        @Override
        public ResourceLocation payloadId() {
            return TYPE.id();
        }
    }

    /** "Write Desktop file number id" (see the client class DesktopFiles). The client does nothing unless the player allows it. */
    public record DesktopFile(int id) implements Payload {
        public static final PayloadType<DesktopFile> TYPE = new PayloadType<>(TheNothingMod.id("desktop_file"), buf -> new DesktopFile(buf.readVarInt()));
        public static final PayloadType<DesktopFile> CODEC = TYPE;

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(id);
        }

        @Override
        public ResourceLocation payloadId() {
            return TYPE.id();
        }
    }

    /** "Show the fake corruption effect": a few seconds of glitching and error text inside the game. Nothing is damaged. */
    public record Corruption() implements Payload {
        public static final PayloadType<Corruption> TYPE = new PayloadType<>(TheNothingMod.id("corruption"), buf -> new Corruption());
        public static final PayloadType<Corruption> CODEC = TYPE;

        @Override
        public void write(FriendlyByteBuf buf) {
        }

        @Override
        public ResourceLocation payloadId() {
            return TYPE.id();
        }
    }

    public static void register() {
        Platform.get().registerClientbound(DesktopFile.TYPE, DesktopFile.CODEC);
        Platform.get().registerClientbound(Corruption.TYPE, Corruption.CODEC);
        Platform.get().registerClientbound(Bsod.TYPE, Bsod.CODEC);
        Platform.get().registerClientbound(Crash.TYPE, Crash.CODEC);
        Platform.get().registerClientbound(Ending.TYPE, Ending.CODEC);
    }
}