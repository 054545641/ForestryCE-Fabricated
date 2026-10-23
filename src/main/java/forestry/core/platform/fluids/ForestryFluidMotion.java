package forestry.core.platform.fluids;

import forestry.api.ForestryConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class ForestryFluidMotion {
    public static final TagKey<Fluid> FLUIDS = TagKey.create(Registries.FLUID, ForestryConstants.forestry("entity_movement"));
}
