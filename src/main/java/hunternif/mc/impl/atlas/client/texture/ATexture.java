package hunternif.mc.impl.atlas.client.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * An abstract base class, which implements the ITexture interface using
 * the DrawHelper.drawTexture method provided by minecraft code.
 */
public abstract class ATexture implements ITexture {
    final ResourceLocation texture;
    final boolean autobind;

    private final RenderType textLayer;
    private final RenderType entitySolidLayer;
    private final RenderType entityTranslucentLayer;

    public ATexture(ResourceLocation texture) {
        this(texture, true);
    }

    public ATexture(ResourceLocation texture, boolean autobind) {
        this.texture = texture;
        this.autobind = autobind;
        this.textLayer = RenderType.text(texture);
        this.entitySolidLayer = RenderType.entitySolid(texture);
        this.entityTranslucentLayer = RenderType.entityNoOutline(texture);
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void bind() {
        RenderSystem.setShaderTexture(0, texture);
    }

    public void draw(GuiGraphics matrices, int x, int y) {
        draw(matrices, x, y, width(), height());
    }

    public void draw(GuiGraphics matrices, int x, int y, int width, int height) {
        draw(matrices, x, y, width, height, 0, 0, this.width(), this.height());
    }

    public void draw(GuiGraphics matrices, int x, int y, int u, int v, int regionWidth, int regionHeight) {
        draw(matrices, x, y, regionWidth, regionHeight, u, v, regionWidth, regionHeight);
    }

    public void draw(GuiGraphics matrices, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight) {
        if (autobind) {
            bind();
        }
        matrices.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, this.width(), this.height());
    }

    public void drawCenteredWithRotation(GuiGraphics matrices, int x, int y, int width, int height, float rotation) {
        matrices.pose().pushPose();
        matrices.pose().translate(x, y, 0);
        matrices.pose().mulPose(Axis.ZP.rotationDegrees(180 + rotation));
        matrices.pose().translate(-width / 2f, -height / 2f, 0f);

        draw(matrices, 0,0, width, height);

        matrices.pose().popPose();
    }

    public void drawWithLight(MultiBufferSource consumer, PoseStack matrices, int x, int y, int width, int height, int light) {
        drawWithLight(consumer, matrices, x, y, width, height, light, LightRenderMode.TEXT);
    }

    public void drawWithLight(MultiBufferSource consumer, PoseStack matrices, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int light) {
        drawWithLight(consumer, matrices, x, y, width, height, u, v, regionWidth,
                regionHeight, light, LightRenderMode.TEXT);
    }

    @Override
    public void drawWithLight(MultiBufferSource consumer, PoseStack matrices, int x, int y,
                              int width, int height, int light, LightRenderMode renderMode) {
        drawWithLight(consumer, matrices, x, y, width, height, 0, 0,
                this.width(), this.height(), light, renderMode);
    }

    @Override
    public void drawWithLight(MultiBufferSource consumer, PoseStack matrices, int x, int y,
                              int width, int height, int u, int v, int regionWidth,
                              int regionHeight, int light, LightRenderMode renderMode) {
        if (autobind) {
            bind();
        }
        drawTexturedQuadWithLight(consumer, matrices.last(), x, x + width, y, y + height,
                (u + 0.0F) / (float) this.width(),
                (u + (float) regionWidth) / (float) this.width(),
                (v + 0.0F) / (float) this.height(),
                (v + (float) regionHeight) / (float) this.height(), light, renderMode);
    }

    private void drawTexturedQuadWithLight(MultiBufferSource vertexConsumer, PoseStack.Pose pose,
                                           int x0, int x1, int y0, int y1,
                                           float u0, float u1, float v0, float v1,
                                           int light, LightRenderMode renderMode) {
        VertexConsumer consumer = vertexConsumer.getBuffer(layer(renderMode));
        vertex(consumer, pose, x0, y1, u0, v1, light, renderMode);
        vertex(consumer, pose, x1, y1, u1, v1, light, renderMode);
        vertex(consumer, pose, x1, y0, u1, v0, light, renderMode);
        vertex(consumer, pose, x0, y0, u0, v0, light, renderMode);
    }

    private RenderType layer(LightRenderMode renderMode) {
        return switch (renderMode) {
            case ENTITY_SOLID -> entitySolidLayer;
            case ENTITY_TRANSLUCENT -> entityTranslucentLayer;
            case TEXT -> textLayer;
        };
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int x, int y,
                               float u, float v, int light, LightRenderMode renderMode) {
        if (renderMode == LightRenderMode.TEXT) {
            consumer.vertex(pose.pose(), x, y, 0.0F)
                    .color(255, 255, 255, 255)
                    .uv(u, v)
                    .uv2(light)
                    .endVertex();
            return;
        }

        consumer.vertex(pose.pose(), x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 0.0F, -1.0F)
                .endVertex();
    }
}
