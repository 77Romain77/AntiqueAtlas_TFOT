package hunternif.mc.impl.atlas.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import org.lwjgl.opengl.GL11;

class ProgressBarOverlay {
    /**
     * Total width of the progress bar.
     */
    private final int barWidth;

    /**
     * Total height of the progress bar.
     */
    private final int barHeight;

    private final Font textRenderer;

    public ProgressBarOverlay(int barWidth, int barHeight) {
        this.barWidth = barWidth;
        this.barHeight = barHeight;
        textRenderer = Minecraft.getInstance().font;
    }

    /**
     * Render progress bar on the screen.
     */
    public void draw(GuiGraphics matrices, int x, int y) {
        ExportUpdateListener l = ExportUpdateListener.INSTANCE;

        int headerWidth = this.textRenderer.width(l.header);
        matrices.drawString(this.textRenderer, l.header, (int) (x + (barWidth - headerWidth) / 2F), y - 14, 0xffffff);
        int statusWidth = this.textRenderer.width(l.status);
        matrices.drawString(this.textRenderer, l.status, (int) (x + (barWidth - statusWidth) / 2F), y, 0xffffff);
        y += 14;

        float p = l.currentProgress / l.maxProgress;
        if (l.maxProgress < 0)
            p = 0;

//        RenderSystem.disableTexture();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder vb = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        vb.addVertex(x, y, 0).setColor(0.5f, 0.5f, 0.5f, 1);
        vb.addVertex(x, y + barHeight, 0).setColor(0.5f, 0.5f, 0.5f, 1);
        vb.addVertex(x + barWidth, y + barHeight, 0).setColor(0.5f, 0.5f, 0.5f, 1);
        vb.addVertex(x + barWidth, y, 0).setColor(0.5f, 0.5f, 0.5f, 1);

        vb.addVertex(x, y, 0).setColor(0.5f, 1, 0.5f, 1);
        vb.addVertex(x, y + barHeight, 0).setColor(0.5f, 1, 0.5f, 1);
        vb.addVertex(x + barWidth * p, y + barHeight, 0).setColor(0.5f, 1, 0.5f, 1);
        vb.addVertex(x + barWidth * p, y, 0).setColor(0.5f, 1, 0.5f, 1);

		BufferUploader.drawWithShader(vb.buildOrThrow());

//        RenderSystem.enableTexture();
    }

}
