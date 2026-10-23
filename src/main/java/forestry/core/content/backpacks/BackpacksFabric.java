package forestry.core.content.backpacks;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.backpacks.features.*;
import forestry.core.content.backpacks.items.BackpackItem;
import forestry.core.platform.registration.*;

public final class BackpacksFabric {
    public static void register() {
        var registry = ModFeatureRegistry.create(ForestryModuleIds.STORAGE, "forestry");
        CrateItems.CRATE.id(); forestry.apiculture.features.ApicultureCrates.CRATED_PROPOLIS.id();
        BackpackItems.MINER_BACKPACK.id(); BackpackMenuTypes.BACKPACK.getName();
        registry.creativeTab("storage", tab -> tab.icon(() -> BackpackItems.MINER_BACKPACK.stack())
            .displayItems((parameters, output) -> registry.getFeatures().stream().filter(feature -> feature instanceof IItemFeature<?>)
                .map(feature -> (IItemFeature<?>) feature).forEach(feature -> output.accept(feature.stack()))));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (forestry.core.platform.config.ForestryConfig.SERVER.enableBackpackResupply.get()) level.players().forEach(BackpackResupplyHandler::resupply);
        });
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!player.isSpectator() && player.getItemInHand(hand).getItem() instanceof BackpackItem backpack)
                return backpack.useBeforeBlock(player.getItemInHand(hand), new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
            return net.minecraft.world.InteractionResult.PASS;
        });
    }
}
