package forestry.core.platform.mixin;

import forestry.core.platform.fluids.ForestryFluidMotion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.HashSet;
import java.util.Set;

@Mixin(EntityFluidInteraction.class)
abstract class EntityFluidTrackingMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static Set<TagKey<Fluid>> forestry$trackFluids(Set<TagKey<Fluid>> fluids) {
        var tracked = new HashSet<>(fluids);
        tracked.add(ForestryFluidMotion.FLUIDS);
        return Set.copyOf(tracked);
    }
}
