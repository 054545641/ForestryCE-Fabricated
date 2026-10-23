package forestry.core.platform.mixin;

import forestry.core.platform.item.HasRemnants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Consumer;

/** Replaces the two item extension hooks that have no vanilla/Fabric callback. */
@Mixin(ItemStack.class)
abstract class ItemStackMixin implements net.minecraft.world.item.ItemInstance {
    @Shadow public abstract Item getItem();

    @Override public int getMaxStackSize() {
        if (getItem() instanceof forestry.apiculture.apiary.HiveFrameItem) return 64;
        return net.minecraft.world.item.ItemInstance.super.getMaxStackSize();
    }

    @Redirect(method = "applyDamage", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void forestry$brokenToolRemnant(Consumer<Item> callback, Object broken, int damage, ServerPlayer player, Consumer<Item> ignored) {
        Item item = (Item) broken;
        callback.accept(item);
        if (player != null && item instanceof HasRemnants tool) player.getInventory().placeItemBackInInventory(tool.getRemnant());
    }
}
