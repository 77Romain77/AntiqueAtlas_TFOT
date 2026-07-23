package hunternif.mc.impl.atlas.client.gui;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.config.ClientConfigStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small settings screen for the client-only options that matter most. */
public class GuiClientSettings extends Screen {
    private static final int[] SCAN_BUDGETS = {1, 2, 4, 8, 16, 32, 64};
    private final Screen parent;

    public GuiClientSettings(Screen parent) {
        super(Component.translatable("gui.antiqueatlas.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = height / 2 - 70;

        addRenderableWidget(Button.builder(itemLabel(), button -> {
            AntiqueAtlas.CONFIG.itemNeeded = !AntiqueAtlas.CONFIG.itemNeeded;
            button.setMessage(itemLabel());
        }).bounds(x, y, 220, 20).build());

        addRenderableWidget(Button.builder(browsingLabel(), button -> {
            AntiqueAtlas.CONFIG.doSaveBrowsingPos = !AntiqueAtlas.CONFIG.doSaveBrowsingPos;
            button.setMessage(browsingLabel());
        }).bounds(x, y + 24, 220, 20).build());

        addRenderableWidget(Button.builder(radiusLabel(), button -> {
            AntiqueAtlas.CONFIG.scanRadius = AntiqueAtlas.CONFIG.scanRadius >= 32
                    ? 0 : AntiqueAtlas.CONFIG.scanRadius + 1;
            button.setMessage(radiusLabel());
        }).bounds(x, y + 48, 108, 20).build());

        addRenderableWidget(Button.builder(budgetLabel(), button -> {
            AntiqueAtlas.CONFIG.clientScanBudget = nextBudget(AntiqueAtlas.CONFIG.clientScanBudget);
            button.setMessage(budgetLabel());
        }).bounds(x + 112, y + 48, 108, 20).build());

        addRenderableWidget(Button.builder(cloudsLabel(), button -> {
            AntiqueAtlas.CONFIG.showUnexploredClouds = !AntiqueAtlas.CONFIG.showUnexploredClouds;
            button.setMessage(cloudsLabel());
        }).bounds(x, y + 72, 220, 20).build());

        addRenderableWidget(Button.builder(bookSpineLabel(), button -> {
            AntiqueAtlas.CONFIG.showBookSpine = !AntiqueAtlas.CONFIG.showBookSpine;
            button.setMessage(bookSpineLabel());
        }).bounds(x, y + 96, 220, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 50, y + 132, 100, 20).build());
    }

    private Component itemLabel() {
        return option("gui.antiqueatlas.config.itemRequired", AntiqueAtlas.CONFIG.itemNeeded);
    }

    private Component browsingLabel() {
        return option("gui.antiqueatlas.config.saveBrowsing", AntiqueAtlas.CONFIG.doSaveBrowsingPos);
    }

    private Component radiusLabel() {
        return Component.translatable("gui.antiqueatlas.config.scanRadius", AntiqueAtlas.CONFIG.scanRadius);
    }

    private Component budgetLabel() {
        return Component.translatable("gui.antiqueatlas.config.scanBudget", AntiqueAtlas.CONFIG.clientScanBudget);
    }

    private Component cloudsLabel() {
        return option("gui.antiqueatlas.config.unexploredClouds",
                AntiqueAtlas.CONFIG.showUnexploredClouds);
    }

    private Component bookSpineLabel() {
        return option("gui.antiqueatlas.config.bookSpine", AntiqueAtlas.CONFIG.showBookSpine);
    }

    private static Component option(String key, boolean enabled) {
        return Component.translatable(key, Component.translatable(enabled ? "options.on" : "options.off"));
    }

    private static int nextBudget(int current) {
        for (int budget : SCAN_BUDGETS) if (budget > current) return budget;
        return SCAN_BUDGETS[0];
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 104, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ClientConfigStorage.save(AntiqueAtlas.CONFIG);
        minecraft.setScreen(parent);
    }
}
