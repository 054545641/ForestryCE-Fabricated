package forestry.environment;

import forestry.core.platform.item.FluidContainerType;
import forestry.core.platform.util.InventoryData;
import forestry.core.platform.item.FluidHandlerItemForestry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class N3FluidProbe {
	static final net.neoforged.neoforge.registries.DeferredRegister<DataComponentType<?>> COMPONENTS =
		net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, EnvironmentProbe.ID);
	static final java.util.function.Supplier<DataComponentType<SimpleFluidContent>> CONTENT = COMPONENTS.register("fluid_content",
		() -> DataComponentType.<SimpleFluidContent>builder().persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());

	public static void containers(GameTestHelper helper) {
		var water = FluidResource.of(Fluids.WATER);
		var lava = FluidResource.of(Fluids.LAVA);
		ItemStack capsule = new ItemStack(Items.PAPER);
		var handler = new FluidHandlerItemForestry(ItemAccess.forStack(capsule), CONTENT.get(), FluidContainerType.WAX_CAPSULE);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(handler.insert(water, 999, tx) == 0, "Capsules reject partial filling");
			helper.assertTrue(handler.insert(lava, 1000, tx) == 0, "Wax rejects hot fluid");
			helper.assertTrue(handler.insert(water, 1000, tx) == 1000, "A full bucket fits");
		}
		helper.assertTrue(capsule.get(CONTENT) == null && capsule.getCount() == 1, "Aborted fill restores the item");
		try (var outer = Transaction.openRoot()) {
			try (var inner = Transaction.open(outer)) {
				helper.assertTrue(handler.insert(water, 1000, inner) == 1000, "Nested fill succeeds");
				inner.commit();
			}
		}
		helper.assertTrue(capsule.get(CONTENT) == null, "Parent rollback undoes a committed child");
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(handler.insert(water, 1000, tx) == 1000, "Committed filling succeeds");
			tx.commit();
		}
		helper.assertTrue(capsule.get(CONTENT).getAmount() == 1000, "Commit persists fluid components");
		var registries = helper.getLevel().registryAccess();
		ItemStack restored = InventoryData.readStack(InventoryData.writeStack(capsule, registries), registries);
		helper.assertTrue(ItemStack.matches(capsule, restored), "Filled container components survive NBT round trip");
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(handler.extract(water, 999, tx) == 0, "Capsules reject partial draining");
			helper.assertTrue(handler.extract(water, 1000, tx) == 1000, "Full draining consumes the capsule");
		}
		helper.assertTrue(capsule.getCount() == 1 && handler.getAmountAsInt(0) == 1000, "Aborted drain restores capsule and fluid");
		var tooSmall = new FluidStacksResourceHandler(1, 999);
		helper.assertTrue(ResourceHandlerUtil.move(handler, tooSmall, fluid -> true, 1000, null) == 0, "Undersized destination rejects the whole capsule");
		helper.assertTrue(capsule.getCount() == 1 && handler.getAmountAsInt(0) == 1000 && tooSmall.getAmountAsInt(0) == 0, "Failed transfer preserves both ends");
		var tank = new FluidStacksResourceHandler(1, 1000);
		helper.assertTrue(ResourceHandlerUtil.move(handler, tank, fluid -> true, 1000, null) == 1000, "Successful transfer moves a full bucket");
		helper.assertTrue(capsule.isEmpty() && tank.getAmountAsInt(0) == 1000, "Successful transfer consumes exactly one capsule");

		SimpleContainer inventory = new SimpleContainer(2);
		inventory.setItem(0, new ItemStack(Items.PAPER, 2));
		inventory.setItem(1, new ItemStack(Items.STONE, 64));
		var access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(inventory), 0).oneByOne();
		var stacked = new FluidHandlerItemForestry(access, CONTENT.get(), FluidContainerType.CAN);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(stacked.insert(water, 1000, tx) == 0, "Full inventory cannot split a filled container");
			tx.commit();
		}
		helper.assertTrue(inventory.getItem(0).getCount() == 2 && inventory.getItem(0).get(CONTENT) == null, "Failed split preserves the empty stack");
		inventory.setItem(1, ItemStack.EMPTY);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(stacked.insert(lava, 1000, tx) == 1000, "Cans accept hot fluid and split into a free slot");
			tx.commit();
		}
		helper.assertTrue(inventory.getItem(0).getCount() == 1 && inventory.getItem(1).get(CONTENT).getAmount() == 1000, "Exactly one stacked container is filled");
		helper.succeed();
	}
}
