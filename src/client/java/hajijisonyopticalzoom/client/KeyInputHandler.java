package hajijisonyopticalzoom.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Handles keybind registration and per-tick key state detection.
 * Supports: zoom key (LCtrl), zoom lock toggle (N), presets (Numpad 1/2/3),
 * screenshot (F2), max zoom up/down ([ ]).
 */
public class KeyInputHandler {
    /** Default key: Left Control — hold to zoom (or toggle lock) */
    private static final KeyMapping ZOOM_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.sonyopticalzoom.zoom",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_CONTROL,
                    "category.sonyopticalzoom"
            )
    );

    /** Zoom lock toggle: N */
    private static final KeyMapping ZOOM_LOCK_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.sonyopticalzoom.lock",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    "category.sonyopticalzoom"
            )
    );

    /** Preset 1: Numpad 1 */
    private static final KeyMapping PRESET_1_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.sonyopticalzoom.preset1",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_1,
                    "category.sonyopticalzoom"
            )
    );

    /** Preset 2: Numpad 2 */
    private static final KeyMapping PRESET_2_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.sonyopticalzoom.preset2",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_2,
                    "category.sonyopticalzoom"
            )
    );

    /** Preset 3: Numpad 3 */
    private static final KeyMapping PRESET_3_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.sonyopticalzoom.preset3",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_3,
                    "category.sonyopticalzoom"
            )
    );

    /** Screenshot: F2 */
    private static final KeyMapping SCREENSHOT_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.sonyopticalzoom.screenshot",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F2,
                    "category.sonyopticalzoom"
            )
    );

    /** Increase max zoom: ] */
    private static final KeyMapping MAX_ZOOM_UP_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.sonyopticalzoom.maxzoomup",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT_BRACKET,
                    "category.sonyopticalzoom"
            )
    );

    /** Decrease max zoom: [ */
    private static final KeyMapping MAX_ZOOM_DOWN_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.sonyopticalzoom.maxzoomdown",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_BRACKET,
                    "category.sonyopticalzoom"
            )
    );

    // Track previous key states to detect press edges
    private static boolean wasLockKeyDown = false;
    private static boolean wasPreset1Down = false;
    private static boolean wasPreset2Down = false;
    private static boolean wasPreset3Down = false;
    private static boolean wasScreenshotDown = false;
    private static boolean wasMaxZoomUpDown = false;
    private static boolean wasMaxZoomDownDown = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(KeyInputHandler::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        ZoomManager zoomManager = ZoomManager.getInstance();

        // === Zoom key (LCtrl): hold or toggle lock ===
        boolean isZoomKeyDown = ZOOM_KEY.isDown();
        if (!zoomManager.isZoomLocked()) {
            zoomManager.setZooming(isZoomKeyDown);
        }

        // === Zoom lock toggle (N): press to toggle ===
        boolean isLockKeyDown = ZOOM_LOCK_KEY.isDown();
        if (isLockKeyDown && !wasLockKeyDown) {
            zoomManager.toggleZoomLock();
        }
        wasLockKeyDown = isLockKeyDown;

        // === Preset keys (Numpad 1/2/3): only work when zooming ===
        boolean isPreset1Down = PRESET_1_KEY.isDown();
        boolean isPreset2Down = PRESET_2_KEY.isDown();
        boolean isPreset3Down = PRESET_3_KEY.isDown();

        if (zoomManager.isZooming()) {
            if (isPreset1Down && !wasPreset1Down) {
                zoomManager.setPresetZoom(zoomManager.getPreset1());
            }
            if (isPreset2Down && !wasPreset2Down) {
                zoomManager.setPresetZoom(zoomManager.getPreset2());
            }
            if (isPreset3Down && !wasPreset3Down) {
                zoomManager.setPresetZoom(zoomManager.getPreset3());
            }
        }

        wasPreset1Down = isPreset1Down;
        wasPreset2Down = isPreset2Down;
        wasPreset3Down = isPreset3Down;

        // === Screenshot (F2): press to capture ===
        boolean isScreenshotDown = SCREENSHOT_KEY.isDown();
        if (isScreenshotDown && !wasScreenshotDown) {
            ScreenshotHandler.takeScreenshot();
        }
        wasScreenshotDown = isScreenshotDown;

        // === Max zoom adjustment ([ / ]): press to change ===
        boolean isMaxZoomUpDown = MAX_ZOOM_UP_KEY.isDown();
        boolean isMaxZoomDownDown = MAX_ZOOM_DOWN_KEY.isDown();

        if (isMaxZoomUpDown && !wasMaxZoomUpDown) {
            zoomManager.increaseMaxZoom();
            showMaxZoomMessage(client, zoomManager);
        }
        if (isMaxZoomDownDown && !wasMaxZoomDownDown) {
            zoomManager.decreaseMaxZoom();
            showMaxZoomMessage(client, zoomManager);
        }

        wasMaxZoomUpDown = isMaxZoomUpDown;
        wasMaxZoomDownDown = isMaxZoomDownDown;
    }

    private static void showMaxZoomMessage(Minecraft client, ZoomManager zoomManager) {
        if (client.gui != null && client.gui.getChat() != null) {
            client.gui.getChat().addMessage(
                    Component.literal("§7[§fSony Zoom§7] §fMax zoom: §e"
                            + String.format("%.0fx", zoomManager.getMaxZoom())));
        }
    }

    public static KeyMapping getZoomKey() {
        return ZOOM_KEY;
    }
}
