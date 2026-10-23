package forestry.core.data;

import forestry.api.ForestryTags;
import forestry.core.platform.fluids.ForestryFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.material.Fluid;
import thedarkcolour.modkit.data.MKTagsProvider;

public class ForestryFluidTagsProvider {
	public static void addTags(MKTagsProvider<Fluid> tags, HolderLookup.Provider lookup) {
		tags.tag(ForestryTags.Fluids.HONEY).add(ForestryFluids.HONEY.getFluid(), ForestryFluids.HONEY.getFlowing());
		tags.tag(ForestryTags.Fluids.EXPERIENCE).add(ForestryFluids.EXPERIENCE.getFluid(), ForestryFluids.EXPERIENCE.getFlowing());
		tags.tag(ForestryTags.Fluids.WAX).add(ForestryFluids.WAX.getFluid(), ForestryFluids.WAX.getFlowing());
	}
}
