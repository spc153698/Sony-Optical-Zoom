package hajijisonyopticalzoom.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Renders a Sony-style zoom HUD with:
 * - W-T zoom bar (white background, black text)
 * - REC indicator when zoom is locked
 */
public class ZoomHudOverlay {

    // Panel layout
    private static final int PANEL_WIDTH = 140;
    private static final int PANEL_HEIGHT = 36;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_MARGIN = 4; // margin from screen edge

    // Bar layout offsets (relative to panel)
    private static final int BAR_OFFSET_X = PANEL_PADDING + 12;
    private static final int BAR_OFFSET_Y = 18;
    private static final int BAR_WIDTH = PANEL_WIDTH - PANEL_PADDING * 2 - 24;
    private static final int BAR_HEIGHT = 8;

    // Colors (ARGB)
    private static final int COLOR_BG = 0xE8FFFFFF;
    private static final int COLOR_BORDER = 0xFF888888;
    private static final int COLOR_BAR_BG = 0xFFD0D0D0;
    private static final int COLOR_BAR_FILL = 0xFF1A1A1A;
    private static final int COLOR_TEXT = 0xFF1A1A1A;
    private static final int COLOR_INDICATOR = 0xFF1A1A1A;
    private static final int COLOR_REC_RED = 0xFFE03030;

    public static void register() {
        HudRenderCallback.EVENT.register(ZoomHudOverlay::onHudRender);
    }

    private static void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        ZoomManager zoomManager = ZoomManager.getInstance();

        if (!zoomManager.isZooming() && zoomManager.getCurrentZoom() <= 1.01) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        float progress = zoomManager.getZoomProgress();
        double currentZoom = zoomManager.getCurrentZoom();

        // Dynamic panel position: left-center of screen
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int panelX = PANEL_MARGIN;
        int panelY = screenH / 2 - PANEL_HEIGHT / 2;
        int barX = panelX + BAR_OFFSET_X;
        int barY = panelY + BAR_OFFSET_Y;

        // === Zoom bar panel ===
        // Border
        guiGraphics.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, COLOR_BORDER);
        // White background
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_BG);

        // Zoom value text (centered, top line)
        String zoomText = String.format("%.1fx", currentZoom);
        int zoomTextX = panelX + (PANEL_WIDTH - mc.font.width(zoomText)) / 2;
        guiGraphics.drawString(mc.font, Component.literal(zoomText), zoomTextX, panelY + 4, COLOR_TEXT, false);

        // Lock indicator next to zoom text
        if (zoomManager.isZoomLocked()) {
            String lockText = " LOCK";
            guiGraphics.drawString(mc.font, Component.literal(lockText),
                    zoomTextX + mc.font.width(zoomText), panelY + 4, COLOR_REC_RED, false);
        }

        // "W" label
        guiGraphics.drawString(mc.font, Component.literal("W"), panelX + PANEL_PADDING, barY - 1, COLOR_TEXT, false);

        // "T" label
        int tX = panelX + PANEL_WIDTH - PANEL_PADDING - mc.font.width("T");
        guiGraphics.drawString(mc.font, Component.literal("T"), tX, barY - 1, COLOR_TEXT, false);

        // Bar track
        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, COLOR_BAR_BG);

        // Bar fill
        int fillWidth = (int) (progress * BAR_WIDTH);
        if (fillWidth > 0) {
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, COLOR_BAR_FILL);
        }

        // Center mark (Sony style)
        int centerX = barX + BAR_WIDTH / 2;
        guiGraphics.fill(centerX - 1, barY - 3, centerX + 1, barY + BAR_HEIGHT + 3, COLOR_INDICATOR);

        // Indicator line
        int indicatorX = barX + (int) (progress * BAR_WIDTH);
        guiGraphics.fill(indicatorX - 1, barY - 2, indicatorX + 1, barY + BAR_HEIGHT + 2, COLOR_INDICATOR);

        // Bar border
        guiGraphics.fill(barX, barY - 1, barX + BAR_WIDTH, barY, COLOR_BORDER);
        guiGraphics.fill(barX, barY + BAR_HEIGHT, barX + BAR_WIDTH, barY + BAR_HEIGHT + 1, COLOR_BORDER);
        guiGraphics.fill(barX - 1, barY - 1, barX, barY + BAR_HEIGHT + 1, COLOR_BORDER);
        guiGraphics.fill(barX + BAR_WIDTH, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, COLOR_BORDER);

        // === REC indicator (top-right) ===
        renderRecIndicator(guiGraphics, mc, zoomManager);
    }

    /**
     * Render REC indicator in the top-right corner when zoom is locked.
     */
    private static void renderRecIndicator(GuiGraphics guiGraphics, Minecraft mc, ZoomManager zoomManager) {
        if (!zoomManager.isZoomLocked()) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int recX = screenW - 60;
        int recY = 8;

        // Red dot (pulsing)
        long timeMs = System.currentTimeMillis();
        float pulse = (float) (0.5 + 0.5 * Math.sin(timeMs * 0.005));
        int dotAlpha = (int) (180 + 75 * pulse);
        int dotColor = (dotAlpha << 24) | 0x00E03030;
        guiGraphics.fill(recX, recY + 2, recX + 6, recY + 8, dotColor);

        // "REC" text
        guiGraphics.drawString(mc.font, Component.literal(" REC"), recX + 8, recY, COLOR_REC_RED, false);
    }
}
