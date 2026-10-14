package forestry.core.platform.fluids;

import forestry.api.ForestryTags;
import forestry.core.platform.util.RecipeUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.Set;

public class FluidTagFilter extends ReloadableFluidFilter {
	public static final FluidTagFilter WATER = new FluidTagFilter(FluidTags.WATER);
	public static final FluidTagFilter LAVA = new FluidTagFilter(FluidTags.LAVA);
	public static final FluidTagFilter HONEY = new FluidTagFilter(ForestryTags.Fluids.HONEY);

	public FluidTagFilter(TagKey<Fluid> tag) {
		super(() -> {
			HolderSet.Named<Fluid> set = RecipeUtils.getFluidRegistry().get(tag).orElse(null);
			if (set == null || set.size() == 0) {
				return Set.of();
			}
			Set<Identifier> ids = new HashSet<>(set.size());

			for (Holder<Fluid> holder : set) {
				ids.add(holder.unwrapKey().get().identifier());
			}

			return ids;
		});
	}
}
