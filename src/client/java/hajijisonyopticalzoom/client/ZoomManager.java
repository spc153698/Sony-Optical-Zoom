package hajijisonyopticalzoom.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

/**
 * Manages the zoom state for the Sony-style optical zoom feature.
 * Handles smooth interpolation between zoom levels,
 * zoom lock mode, presets, and configurable max zoom.
 */
public class ZoomManager {
    private static final ZoomManager INSTANCE = new ZoomManager();

    public static final double MIN_ZOOM = 1.0;
    public static final double DEFAULT_MAX_ZOOM = 50.0;
    public static final double ABSOLUTE_MAX_ZOOM = 200.0;
    public static final double DEFAULT_FOV = 70.0;
    /** Default preset values */
    public static final double DEFAULT_PRESET_1 = 10.0;
    public static final double DEFAULT_PRESET_2 = 25.0;
    public static final double DEFAULT_PRESET_3 = 50.0;
    /** How fast the smooth interpolation catches up (higher = faster) */
    private static final float LERP_SPEED = 8.0f;
    /** Slower speed for preset transitions (simulates zoom motor) */
    private static final float PRESET_LERP_SPEED = 3.0f;
    /** Step size for max zoom adjustment */
    public static final double MAX_ZOOM_STEP = 10.0;

    /** Configurable max zoom limit */
    private double maxZoom = DEFAULT_MAX_ZOOM;
    /** Configurable preset zoom levels */
    private double preset1 = DEFAULT_PRESET_1;
    private double preset2 = DEFAULT_PRESET_2;
    private double preset3 = DEFAULT_PRESET_3;

    /** Target zoom multiplier set by scroll wheel */
    private double targetZoom = 1.0;
    /** Current smoothly interpolated zoom multiplier */
    private double currentZoom = 1.0;
    /** Whether the zoom key is currently held or locked */
    private boolean zooming = false;
    /** Whether zoom is locked (toggle mode) */
    private boolean zoomLocked = false;
    /** Whether we're transitioning to a preset target */
    private boolean presetTransition = false;
    /** Timestamp for frame-delta calculation */
    private long lastUpdateNano = System.nanoTime();

    private ZoomManager() {
        loadConfig();
    }

    public static ZoomManager getInstance() {
        return INSTANCE;
    }

    // === Configurable Max Zoom ===

    public double getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(double newMax) {
        this.maxZoom = Math.clamp(newMax, 10.0, ABSOLUTE_MAX_ZOOM);
        // If current target exceeds new max, clamp it
        if (targetZoom > maxZoom) {
            targetZoom = maxZoom;
        }
        saveConfig();
    }

    public void increaseMaxZoom() {
        setMaxZoom(maxZoom + MAX_ZOOM_STEP);
    }

    public void decreaseMaxZoom() {
        setMaxZoom(maxZoom - MAX_ZOOM_STEP);
    }

    // === Configurable Presets ===

    public double getPreset1() {
        return preset1;
    }

    public double getPreset2() {
        return preset2;
    }

    public double getPreset3() {
        return preset3;
    }

    public void setPreset(int index, double value) {
        double clamped = Math.clamp(value, MIN_ZOOM, maxZoom);
        switch (index) {
            case 1 -> preset1 = clamped;
            case 2 -> preset2 = clamped;
            case 3 -> preset3 = clamped;
            default -> throw new IllegalArgumentException("Preset index must be 1, 2, or 3");
        }
        saveConfig();
    }

    public double getPreset(int index) {
        return switch (index) {
            case 1 -> preset1;
            case 2 -> preset2;
            case 3 -> preset3;
            default -> throw new IllegalArgumentException("Preset index must be 1, 2, or 3");
        };
    }

    public void resetPresets() {
        preset1 = DEFAULT_PRESET_1;
        preset2 = DEFAULT_PRESET_2;
        preset3 = DEFAULT_PRESET_3;
        saveConfig();
    }

    // === Zoom State ===

    public boolean isZooming() {
        return zooming;
    }

    public void setZooming(boolean zooming) {
        this.zooming = zooming;
        if (!zooming && !zoomLocked) {
            targetZoom = 1.0;
        }
    }

    public boolean isZoomLocked() {
        return zoomLocked;
    }

    public void toggleZoomLock() {
        zoomLocked = !zoomLocked;
        if (zoomLocked) {
            zooming = true;
        } else {
            zooming = false;
            targetZoom = 1.0;
        }
    }

