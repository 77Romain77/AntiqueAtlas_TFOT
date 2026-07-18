package hunternif.mc.impl.atlas.client.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.Arrays;

/**
 * Collects GUI map quads that use the same tile texture and submits them in a
 * single draw call. The batching approach is inspired by Antique Atlas 4's
 * DrawBatcher and adapted to the Forge 1.20.1 renderer.
 *
 * <p>Primitive storage is used deliberately: a densely explored page can
 * contain thousands of subtiles, and allocating one command object per
 * subtile every frame would move much of the saved render time into the GC.</p>
 */
public final class TileRenderBatch {
    private static final int VALUES_PER_QUAD = 4;
    private static final int INITIAL_QUADS = 64;

    private final TileTexture texture;
    private int[] quads = new int[INITIAL_QUADS * VALUES_PER_QUAD];
    private int quadCount;

    public TileRenderBatch(TileTexture texture) {
        this.texture = texture;
    }

    /** Adds one square using destination X/Y followed by source U/V. */
    public void add(int x, int y, int u, int v) {
        int offset = quadCount * VALUES_PER_QUAD;
        if (offset + VALUES_PER_QUAD > quads.length) {
            quads = Arrays.copyOf(quads, quads.length * 2);
        }
        quads[offset] = x;
        quads[offset + 1] = y;
        quads[offset + 2] = u;
        quads[offset + 3] = v;
        quadCount++;
    }

    public int size() {
        return quadCount;
    }

    /** Draws every collected subtile while binding this texture only once. */
    public void draw(GuiGraphics graphics, int tileHalfSize) {
        if (quadCount == 0) return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture.getTexture());

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float textureWidth = texture.width();
        float textureHeight = texture.height();
        for (int quad = 0; quad < quadCount; quad++) {
            int offset = quad * VALUES_PER_QUAD;
            int x0 = quads[offset];
            int y0 = quads[offset + 1];
            int x1 = x0 + tileHalfSize;
            int y1 = y0 + tileHalfSize;
            float u0 = quads[offset + 2] / textureWidth;
            float v0 = quads[offset + 3] / textureHeight;
            float u1 = (quads[offset + 2] + 8.0F) / textureWidth;
            float v1 = (quads[offset + 3] + 8.0F) / textureHeight;

            builder.vertex(pose, x0, y1, 0.0F).uv(u0, v1).endVertex();
            builder.vertex(pose, x1, y1, 0.0F).uv(u1, v1).endVertex();
            builder.vertex(pose, x1, y0, 0.0F).uv(u1, v0).endVertex();
            builder.vertex(pose, x0, y0, 0.0F).uv(u0, v0).endVertex();
        }

        BufferUploader.drawWithShader(builder.end());
    }
}
