package forestry.core.platform.mixin;

import forestry.core.platform.fluids.ForestryFluidMotion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class EntityFluidCurrentMixin {
    @Shadow @Final private EntityFluidInteraction fluidInteraction;

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void forestry$swimmingPose(CallbackInfo callback) {
        if (this.fluidInteraction.isInFluid(ForestryFluidMotion.FLUIDS)) {
            var entity = (Entity) (Object) this;
            entity.setSwimming(entity.isSprinting() && !entity.isPassenger()
                && (entity.isSwimming() || entity.isEyeInFluid(ForestryFluidMotion.FLUIDS)
                    && entity.level().getFluidState(entity.blockPosition()).is(ForestryFluidMotion.FLUIDS)));
            callback.cancel();
        }
    }

    @Inject(method = "updateFluidInteraction", at = @At("TAIL"))
    private void forestry$applyCurrent(CallbackInfoReturnable<Boolean> result) {
        var entity = (Entity) (Object) this;
        if (this.fluidInteraction.isInFluid(ForestryFluidMotion.FLUIDS)) {
            entity.resetFallDistance();
            if (entity.isPushedByFluid()) this.fluidInteraction.applyCurrentTo(ForestryFluidMotion.FLUIDS, entity, 0.014);
        }
    }
}
