package com.realarqade.thenothingmod.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class GlitchStalkerRenderState extends EntityRenderState {
    /** 0 when far away, up to 1 right next to the camera. The closer it is, the more the sprite glitches. */
    public float glitch;
}
