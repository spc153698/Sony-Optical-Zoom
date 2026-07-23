package hajijisonyopticalzoom.client.mixin;

import hajijisonyopticalzoom.client.ZoomManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into GameRenderer to modify the FOV when zooming.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float partialTick, boolean useActiveRenderPosition, CallbackInfoReturnable<Double> cir) {
        ZoomManager zoomManager = ZoomManager.getInstance();
        zoomManager.update(); // smooth interpolation every render frame
        double originalFov = cir.getReturnValue();
        double zoomedFov = zoomManager.getZoomedFov(originalFov);
        cir.setReturnValue(zoomedFov);
    }
}
