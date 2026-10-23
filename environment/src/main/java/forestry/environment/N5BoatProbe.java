package forestry.environment;

import forestry.arboriculture.entities.*;
import forestry.arboriculture.wood.ForestryWoodType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Real production boat classes; only wood/item/entity registry lookup is provided by the isolated fixture. */
public final class N5BoatProbe {
    public static void boats(GameTestHelper helper) {
        var level = helper.getLevel();
        var boat = new ForestryBoat(level, 0, 70, 0);
        boat.setWoodType(ForestryWoodType.LARCH);
        helper.assertTrue(boat.getPickResult().is(Items.OAK_BOAT), "Pick result uses current wood");
        boat.setWoodType(ForestryWoodType.TEAK);
        helper.assertTrue(boat.getPickResult().is(Items.BIRCH_BOAT), "Drop supplier follows later wood changes");
        var replica = new ForestryBoat(level, 0, 70, 0);
        replica.getEntityData().assignValues(boat.getEntityData().packDirty());
        helper.assertTrue(replica.getWoodType() == ForestryWoodType.TEAK && replica.getPickResult().is(Items.BIRCH_BOAT), "Synced wood updates the bound item supplier");
        var chest = new ForestryChestBoat(level, 0, 70, 0);
        chest.setWoodType(ForestryWoodType.LARCH);
        helper.assertTrue(chest.getPickResult().is(Items.OAK_CHEST_BOAT), "Chest drop supplier dispatches to the chest variant");
        ItemStack cargo = new ItemStack(Items.DIAMOND, 7);
        cargo.set(DataComponents.CUSTOM_NAME, Component.literal("N5 boat cargo"));
        chest.setItem(26, cargo);
        var capability = chest.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY);
        var automation = chest.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY_AUTOMATION, net.minecraft.core.Direction.DOWN);
        helper.assertTrue(capability != null && automation != null, "Both direct and automation capabilities are available");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertTrue(automation.extract(26, net.neoforged.neoforge.transfer.item.ItemResource.of(cargo), 3, transaction) == 3, "Automation can extract cargo");
        }
        helper.assertTrue(ItemStack.matches(cargo, chest.getItem(26)), "Aborted automation extraction restores cargo");
        var problems = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(problems, level.registryAccess());
        chest.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        helper.assertTrue(saved.getStringOr("type", "").equals("LARCH"), "Legacy uppercase wood field is preserved");
        var restored = new ForestryChestBoat(level, 0, 70, 0);
        restored.load(TagValueInput.create(problems, level.registryAccess(), saved));
        helper.assertTrue(problems.isEmpty() && restored.getWoodType() == ForestryWoodType.LARCH
            && ItemStack.matches(cargo, restored.getItem(26)), "ValueIO restores wood and all cargo components in the final slot");
        saved.putString("type", "invalid");
        restored.load(TagValueInput.create(problems, level.registryAccess(), saved));
        helper.assertTrue(restored.getWoodType() == ForestryWoodType.CAMELTHORN, "Unknown wood safely falls back");
        saved.remove("type");
        restored.setWoodType(ForestryWoodType.TEAK);
        restored.load(TagValueInput.create(problems, level.registryAccess(), saved));
        helper.assertTrue(restored.getWoodType() == ForestryWoodType.CAMELTHORN, "Missing wood resets a reused entity");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(boat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
            && player.getVehicle() == boat, "Native hit-location interaction mounts ordinary boat");
        player.stopRiding();
        helper.assertTrue(chest.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
            && player.getVehicle() == chest, "Chest boat prefers riding when there is space");
        var second = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!second.startRiding(chest), "Chest boat retains one passenger limit");
        second.setShiftKeyDown(true);
        helper.assertTrue(chest.interact(second, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
            && !second.isPassenger(), "Secondary interaction opens chest instead of mounting");
        helper.assertTrue(chest.createMenu(1, second.getInventory(), second).slots.size() >= 27, "Container menu exposes all 27 slots");
        player.stopRiding();
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        chest.setPos(pos.getX(), pos.getY(), pos.getZ());
        level.addFreshEntity(chest);
        chest.destroy(level, level.damageSources().generic());
        var drops = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(3));
        int diamonds = drops.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
        int boats = drops.stream().filter(e -> e.getItem().is(Items.OAK_CHEST_BOAT)).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(chest.isRemoved() && diamonds == 7 && boats == 1, "Destroy drops cargo and matching chest boat exactly once");
        helper.assertTrue(chest.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY) == null
            && chest.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY_AUTOMATION, null) == null, "Removed chest boats expose neither capability");
        drops.forEach(Entity::discard);
        boat.discard();
        helper.succeed();
    }
}
