package forestry.core.platform.mixin;

import forestry.core.platform.fluids.ForestryFluidMotion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(LivingEntity.class)
abstract class LivingFluidMovementMixin extends Entity {
    protected LivingFluidMovementMixin(EntityType<?> type, Level level) { super(type, level); }
    @Shadow protected abstract void travelInWater(Vec3 movement, double gravity, boolean falling, double height);

    @ModifyExpressionValue(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean forestry$respiration(boolean submerged) {
        return submerged || isEyeInFluid(ForestryFluidMotion.FLUIDS);
    }

    @Inject(method = "shouldTravelInFluid", at = @At("RETURN"), cancellable = true)
    private void forestry$fluidTravel(FluidState state, CallbackInfoReturnable<Boolean> result) {
        var entity = (LivingEntity) (Object) this;
        if (!result.getReturnValue() && entity.isAffectedByFluids() && !entity.canStandOnFluid(state)
                && getFluidHeight(ForestryFluidMotion.FLUIDS) > 0) result.setReturnValue(true);
    }

    @Inject(method = "travelInFluid", at = @At("HEAD"), cancellable = true)
    private void forestry$waterMovement(Vec3 movement, CallbackInfo result) {
        if (!isInLava() && getFluidHeight(ForestryFluidMotion.FLUIDS) > 0) {
            var entity = (LivingEntity) (Object) this;
            if (entity.isJumping() && getFluidHeight(ForestryFluidMotion.FLUIDS) > getFluidJumpThreshold())
                setDeltaMovement(getDeltaMovement().add(0, 0.04, 0));
            travelInWater(movement, getEffectiveGravity(), getDeltaMovement().y <= 0, getY());
            result.cancel();
        }
    }
}
