package forestry.core.platform.mixin;

import forestry.core.content.backpacks.PickupHandlerStorage;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Runs after vanilla ownership/pickup-delay checks and preserves its stats, sound and discard path. */
@Mixin(ItemEntity.class)
abstract class ItemEntityPickupMixin implements PickupHandlerStorage.PickupOwner {
    @org.spongepowered.asm.mixin.Shadow private java.util.UUID target;
    @Override public java.util.UUID forestry$getPickupTarget() { return this.target; }
    @Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean forestry$collectInBackpacks(Inventory inventory, ItemStack stack, Player player) {
        return PickupHandlerStorage.onItemPickup(player, (ItemEntity) (Object) this) || inventory.add(stack);
    }
}
