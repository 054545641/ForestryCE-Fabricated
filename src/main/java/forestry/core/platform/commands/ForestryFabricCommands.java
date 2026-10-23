package forestry.core.platform.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public final class ForestryFabricCommands {
    public static void initialize() {
        CommandArgumentTypes.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) -> dispatcher.register(Commands.literal("forestry")
            .then(DiagnosticsCommand.register())
            .then(DumpCommand.register())
            .then(MultiblockDebugCommand.register())
            .then(forestry.apiculture.commands.CommandBee.register())
            .then(forestry.arboriculture.commands.TreeCommand.register())));
    }
}
