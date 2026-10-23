package forestry.lepidopterology.mixin;

import forestry.lepidopterology.butterflies.ItemButterflyGE;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ButterflyItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void forestry$releasedButterfly(CallbackInfo callback) {
        var entity = (ItemEntity) (Object) this;
        if (entity.getItem().getItem() instanceof ItemButterflyGE butterfly && butterfly.onEntityItemUpdate(entity.getItem(), entity)) callback.cancel();
    }
}
