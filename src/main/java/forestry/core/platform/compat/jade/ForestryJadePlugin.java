package forestry.core.platform.compat.jade;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

public final class ForestryJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ForestryJadeData.INSTANCE, BlockEntity.class);
        registration.registerEnergyStorage(ForestryJadeData.Energy.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(forestry.core.platform.client.compat.ForestryJadeTooltip.INSTANCE, Block.class);
        registration.registerEnergyStorageClient(forestry.core.platform.client.compat.ForestryJadeTooltip.Energy.INSTANCE);
    }
}
