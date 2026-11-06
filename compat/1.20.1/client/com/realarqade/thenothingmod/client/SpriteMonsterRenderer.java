package com.realarqade.thenothingmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.realarqade.thenothingmod.ApparitionProperties;
import com.realarqade.thenothingmod.StalkerProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;

/**
 * Draws any of the mod's monsters as a flat 2D picture that always turns to face the camera, full-bright. The picture comes
 * from {@link GlitchTexture}, a live texture whose pixels are corrupted about 60 times a second; this class only decides how
 * strong the glitching is, shakes the whole sprite a little, and makes it vanish for a split second now and then.
 * The closer you are, the stronger it gets, with short random bursts on top. How glitchy a monster is when you are far away is
 * {@code baseGlitch}; overall speed and strength are {@link StalkerProperties#GLITCH_SPEED} and
 * {@link StalkerProperties#GLITCH_STRENGTH}.
 */
public class SpriteMonsterRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Blocks away at which the glitching is back to its calmest. */
    private static final float GLITCH_RANGE = 24f;

    private final String name;
    private final ApparitionProperties.Sprite sprite;
    private final float baseGlitch;

    /**
     * @param name       the monster's name: its texture is {@code textures/entity/<name>.png}
     * @param baseGlitch how glitchy it is when far away, 0 (clean) to about 0.5
     */
    public SpriteMonsterRenderer(EntityRendererProvider.Context context, String name, ApparitionProperties.Sprite sprite, float baseGlitch) {
        super(context);
        this.name = name;
        this.sprite = sprite;
        this.baseGlitch = baseGlitch;
        this.shadowRadius = 0.0F; // a flat picture doesn't cast a round shadow
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return com.realarqade.thenothingmod.TheNothingMod.id("textures/entity/" + name + ".png");
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float glitch = Mth.clamp(1.0f - (float) Math.sqrt(this.entityRenderDispatcher.distanceToSqr(entity)) / GLITCH_RANGE, 0.0f, 1.0f);
        // the age includes the partial tick, so it moves smoothly every rendered frame, not just every game tick
        float age = (entity.tickCount + partialTick) * StalkerProperties.GLITCH_SPEED;
        float intensity = intensity(age, glitch);
        ResourceLocation texture = GlitchTexture.get(name, age, intensity);

        boolean vanished = RandomSource.create((long) age * 7919L + 104729L).nextFloat() < 0.04f * intensity; // ~20 times a second
        if (texture != null && !vanished) {
            float glitchScale = sprite.height() * 0.25f;
            float jitter = (RandomSource.create((long) (age * 3.0f) * 6151L + 11L).nextFloat() - 0.5f) * 0.08f * glitchScale * intensity;

            poseStack.pushPose();
            float camYaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();
            ClientCompat.rotate(poseStack, new Quaternionf().rotationY((float) Math.toRadians(180.0 - camYaw)));
            drawSprite(poseStack.last(), buffers.getBuffer(RenderType.entityTranslucent(texture)), jitter);
            poseStack.popPose();
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight); // name tag, if any
    }
    private float intensity(float age, float proximity) {
        // Every so often, a short burst of Word0807 glitching (a few tenths of a second).
        boolean burst = RandomSource.create(((long) age / 6) * 15485863L).nextInt(5) == 0;
        float base = baseGlitch + 0.6f * proximity + (burst ? 0.45f : 0.0f);
        return Mth.clamp(base * StalkerProperties.GLITCH_STRENGTH, 0.0f, 1.0f);
    }

    private void drawSprite(PoseStack.Pose pose, VertexConsumer consumer, float shiftX) {
        float half = sprite.width() / 2.0f;
        float height = sprite.height();
        vertex(consumer, pose, -half + shiftX, 0.0f, 0.0f, 1.0f);
        vertex(consumer, pose, half + shiftX, 0.0f, 1.0f, 1.0f);
        vertex(consumer, pose, half + shiftX, height, 1.0f, 0.0f);
        vertex(consumer, pose, -half + shiftX, height, 0.0f, 0.0f);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
        consumer.vertex(pose.pose(), x, y, 0.0f)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(pose.normal(), 0.0f, 1.0f, 0.0f)
                .endVertex();
    }
}
