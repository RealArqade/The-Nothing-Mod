package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Three field-less messages from the server to one player: show the fake blue screen, crash the game on purpose, or show the ending.
 * What each does is client-side; the client may ignore the first two if the player switched them off in the mod's settings.
 */
public final class HauntPayloads {
    private HauntPayloads() {
    }

    public record Bsod() implements CustomPacketPayload {
        public static final Type<Bsod> TYPE = new Type<>(TheNothingMod.id("bluescreen"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Bsod> CODEC = StreamCodec.unit(new Bsod());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Crash() implements CustomPacketPayload {
        public static final Type<Crash> TYPE = new Type<>(TheNothingMod.id("crash"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Crash> CODEC = StreamCodec.unit(new Crash());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Ending() implements CustomPacketPayload {
        public static final Type<Ending> TYPE = new Type<>(TheNothingMod.id("ending"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ending> CODEC = StreamCodec.unit(new Ending());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "placeholder text" (see the client class DesktopFiles). The client does nothing unless the player allows it. */
    public record DesktopFile(int id) implements CustomPacketPayload {
        public static final Type<DesktopFile> TYPE = new Type<>(TheNothingMod.id("desktop_file"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopFile> CODEC =
                StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.VAR_INT, DesktopFile::id, DesktopFile::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "placeholder text": a few seconds of glitching and error text inside the game. Nothing is damaged. */
    public record Corruption() implements CustomPacketPayload {
        public static final Type<Corruption> TYPE = new Type<>(TheNothingMod.id("corruption"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Corruption> CODEC = StreamCodec.unit(new Corruption());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
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
