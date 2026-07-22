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
 * Batches the small parchment-style clouds drawn over unexplored map cells.
 * Geometry is generated only when the terrain cache changes, then submitted
 * in a single draw call so the optional fog-of-war decoration stays cheap.
 */
public final class UnexploredCloudBatch {
    private static final int VALUES_PER_CELL = 3;
    private static final int INITIAL_CELLS = 128;

    private int[] cells = new int[INITIAL_CELLS * VALUES_PER_CELL];
    private int cellCount;

    public void clear() {
        cellCount = 0;
    }

    public void add(int x, int y, int variation) {
        int variant = Math.floorMod(variation, 7);
        if (variant == 0 || variant == 5) return;

        int offset = cellCount * VALUES_PER_CELL;
        if (offset + VALUES_PER_CELL > cells.length) {
            cells = Arrays.copyOf(cells, cells.length * 2);
        }
        cells[offset] = x;
        cells[offset + 1] = y;
        cells[offset + 2] = variant;
        cellCount++;
    }

    public int size() {
        return cellCount;
    }

    public void draw(GuiGraphics graphics, int cellSize) {
        if (cellCount == 0 || cellSize <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int cell = 0; cell < cellCount; cell++) {
            int offset = cell * VALUES_PER_CELL;
            drawCloud(builder, pose, cells[offset], cells[offset + 1],
                    cellSize, cells[offset + 2]);
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void drawCloud(BufferBuilder builder, Matrix4f pose,
                                  int x, int y, int size, int variation) {
        // Keep a little parchment visible between clouds and vary the pattern
        // deterministically so it does not shimmer while the map moves.
        int variant = variation;

        if (size <= 2) {
            quad(builder, pose, x, y, x + size, y + size,
                    0.76F, 0.68F, 0.53F, 0.34F);
            return;
        }

        // Soft brown shadow, followed by the pale cloud body. Coordinates
        // use an 8x8 logical pixel grid and scale with every atlas zoom level.
        rect(builder, pose, x, y, size, 1, 5, 7, 7,
                0.48F, 0.39F, 0.27F, 0.30F);
        rect(builder, pose, x, y, size, 1, 3, 7, 6,
                0.96F, 0.92F, 0.80F, 0.88F);
        rect(builder, pose, x, y, size, 0, 4, 2, 6,
                0.96F, 0.92F, 0.80F, 0.88F);
        rect(builder, pose, x, y, size, 6, 4, 8, 6,
                0.96F, 0.92F, 0.80F, 0.88F);

        if ((variant & 1) == 0) {
            rect(builder, pose, x, y, size, 2, 2, 4, 4,
                    0.98F, 0.95F, 0.85F, 0.92F);
            rect(builder, pose, x, y, size, 4, 1, 6, 4,
                    0.98F, 0.95F, 0.85F, 0.92F);
        } else {
            rect(builder, pose, x, y, size, 2, 1, 4, 4,
                    0.98F, 0.95F, 0.85F, 0.92F);
            rect(builder, pose, x, y, size, 4, 2, 6, 4,
                    0.98F, 0.95F, 0.85F, 0.92F);
        }

        if (variant == 3 || variant == 6) {
            rect(builder, pose, x, y, size, 3, 1, 5, 3,
                    1.00F, 0.97F, 0.88F, 0.72F);
        }
    }

    private static void rect(BufferBuilder builder, Matrix4f pose,
                             int cellX, int cellY, int cellSize,
                             int x0, int y0, int x1, int y1,
                             float red, float green, float blue, float alpha) {
        int left = cellX + Math.round(x0 * cellSize / 8.0F);
        int top = cellY + Math.round(y0 * cellSize / 8.0F);
        int right = cellX + Math.round(x1 * cellSize / 8.0F);
        int bottom = cellY + Math.round(y1 * cellSize / 8.0F);
        if (right <= left || bottom <= top) return;
        quad(builder, pose, left, top, right, bottom, red, green, blue, alpha);
    }

    private static void quad(BufferBuilder builder, Matrix4f pose,
                             int x0, int y0, int x1, int y1,
                             float red, float green, float blue, float alpha) {
        builder.vertex(pose, x0, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        builder.vertex(pose, x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        builder.vertex(pose, x1, y0, 0.0F).color(red, green, blue, alpha).endVertex();
        builder.vertex(pose, x0, y0, 0.0F).color(red, green, blue, alpha).endVertex();
    }
}
