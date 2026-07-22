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
 * Batches parchment-style clouds drawn over unexplored map cells.
 * Candidate positions are thinned when the terrain cache changes so the
 * amount of translucent geometry remains stable across atlas zoom levels.
 */
public final class UnexploredCloudBatch {
    private static final int VALUES_PER_QUAD = 8;
    private static final int INITIAL_QUADS = 1024;

    /**
     * Pixel-art silhouettes on an 18x12 logical grid. Every entry is
     * {@code y, x0, x1}; repeated rows allow variants with two separate lobes.
     */
    private static final int[][][] CLOUD_ROWS = {
            {
                    {2, 7, 12}, {3, 5, 14}, {4, 4, 15}, {5, 2, 16},
                    {6, 0, 18}, {7, 0, 18}, {8, 1, 17}, {9, 3, 16},
                    {10, 5, 14}
            },
            {
                    {1, 5, 9}, {2, 4, 11}, {3, 3, 13}, {4, 2, 15},
                    {5, 0, 17}, {6, 0, 18}, {7, 1, 18}, {8, 2, 17},
                    {9, 4, 15}, {10, 6, 13}
            },
            {
                    {2, 3, 7}, {2, 10, 14}, {3, 2, 15}, {4, 1, 16},
                    {5, 0, 18}, {6, 0, 18}, {7, 1, 17}, {8, 2, 16},
                    {9, 4, 15}, {10, 6, 13}
            },
            {
                    {1, 10, 14}, {2, 7, 15}, {3, 5, 16}, {4, 3, 17},
                    {5, 1, 18}, {6, 0, 18}, {7, 0, 17}, {8, 1, 16},
                    {9, 3, 14}, {10, 5, 12}
            }
    };

    private int[] quads = new int[INITIAL_QUADS * VALUES_PER_QUAD];
    private int quadCount;
    private int cloudCount;

    public void clear() {
        quadCount = 0;
        cloudCount = 0;
    }

    public void add(int x, int y, int cellSize, int variation) {
        int seed = mix(variation);

        // Keep roughly the same number of clouds on screen at every zoom.
        // The old implementation retained 5/7 of all unknown cells, which
        // became particularly expensive when a cell was only one pixel wide.
        int sampleDivisor;
        if (cellSize >= 8) {
            sampleDivisor = 12;
        } else if (cellSize >= 4) {
            sampleDivisor = 48;
        } else if (cellSize >= 2) {
            sampleDivisor = 192;
        } else {
            sampleDivisor = 768;
        }
        if (Math.floorMod(seed, sampleDivisor) != 0) return;

        prepareCloud(x, y, cellSize, seed);
        cloudCount++;
    }

    public int size() {
        return cloudCount;
    }

    public void draw(GuiGraphics graphics, int cellSize) {
        if (quadCount == 0 || cellSize <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int index = 0; index < quadCount; index++) {
            int offset = index * VALUES_PER_QUAD;
            quad(builder, pose,
                    quads[offset], quads[offset + 1],
                    quads[offset + 2], quads[offset + 3],
                    quads[offset + 4], quads[offset + 5],
                    quads[offset + 6], quads[offset + 7]);
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private void prepareCloud(int x, int y, int cellSize, int seed) {
        int renderSize = Math.max(cellSize, 8);
        int variant = (seed >>> 12) & 3;
        int jitterX = ((seed >>> 16) & 3) - 1;
        int jitterY = ((seed >>> 18) & 3) - 1;
        int originX = x + cellSize / 2 - Math.round(9 * renderSize / 8.0F)
                + Math.round(jitterX * renderSize / 8.0F);
        int originY = y + cellSize / 2 - Math.round(6 * renderSize / 8.0F)
                + Math.round(jitterY * renderSize / 8.0F);
        int[][] rows = CLOUD_ROWS[variant];

        // One-pixel warm shadow gives the cloud a readable outline over the
        // parchment without introducing a dark rectangular background.
        for (int[] row : rows) {
            rect(originX, originY, renderSize,
                    row[1] + 1, row[0] + 1, row[2] + 1, row[0] + 2,
                    0.45F, 0.36F, 0.24F, 0.26F);
        }

        // Shade each horizontal band separately: bright rounded tops, a warm
        // parchment body and a slightly darker underside for extra volume.
        for (int[] row : rows) {
            int rowY = row[0];
            if (rowY <= 3) {
                rect(originX, originY, renderSize,
                        row[1], rowY, row[2], rowY + 1,
                        1.00F, 0.97F, 0.88F, 0.94F);
            } else if (rowY >= 9) {
                rect(originX, originY, renderSize,
                        row[1], rowY, row[2], rowY + 1,
                        0.88F, 0.81F, 0.68F, 0.90F);
            } else {
                rect(originX, originY, renderSize,
                        row[1], rowY, row[2], rowY + 1,
                        0.96F, 0.92F, 0.80F, 0.93F);
            }
        }

        // Small highlights and a detached wisp prevent the large silhouettes
        // from looking like repeated solid blobs.
        if ((variant & 1) == 0) {
            rect(originX, originY, renderSize, 5, 4, 9, 5,
                    1.00F, 0.99F, 0.93F, 0.58F);
            rect(originX, originY, renderSize, 11, 5, 14, 6,
                    1.00F, 0.98F, 0.90F, 0.46F);
            rect(originX, originY, renderSize, 1, 11, 5, 12,
                    0.77F, 0.68F, 0.53F, 0.28F);
        } else {
            rect(originX, originY, renderSize, 4, 5, 8, 6,
                    1.00F, 0.99F, 0.93F, 0.54F);
            rect(originX, originY, renderSize, 10, 4, 13, 5,
                    1.00F, 0.98F, 0.90F, 0.50F);
            rect(originX, originY, renderSize, 13, 11, 17, 12,
                    0.77F, 0.68F, 0.53F, 0.28F);
        }
    }

    private void rect(int cellX, int cellY, int cellSize,
                      int x0, int y0, int x1, int y1,
                      float red, float green, float blue, float alpha) {
        int left = cellX + Math.round(x0 * cellSize / 8.0F);
        int top = cellY + Math.round(y0 * cellSize / 8.0F);
        int right = cellX + Math.round(x1 * cellSize / 8.0F);
        int bottom = cellY + Math.round(y1 * cellSize / 8.0F);
        if (right <= left || bottom <= top) return;
        addQuad(left, top, right, bottom,
                Math.round(red * 255.0F), Math.round(green * 255.0F),
                Math.round(blue * 255.0F), Math.round(alpha * 255.0F));
    }

    private void addQuad(int x0, int y0, int x1, int y1,
                         int red, int green, int blue, int alpha) {
        int offset = quadCount * VALUES_PER_QUAD;
        if (offset + VALUES_PER_QUAD > quads.length) {
            quads = Arrays.copyOf(quads, quads.length * 2);
        }
        quads[offset] = x0;
        quads[offset + 1] = y0;
        quads[offset + 2] = x1;
        quads[offset + 3] = y1;
        quads[offset + 4] = red;
        quads[offset + 5] = green;
        quads[offset + 6] = blue;
        quads[offset + 7] = alpha;
        quadCount++;
    }

    private static void quad(BufferBuilder builder, Matrix4f pose,
                             int x0, int y0, int x1, int y1,
                             int red, int green, int blue, int alpha) {
        builder.vertex(pose, x0, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        builder.vertex(pose, x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        builder.vertex(pose, x1, y0, 0.0F).color(red, green, blue, alpha).endVertex();
        builder.vertex(pose, x0, y0, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        return value ^ value >>> 16;
    }
}
