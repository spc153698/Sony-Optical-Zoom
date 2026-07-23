package hajijisonyopticalzoom.client.mixin;

import hajijisonyopticalzoom.client.ZoomManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into MouseHandler to:
 * 1. Intercept scroll wheel events for zoom control
 * 2. Reduce mouse sensitivity when zoomed in
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Accessor("accumulatedDX")
    abstract void setAccumulatedDX(double value);

    @Accessor("accumulatedDY")
    abstract void setAccumulatedDY(double value);

    @Accessor("accumulatedDX")
    abstract double getAccumulatedDX();

    @Accessor("accumulatedDY")
    abstract double getAccumulatedDY();

    /**
     * Intercept scroll wheel: when zooming, use scroll to adjust zoom level
     * instead of scrolling the hotbar.
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        ZoomManager zoomManager = ZoomManager.getInstance();
        if (zoomManager.isZooming() && yOffset != 0) {
            zoomManager.adjustZoom(yOffset);
            ci.cancel();
        }
    }

    /**
     * Before processing accumulated mouse movement, scale down the deltas
     * when zoomed in so the camera doesn't move too fast.
     */
    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void onHandleAccumulatedMovement(CallbackInfo ci) {
        ZoomManager zoomManager = ZoomManager.getInstance();
        double scale = zoomManager.getMouseSensitivityScale();
        if (scale < 0.999) {
            setAccumulatedDX(getAccumulatedDX() * scale);
            setAccumulatedDY(getAccumulatedDY() * scale);
        }
    }
}
