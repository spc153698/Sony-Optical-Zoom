package hajijisonyopticalzoom.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Handles taking screenshots with shutter flash feedback.
 * When a screenshot is taken, a brief white flash appears on screen
 * to simulate a camera shutter effect.
 */
public class ScreenshotHandler {

    private static boolean flashActive = false;
    private static long flashStartNano = 0;
    private static final float FLASH_DURATION = 0.2f; // seconds

    public static void register() {
        HudRenderCallback.EVENT.register(ScreenshotHandler::onHudRender);
    }

    /**
     * Take a screenshot and trigger the shutter flash effect.
     * Screenshot is taken BEFORE the flash activates so the flash
     * doesn't get captured in the image.
     */
    public static void takeScreenshot() {
        Minecraft mc = Minecraft.getInstance();

        // Take the actual screenshot FIRST (before flash appears)
        try {
            Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(),
                    component -> mc.execute(() ->
                            mc.gui.getChat().addMessage(component)));
        } catch (Exception e) {
            mc.gui.getChat().addMessage(
                    Component.literal("\u00a7cScreenshot failed: " + e.getMessage()));
            return; // Don't trigger flash if screenshot failed
        }

        // Trigger flash AFTER screenshot is captured
        flashActive = true;
        flashStartNano = System.nanoTime();
    }

    /**
     * Render the shutter flash overlay on the HUD.
     */
    private static void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!flashActive) return;

        float elapsed = (System.nanoTime() - flashStartNano) / 1_000_000_000f;
        if (elapsed >= FLASH_DURATION) {
            flashActive = false;
            return;
        }

        // Flash intensity: quick peak then fade
        float intensity = 1.0f - (elapsed / FLASH_DURATION);
        int alpha = (int) (intensity * 120); // max ~47% opacity

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // White flash overlay (ARGB format)
        int color = (alpha << 24) | 0x00FFFFFF;
        guiGraphics.fill(0, 0, w, h, color);
    }
}
