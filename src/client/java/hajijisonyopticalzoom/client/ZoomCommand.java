package hajijisonyopticalzoom.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side commands for configuring zoom settings:
 * - /zoom preset <1|2|3> <value>  — Set preset zoom level
 * - /zoom maxzoom <value>         — Set max zoom limit
 * - /zoom reset                   — Reset all settings to defaults
 * - /zoom info                    — Show current settings
 */
public class ZoomCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ZoomCommand::onCommandRegister);
    }

    private static void onCommandRegister(CommandDispatcher<FabricClientCommandSource> dispatcher, 
                                          CommandBuildContext commandBuildContext) {
        dispatcher.register(
            ClientCommandManager.literal("zoom")
                // /zoom preset <1|2|3> <value>
                .then(ClientCommandManager.literal("preset")
                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(1, 3))
                        .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(1.0, 200.0))
                            .executes(ZoomCommand::executeSetPreset)
                        )
                    )
                )
                // /zoom maxzoom <value>
                .then(ClientCommandManager.literal("maxzoom")
                    .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(10.0, 200.0))
                        .executes(ZoomCommand::executeSetMaxZoom)
                    )
                )
                // /zoom reset
                .then(ClientCommandManager.literal("reset")
                    .executes(ZoomCommand::executeReset)
                )
                // /zoom info
                .then(ClientCommandManager.literal("info")
                    .executes(ZoomCommand::executeInfo)
                )
        );
    }

    private static int executeSetPreset(CommandContext<FabricClientCommandSource> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        double value = DoubleArgumentType.getDouble(context, "value");
        
        ZoomManager zoomManager = ZoomManager.getInstance();
        zoomManager.setPreset(index, value);
        double actualValue = zoomManager.getPreset(index);
        
        sendFeedback("§7[§fSony Zoom§7] §fPreset " + index + " set to §e" 
                + String.format("%.1fx", actualValue));
        return 1;
    }

    private static int executeSetMaxZoom(CommandContext<FabricClientCommandSource> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        
        ZoomManager zoomManager = ZoomManager.getInstance();
        zoomManager.setMaxZoom(value);
        double actualValue = zoomManager.getMaxZoom();
        
        sendFeedback("§7[§fSony Zoom§7] §fMax zoom set to §e" 
                + String.format("%.0fx", actualValue));
        return 1;
    }

    private static int executeReset(CommandContext<FabricClientCommandSource> context) {
        ZoomManager zoomManager = ZoomManager.getInstance();
        zoomManager.resetPresets();
        zoomManager.setMaxZoom(ZoomManager.DEFAULT_MAX_ZOOM);
        
        sendFeedback("§7[§fSony Zoom§7] §fAll settings reset to defaults");
        return 1;
    }

    private static int executeInfo(CommandContext<FabricClientCommandSource> context) {
        ZoomManager zm = ZoomManager.getInstance();
        
        sendFeedback("§7[§fSony Zoom§7] §fCurrent settings:");
        sendFeedback("  §7- §fPreset 1: §e" + String.format("%.1fx", zm.getPreset1()));
        sendFeedback("  §7- §fPreset 2: §e" + String.format("%.1fx", zm.getPreset2()));
        sendFeedback("  §7- §fPreset 3: §e" + String.format("%.1fx", zm.getPreset3()));
        sendFeedback("  §7- §fMax zoom: §e" + String.format("%.0fx", zm.getMaxZoom()));
        return 1;
    }

    private static void sendFeedback(String message) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal(message));
    }
}
