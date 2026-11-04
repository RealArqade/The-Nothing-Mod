package com.realarqade.thenothingmod;

import com.realarqade.thenothingmod.platform.Platform;
import net.minecraft.server.level.ServerPlayer;

/**
 * The smaller monsters' jumpscares. Each style is a different shape, so they don't all feel the same (the client draws them,
 * see {@code JumpscareScreen}). None of them hurt, kick or kill; only The Nothing does that.
 * <ul>
 *   <li>1 <b>Slam</b>: black, then the figure fills the screen for a split second.</li>
 *   <li>2 <b>Close-up</b>: a slow zoom into its face, then a snap.</li>
 *   <li>3 <b>Eyes</b>: darkness, two pale eyes open, and it is suddenly right there.</li>
 *   <li>4 <b>Word0865</b>: five of it, side by side, flicker in.</li>
 *   <li>5 <b>Tear</b>: the picture gets sliced into strips that slide apart.</li>
 * </ul>
 */
public final class Jumpscares {
    public static final int STYLE_COUNT = 5;

    private Jumpscares() {
    }

    public static void send(ServerPlayer player, int style, String monsterId) {
        if (Platform.get().canSend(player, JumpscarePayload.TYPE)) {
            Platform.get().send(player, new JumpscarePayload(style == 2 ? 50 : 32, style, monsterId));
        }
    }
}
