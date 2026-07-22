package hunternif.mc.impl.atlas.registry;


import hunternif.mc.impl.atlas.client.texture.ITexture;

public class MarkerRenderInfo {
	public final ITexture tex;
	public int x, y;
	public int width, height;
	public int visibleX, visibleY;
	public int visibleWidth, visibleHeight;

	public MarkerRenderInfo(ITexture tex, int x, int y, int width, int height) {
		this(tex, x, y, width, height, 0, 0, width, height);
	}

	public MarkerRenderInfo(ITexture tex, int x, int y, int width, int height,
							int visibleX, int visibleY, int visibleWidth, int visibleHeight) {
		this.tex = tex;
		this.x = x; this.y = y;
		this.width = width; this.height = height;
		this.visibleX = visibleX; this.visibleY = visibleY;
		this.visibleWidth = visibleWidth; this.visibleHeight = visibleHeight;
	}

	public void scale(double factor) {
		x = (int)((1-factor) / 2f * width + x);
		y = (int)((1-factor) / 2f * height + y);
		width = (int)(factor * width);
		height = (int)(factor * height);
		visibleX = (int)(factor * visibleX);
		visibleY = (int)(factor * visibleY);
		visibleWidth = Math.max(1, (int)(factor * visibleWidth));
		visibleHeight = Math.max(1, (int)(factor * visibleHeight));
	}
}
