package forestry.n6;

import forestry.core.features.CoreItems;
import forestry.core.platform.util.GeneticsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import static forestry.n6.N6ReleaseTest.check;

final class N6CuriosChecks {
    static void equip(Player player, boolean equipped, boolean cosmetic) {
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        var head = CuriosApi.getCuriosInventory(player).orElseThrow().getStacksHandler("head").orElseThrow();
        check(head.getStacks().getSlots() > 0, "Curios head slot exists");
        head.getStacks().setStackInSlot(0, equipped && !cosmetic ? CoreItems.SPECTACLES.stack() : ItemStack.EMPTY);
        head.getCosmeticStacks().setStackInSlot(0, equipped && cosmetic ? CoreItems.SPECTACLES.stack() : ItemStack.EMPTY);
        check(GeneticsUtil.hasNaturalistEye(player) == (equipped && !cosmetic), "Only functional Curios spectacles grant vision");
    }

    static void verifyClient(Player player, boolean equipped) {
        check(GeneticsUtil.hasNaturalistEye(player) == equipped, "Curios inventory and vision synchronize to client");
        check(ICurioRenderer.get(CoreItems.SPECTACLES.item()) instanceof forestry.core.platform.compat.curios.client.SpectaclesCurioRenderer,
            "Production spectacles renderer registers/reloads");
    }
}
