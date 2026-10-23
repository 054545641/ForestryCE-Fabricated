package forestry.core.platform.commands;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import forestry.api.ForestryConstants;

@FeatureProvider
public class CommandArgumentTypes {
	public static void initialize() {
		ArgumentTypeRegistry.registerArgumentType(ForestryConstants.forestry("species"), SpeciesArgument.class, new ISpeciesArgumentType.Serializer<>(SpeciesArgument::new));
		ArgumentTypeRegistry.registerArgumentType(ForestryConstants.forestry("life_stage"), LifeStageArgument.class, new LifeStageArgument.Serializer<>(LifeStageArgument::new));
		ArgumentTypeRegistry.registerArgumentType(ForestryConstants.forestry("chromosome"), ChromosomeArgument.class, new ChromosomeArgument.Serializer<>(ChromosomeArgument::new));
		ArgumentTypeRegistry.registerArgumentType(ForestryConstants.forestry("allele"), AlleleArgument.class, new AlleleArgument.Serializer<>(AlleleArgument::new));
	}
}
