package hajijisonyopticalzoom.client;

import net.fabricmc.api.ClientModInitializer;

public class SonyOpticalZoomClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 注册按键绑定和事件处理
		KeyInputHandler.register();
		// 注册 HUD 变焦条渲染
		ZoomHudOverlay.register();
		// 注册截图闪光渲染
		ScreenshotHandler.register();
		// 注册客户端指令 (/zoom)
		ZoomCommand.register();
	}
}