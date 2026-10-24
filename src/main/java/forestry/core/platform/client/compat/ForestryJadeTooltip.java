package forestry.core.platform.client.compat;

import forestry.core.platform.compat.jade.ForestryJadeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;
import java.util.Objects;

public enum ForestryJadeTooltip implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public Identifier getUid() {
        return ForestryJadeData.ID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        accessor.getServerData().getCompound("forestry:status").ifPresent(status -> {
            status.getList("errors").ifPresent(errors -> errors.stream().limit(32).forEach(error ->
                error.asString().ifPresent(key -> tooltip.add(Component.translatable(key).withStyle(ChatFormatting.RED)))));
            status.getInt("bee_progress").ifPresent(progress -> tooltip.add(Component.translatable("jade.forestry.bee_progress", progress)));
            status.getInt("ripeness").ifPresent(progress -> tooltip.add(Component.translatable("jade.forestry.ripeness", progress)));
        });
    }

    public enum Energy implements IClientExtensionProvider<EnergyView.Data, EnergyView> {
        INSTANCE;

        @Override
        public Identifier getUid() {
            return ForestryJadeData.ENERGY_ID;
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<EnergyView.Data>> groups) {
            // Keep Jade's native formatting, energy bar and display settings, using the GUI's FE unit.
            return groups.stream().map(group -> new ClientViewGroup<>(group.views.stream()
                .map(data -> EnergyView.read(data, "FE")).filter(Objects::nonNull).toList())).toList();
        }
    }
}