    public double getTargetZoom() {
        return targetZoom;
    }

    public double getCurrentZoom() {
        return currentZoom;
    }

    /**
     * Set zoom to a specific preset level.
     * Uses smooth motor-like transition.
     */
    public void setPresetZoom(double level) {
        if (!zooming) {
            zooming = true;
        }
        double newTarget = Math.clamp(level, MIN_ZOOM, maxZoom);
        if (Math.abs(newTarget - targetZoom) > 0.5) {
            presetTransition = true;
        }
        targetZoom = newTarget;
    }

    /**
     * Adjust the target zoom by a delta (from scroll wheel).
     */
    public void adjustZoom(double delta) {
        presetTransition = false; // Manual scroll overrides preset transition
        double logCurrent = Math.log(targetZoom);
        double logMax = Math.log(maxZoom);
        double step = logMax / 100.0;
        double newLog = logCurrent + delta * step;
        targetZoom = Math.clamp(Math.exp(newLog), MIN_ZOOM, maxZoom);
    }

    /**
     * Update the smooth interpolation.
     */
    public void update() {
        long now = System.nanoTime();
        float deltaSeconds = (now - lastUpdateNano) / 1_000_000_000f;
        lastUpdateNano = now;
        deltaSeconds = Math.min(deltaSeconds, 0.1f);

        if (zooming) {
            // Use slower speed for preset transitions, normal speed for scroll
            float speed = presetTransition ? PRESET_LERP_SPEED : LERP_SPEED;
            float t = Math.min(1.0f, speed * deltaSeconds);
            currentZoom += (targetZoom - currentZoom) * t;
            // End preset transition when close enough
            if (presetTransition && Math.abs(currentZoom - targetZoom) < 0.1) {
                presetTransition = false;
            }
        } else {
            float t = Math.min(1.0f, LERP_SPEED * deltaSeconds);
            currentZoom += (1.0 - currentZoom) * t;
            if (Math.abs(currentZoom - 1.0) < 0.01) {
                currentZoom = 1.0;
            }
        }
    }

    public double getZoomedFov(double baseFov) {
        if (currentZoom <= 1.001) return baseFov;
        return baseFov / currentZoom;
    }

    public float getZoomProgress() {
        if (currentZoom <= MIN_ZOOM) return 0f;
        return (float) ((Math.log(currentZoom) - Math.log(MIN_ZOOM)) / (Math.log(maxZoom) - Math.log(MIN_ZOOM)));
    }

    public double getMouseSensitivityScale() {
        if (currentZoom <= 1.001) return 1.0;
        return 1.0 / currentZoom;
    }

    // === Config Persistence ===

    private static class Config {
        double maxZoom = DEFAULT_MAX_ZOOM;
        double preset1 = DEFAULT_PRESET_1;
        double preset2 = DEFAULT_PRESET_2;
        double preset3 = DEFAULT_PRESET_3;
    }

    private void loadConfig() {
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("sonyopticalzoom.json");
            if (configPath.toFile().exists()) {
                Gson gson = new Gson();
                FileReader reader = new FileReader(configPath.toFile());
                Config config = gson.fromJson(reader, Config.class);
                reader.close();
                if (config != null && config.maxZoom >= 10.0) {
                    this.maxZoom = Math.clamp(config.maxZoom, 10.0, ABSOLUTE_MAX_ZOOM);
                }
                if (config != null) {
                    if (config.preset1 >= 1.0) this.preset1 = Math.clamp(config.preset1, MIN_ZOOM, maxZoom);
                    if (config.preset2 >= 1.0) this.preset2 = Math.clamp(config.preset2, MIN_ZOOM, maxZoom);
                    if (config.preset3 >= 1.0) this.preset3 = Math.clamp(config.preset3, MIN_ZOOM, maxZoom);
                }
            }
        } catch (Exception e) {
            this.maxZoom = DEFAULT_MAX_ZOOM;
        }
    }

    private void saveConfig() {
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("sonyopticalzoom.json");
            Config config = new Config();
            config.maxZoom = this.maxZoom;
            config.preset1 = this.preset1;
            config.preset2 = this.preset2;
            config.preset3 = this.preset3;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(configPath.toFile());
            gson.toJson(config, writer);
            writer.close();
        } catch (Exception ignored) {}
    }
}
