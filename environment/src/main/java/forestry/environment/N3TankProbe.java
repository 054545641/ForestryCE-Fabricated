package forestry.environment;

import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.fluids.TankManager;
import forestry.core.platform.item.FluidContainerType;
import forestry.core.platform.item.FluidHandlerItemForestry;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class N3TankProbe {
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new FluidHandlerItemForestry(access, N3FluidProbe.CONTENT.get(), FluidContainerType.CAN), Items.PAPER);
		event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new FluidHandlerItemForestry(access, N3FluidProbe.CONTENT.get(), FluidContainerType.WAX_CAPSULE), Items.FEATHER);
		event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new ItemAccessFluidHandler(access, N3FluidProbe.CONTENT.get(), 4000), Items.STICK);
	}

	public static void tanks(GameTestHelper helper) {
		var water = FluidResource.of(Fluids.WATER);
		var lava = FluidResource.of(Fluids.LAVA);
		int[] changes = {0};
		var first = new StandardTank(1000);
		var second = new StandardTank(1000);
		var manager = new TankManager(new TankManager.Listener() {
			@Override
			public void updateTankLevels(StandardTank tank) { changes[0]++; }
			@Override
			public void sendTankUpdate(ServerPlayer player, int index, FluidStack fluid, int capacity) { }
		}, first, second);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(manager.insert(water, 1500, tx) == 1500, "Manager distributes fluid across tanks");
			helper.assertTrue(changes[0] == 0, "Uncommitted changes do not dirty or sync tanks");
		}
		helper.assertTrue(first.isEmpty() && second.isEmpty() && changes[0] == 0, "Aborted insertion restores all tanks without callbacks");
		try (var outer = Transaction.openRoot()) {
			manager.insert(water, 750, outer);
			try (var inner = Transaction.open(outer)) {
				manager.extract(water, 500, inner);
				inner.commit();
			}
		}
		helper.assertTrue(first.isEmpty() && changes[0] == 0, "Parent rollback undoes nested extraction and insertion");
		try (var tx = Transaction.openRoot()) {
			manager.insert(water, 750, tx);
			tx.commit();
		}
		helper.assertTrue(changes[0] == 1 && first.getFluidAmount() == 750, "Root commit marks the changed tank once");
		try (var tx = Transaction.openRoot()) {
			manager.extract(water, 250, tx);
			manager.insert(water, 250, tx);
			tx.commit();
		}
		helper.assertTrue(changes[0] == 1, "Net zero changes do not trigger dirty or sync callbacks");
		first.getFluid().setAmount(1);
		helper.assertTrue(first.getFluidAmount() == 750, "Read access cannot mutate the journal's fluid");

		var locked = new StandardTank(1000, false, false).setValidator(stack -> stack.is(Fluids.WATER));
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(locked.insert(water, 500, tx) == 0, "External insertion respects tank direction");
			helper.assertTrue(locked.insertInternal(lava, 500, tx) == 500, "Internal products bypass external filters");
			helper.assertTrue(locked.extract(lava, 500, tx) == 0, "External extraction respects tank direction");
			helper.assertTrue(locked.extractInternal(lava, 250, tx) == 250, "Internal consumption shares the same journal");
		}
		helper.assertTrue(locked.isEmpty(), "Internal bypass rolls back");
		var filtered = new StandardTank(1000).setValidator(stack -> stack.is(Fluids.WATER));
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(filtered.insert(lava, 1000, tx) == 0 && filtered.getCapacityAsLong(0, lava) == 0, "Filters reject invalid fluid and report zero capacity");
		}
		var target = new StandardTank(1000);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(ResourceHandlerUtil.move(manager, target, fluid -> true, 500, tx) == 500, "Fluid moves between real production handlers");
		}
		helper.assertTrue(first.getFluidAmount() == 750 && target.isEmpty(), "Transfer rollback restores both ends");

		var registries = helper.getLevel().registryAccess();
		FluidStack named = new FluidStack(Fluids.WATER, 600);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Stored water"));
		first.setFluid(named);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(first.insert(water, 100, tx) == 0, "Components distinguish fluids of the same type");
			helper.assertTrue(first.extract(water, 100, tx) == 0, "Extraction must match fluid components");
		}
		CompoundTag saved = manager.write(new CompoundTag(), registries);
		var persisted = helper.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("n3-tanks-probe.dat");
		try {
			net.minecraft.nbt.NbtIo.writeCompressed(saved, persisted);
			CompoundTag disk = net.minecraft.nbt.NbtIo.readCompressed(persisted, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
			var diskManager = new TankManager(null, new StandardTank(1000), new StandardTank(1000));
			diskManager.read(disk, registries);
			helper.assertTrue(FluidStack.matches(named, diskManager.getFluid(0)), "Components and fluid survive compressed disk persistence");
		} catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }

		var problems = new net.minecraft.util.ProblemReporter.Collector();
		var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(problems, registries);
		manager.write(output);
		helper.assertTrue(saved.equals(output.buildResult()), "ValueOutput preserves the legacy flat fluid entry layout");
		helper.assertTrue(problems.isEmpty(), "Registry-aware fluid encoding has no problems");
		var restored = new TankManager(null, new StandardTank(1000), new StandardTank(1000));
		restored.read(saved, registries);
		helper.assertTrue(FluidStack.matches(named, restored.getFluid(0)), "Fluid amount and components survive NBT round trip");
		restored.read(saved, registries);
		helper.assertTrue(restored.getAmountAsInt(0) == 600, "Repeated loading does not append fluid");
		restored.getTank(1).setFluid(new FluidStack(Fluids.LAVA, 999));
		restored.read(saved, registries);
		helper.assertTrue(restored.getTank(1).isEmpty(), "Omitted tanks clear old state");
		CompoundTag legacy = saved.copy();
		legacy.getListOrEmpty("tanks").getCompound(0).orElseThrow().putByte("tank", (byte) 0);
		restored.read(legacy, registries);
		helper.assertTrue(restored.getAmountAsInt(0) == 600, "Old byte tank indices remain readable");
		var many = new TankManager();
		for (int i = 0; i < 300; i++) many.add(new StandardTank(1000));
		many.getTank(299).setFluid(named);
		CompoundTag manySaved = many.write(new CompoundTag(), registries);
		many.getTank(299).setFluid(FluidStack.EMPTY);
		many.read(manySaved, registries);
		helper.assertTrue(many.getAmountAsInt(299) == 600, "Integer indices do not wrap above 127 or 255");
		CompoundTag broken = saved.copy();
		CompoundTag badEntry = new CompoundTag();
		badEntry.putInt("tank", 1);
		badEntry.putString("id", "minecraft:no_such_fluid");
		badEntry.putInt("amount", 500);
		broken.getListOrEmpty("tanks").add(badEntry);
		restored.getTank(0).setFluid(new FluidStack(Fluids.LAVA, 400));
		expectFailure(helper, () -> restored.read(broken, registries), "Invalid later entry must fail before replacing any tanks");
		helper.assertTrue(restored.getFluid(0).is(Fluids.LAVA) && restored.getAmountAsInt(0) == 400, "Decode failure leaves every live tank unchanged");
		CompoundTag duplicate = saved.copy();
		duplicate.getListOrEmpty("tanks").add(saved.getListOrEmpty("tanks").get(0).copy());
		expectFailure(helper, () -> restored.read(duplicate, registries), "Duplicate indices are rejected");
		CompoundTag overfull = saved.copy();
		overfull.getListOrEmpty("tanks").getCompound(0).orElseThrow().putInt("amount", 1001);
		expectFailure(helper, () -> restored.read(overfull, registries), "Overfull entries are rejected without losing fluid");
		CompoundTag wrongType = new CompoundTag();
		wrongType.putString("tanks", "broken");
		expectFailure(helper, () -> restored.read(wrongType, registries), "Wrong list type is rejected");

		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try {
			manager.writeData(buffer);
			restored.readData(buffer);
			helper.assertTrue(FluidStack.matches(named, restored.getFluid(0)), "Network round trip preserves fluid components");
			buffer.clear();
			FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, new FluidStack(Fluids.LAVA, 900));
			expectFailure(helper, () -> restored.readData(buffer), "Truncated multi-tank packet is rejected");
			helper.assertTrue(FluidStack.matches(named, restored.getFluid(0)), "Truncated packets cannot partially overwrite tanks");
		} finally {
			buffer.release();
		}
		restored.processTankUpdate(restored.size(), new FluidStack(Fluids.WATER, 1));
		restored.processTankUpdate(-1, null);
		restored.read(new CompoundTag(), registries);
		helper.assertTrue(ResourceHandlerUtil.isEmpty(restored), "Missing tanks field clears all contents");
		var variable = StandardTank.variableCapacity(1000);
		variable.setCapacity(50000);
		variable.setFluid(new FluidStack(Fluids.LAVA, 40000));
		var variableManager = new TankManager(null, variable);
		var restoredVariable = new TankManager(null, StandardTank.variableCapacity(1000));
		CompoundTag variableSaved = variableManager.write(new CompoundTag(), registries);
		restoredVariable.read(variableSaved, registries);
		helper.assertTrue(restoredVariable.getTank(0).getCapacity() == 50000 && restoredVariable.getAmountAsInt(0) == 40000, "Engine timing tanks preserve variable capacity and remaining fuel");
		try (var tx = Transaction.openRoot()) {
			variable.setCapacity(60000, tx);
			variable.extractInternal(lava, 30000, tx);
		}
		helper.assertTrue(variable.getCapacity() == 50000 && variable.getFluidAmount() == 40000, "Capacity and contents roll back together");
		var variableOutput = net.minecraft.world.level.storage.TagValueOutput.createWithContext(problems, registries);
		variableManager.write(variableOutput);
		helper.assertTrue(variableSaved.equals(variableOutput.buildResult()), "Variable capacity uses the same NBT and ValueOutput layout");
		var variableBuffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try {
			variableManager.writeData(variableBuffer);
			restoredVariable.readData(variableBuffer);
			helper.assertTrue(restoredVariable.getTank(0).getCapacity() == 50000, "Variable capacity survives network synchronization");
		} finally { variableBuffer.release(); }
		variableSaved.getListOrEmpty("tanks").getCompound(0).orElseThrow().remove("capacity");
		restoredVariable.read(variableSaved, registries);
		helper.assertTrue(restoredVariable.getTank(0).getCapacity() >= 40000 && restoredVariable.getAmountAsInt(0) == 40000, "Legacy engine timing tanks do not lose amounts above the initial bucket capacity");
		variable.setFluid(FluidStack.EMPTY);
		restoredVariable.read(variableManager.write(new CompoundTag(), registries), registries);
		helper.assertTrue(restoredVariable.getTank(0).isEmpty() && restoredVariable.getTank(0).getCapacity() == 50000, "An empty timing tank retains its capacity on reload");
		restoredVariable.processTankUpdate(0, new FluidStack(Fluids.LAVA, 30000), 45000);
		helper.assertTrue(restoredVariable.getTank(0).getCapacity() == 45000 && restoredVariable.getAmountAsInt(0) == 30000, "Incremental updates replace contents and capacity together");

		helper.succeed();
	}

	public static void machines(GameTestHelper helper) {
		var tank = new StandardTank(10000);
		tank.setFluid(new FluidStack(Fluids.WATER, 5000));
		int[] changes = {0};
		tank.setTankUpdateHandler(changed -> changes[0]++);
		SimpleContainer inventory = new SimpleContainer(2);
		inventory.setItem(0, new ItemStack(Items.BUCKET, 2));
		inventory.setItem(1, new ItemStack(Items.STONE, 64));
		helper.assertTrue(FluidHelper.fillContainers(tank, inventory, 0, 1, Fluids.WATER, true) == FluidHelper.FillStatus.NO_SPACE, "Full output rejects filling");
		helper.assertTrue(tank.getFluidAmount() == 5000 && changes[0] == 0 && inventory.getItem(0).getCount() == 2, "Full output restores fluid and input items without callbacks");
		inventory.setItem(1, ItemStack.EMPTY);
		helper.assertTrue(FluidHelper.fillContainers(tank, inventory, 0, 1, Fluids.WATER, false) == FluidHelper.FillStatus.SUCCESS, "Filling simulation succeeds");
		helper.assertTrue(tank.getFluidAmount() == 5000 && inventory.getItem(1).isEmpty() && changes[0] == 0, "Simulation restores every participant");
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(FluidHelper.fillContainers(tank, inventory, 0, 1, Fluids.WATER, tx) == FluidHelper.FillStatus.SUCCESS, "Nested fill succeeds");
		}
		helper.assertTrue(tank.getFluidAmount() == 5000 && inventory.getItem(0).getCount() == 2 && inventory.getItem(1).isEmpty(), "Parent rollback undoes a completed container operation");
		helper.assertTrue(FluidHelper.fillContainers(tank, inventory, 0, 1, Fluids.WATER, true) == FluidHelper.FillStatus.SUCCESS, "Bucket filling commits");
		helper.assertTrue(tank.getFluidAmount() == 4000 && inventory.getItem(0).getCount() == 1 && inventory.getItem(1).is(Items.WATER_BUCKET) && changes[0] == 1, "A single bucket moves to output and exactly 1000 mB leaves the tank");
		inventory.setItem(0, new ItemStack(Items.WATER_BUCKET));
		inventory.setItem(1, new ItemStack(Items.BUCKET, 15));
		helper.assertTrue(FluidHelper.drainContainers(tank, inventory, 0, 1, true) == FluidHelper.FillStatus.SUCCESS, "Empty buckets can fill output exactly to stack limit");
		helper.assertTrue(inventory.getItem(0).isEmpty() && inventory.getItem(1).getCount() == 16 && tank.getFluidAmount() == 5000, "Bucket draining conserves fluid and containers");
		inventory.setItem(0, filled(Items.PAPER, 2, Fluids.WATER, 1000));
		inventory.setItem(1, new ItemStack(Items.STONE, 64));
		var small = new StandardTank(999);
		helper.assertTrue(FluidHelper.drainContainers(small, inventory, 0, 1, true) == FluidHelper.FillStatus.NO_SPACE_FLUID, "Capsules cannot partially empty into an undersized tank");
		helper.assertTrue(small.isEmpty() && inventory.getItem(0).getCount() == 2, "Failed capsule draining leaves both ends unchanged");
		helper.assertTrue(FluidHelper.drainContainers(tank, inventory, 0), "Consumed capsules do not require output space");
		helper.assertTrue(inventory.getItem(0).getCount() == 1 && tank.getFluidAmount() == 6000, "Only one capsule is consumed from a stack");
		inventory.setItem(0, new ItemStack(Items.PAPER, 2));
		inventory.setItem(1, filled(Items.PAPER, 63, Fluids.WATER, 1000));
		helper.assertTrue(FluidHelper.fillContainers(tank, inventory, 0, 1, Fluids.WATER, true) == FluidHelper.FillStatus.SUCCESS, "Filled capsules merge into matching output components");
		helper.assertTrue(inventory.getItem(1).getCount() == 64 && inventory.getItem(0).getCount() == 1 && tank.getFluidAmount() == 5000, "Capsule filling respects stack size and fluid balance");
		helper.assertTrue(FluidHelper.isFillableContainer(new ItemStack(Items.PAPER), new FluidStack(Fluids.WATER, 1)), "Fillability probes support all-or-nothing capsules");
		helper.assertTrue(!FluidHelper.isFillableContainer(new ItemStack(Items.FEATHER), new FluidStack(Fluids.LAVA, 1000)), "Wax temperature filtering survives machine integration");
		helper.assertTrue(FluidHelper.getEmptyContainer(new ItemStack(Items.WATER_BUCKET)).is(Items.BUCKET), "Empty-container lookup supports replacing the underlying item");
		helper.assertTrue(FluidHelper.getEmptyContainer(filled(Items.PAPER, 1, Fluids.WATER, 1000)).isEmpty(), "Consumed capsules have no returned empty item");

		var partialTank = new StandardTank(10000);
		partialTank.setFluid(new FluidStack(Fluids.WATER, 300));
		inventory.setItem(0, new ItemStack(Items.STICK));
		inventory.setItem(1, ItemStack.EMPTY);
		helper.assertTrue(FluidHelper.fillContainers(partialTank, inventory, 0, 1, Fluids.WATER, true) == FluidHelper.FillStatus.SUCCESS, "Partial containers accept available fluid");
		helper.assertTrue(inventory.getItem(0).get(N3FluidProbe.CONTENT).getAmount() == 300 && inventory.getItem(1).isEmpty() && partialTank.isEmpty(), "Partly filled containers remain in input");
		partialTank.setFluid(new FluidStack(Fluids.WATER, 3700));
		helper.assertTrue(FluidHelper.fillContainers(partialTank, inventory, 0, 1, Fluids.WATER, true) == FluidHelper.FillStatus.SUCCESS, "Partial filling can complete later");
		helper.assertTrue(inventory.getItem(0).isEmpty() && inventory.getItem(1).get(N3FluidProbe.CONTENT).getAmount() == 4000 && partialTank.isEmpty(), "Only full containers move to output");
		inventory.setItem(0, new ItemStack(Items.STICK, 2));
		inventory.setItem(1, ItemStack.EMPTY);
		partialTank.setFluid(new FluidStack(Fluids.WATER, 300));
		helper.assertTrue(FluidHelper.fillContainers(partialTank, inventory, 0, 1, Fluids.WATER, true) == FluidHelper.FillStatus.NO_SPACE, "A partial container cannot replace a stack of unfilled items");
		helper.assertTrue(inventory.getItem(0).getCount() == 2 && partialTank.getFluidAmount() == 300, "Failed partial split rolls back fluid and items");
		var backing = new forestry.core.platform.inventory.InventoryPlain(2, "Items", 64);
		var owner = new InventoryOwner(backing);
		var alias = new InventoryOwner(backing);
		backing.setItem(0, new ItemStack(Items.PAPER, 2));
		partialTank.setFluid(new FluidStack(Fluids.WATER, 2000));
		try (var outer = Transaction.openRoot()) {
			helper.assertTrue(FluidHelper.fillContainers(partialTank, owner, 0, 1, Fluids.WATER, outer) == FluidHelper.FillStatus.SUCCESS, "Production inventory participates in nested fluid transfer");
			helper.assertTrue(FluidHelper.fillContainers(partialTank, alias, 0, 1, Fluids.WATER, outer) == FluidHelper.FillStatus.SUCCESS, "Inventory aliases share a backing journal");
		}
		helper.assertTrue(backing.getItem(0).getCount() == 2 && backing.getItem(1).isEmpty() && partialTank.getFluidAmount() == 2000 && owner.changes == 0 && alias.changes == 0, "Parent rollback restores shared inventory aliases without dirty callbacks");
		FluidHelper.fillContainers(partialTank, owner, 0, 1, Fluids.WATER, true);
		helper.assertTrue(owner.changes == 1, "Inventory owner is dirtied only after root commit");
		CompoundTag itemData = backing.write(new CompoundTag(), helper.getLevel().registryAccess());
		var loadedInventory = new forestry.core.platform.inventory.InventoryPlain(2, "Items", 64);
		loadedInventory.read(itemData, helper.getLevel().registryAccess());
		helper.assertTrue(ItemStack.matches(backing.getItem(1), loadedInventory.getItem(1)), "Machine inventory preserves filled-container components on reload");
		FluidStack distinct = new FluidStack(Fluids.WATER, 1000);
		distinct.set(DataComponents.CUSTOM_NAME, Component.literal("Distinct water"));
		partialTank.setFluid(distinct);
		helper.assertTrue(FluidHelper.fillContainers(partialTank, owner, 0, 1, Fluids.WATER, true) == FluidHelper.FillStatus.NO_SPACE, "Different fluid components cannot merge into the same output stack");
		helper.assertTrue(FluidStack.matches(distinct, partialTank.getFluid()) && backing.getItem(0).getCount() == 1 && backing.getItem(1).getCount() == 1, "Rejected component merge preserves the source and both item slots");

		var bottlerRecipe = forestry.core.content.machines.recipes.BottlerRecipe.createFillingRecipe(distinct, new ItemStack(Items.PAPER));
		helper.assertTrue(bottlerRecipe != null && FluidStack.matches(distinct, bottlerRecipe.fluid), "Bottler recipes keep fluid components");
		helper.assertTrue(FluidStack.matches(distinct, bottlerRecipe.outputStack.get(N3FluidProbe.CONTENT).copy()), "Bottler previews keep the filled container components");
		var drainedRecipe = forestry.core.content.machines.recipes.BottlerRecipe.createEmptyingRecipe(bottlerRecipe.outputStack);
		helper.assertTrue(drainedRecipe != null && drainedRecipe.inputStack.isEmpty() && FluidStack.matches(distinct, drainedRecipe.fluid), "Emptying previews retain consumed-container semantics and fluid components");
		var distinctTank = new StandardTank(5000);
		distinctTank.setFluid(distinct.copyWithAmount(3700));
		var plainTank = new StandardTank(5000);
		plainTank.setFluid(new FluidStack(Fluids.WATER, 4000));
		var mixedManager = new TankManager(null, plainTank, distinctTank);
		inventory.setItem(0, filled(Items.STICK, 1, Fluids.WATER, 300));
		inventory.getItem(0).set(N3FluidProbe.CONTENT, SimpleFluidContent.copyOf(distinct.copyWithAmount(300)));
		inventory.setItem(1, ItemStack.EMPTY);
		helper.assertTrue(FluidHelper.fillContainers(mixedManager, inventory, 0, 1, Fluids.WATER, true) == FluidHelper.FillStatus.SUCCESS, "Filling finds a compatible component variant beyond the first tank");
		helper.assertTrue(plainTank.getFluidAmount() == 4000 && distinctTank.isEmpty() && inventory.getItem(1).get(N3FluidProbe.CONTENT).getAmount() == 4000, "Only the matching fluid variant is transferred");

		helper.succeed();
	}

	private static final class InventoryOwner extends forestry.core.platform.inventory.InventoryAdapter {
		private int changes;
		private InventoryOwner(forestry.core.platform.inventory.InventoryPlain backing) { super(backing); }
		@Override public void setChanged() { super.setChanged(); this.changes++; }
	}

	private static ItemStack filled(net.minecraft.world.item.Item item, int count, net.minecraft.world.level.material.Fluid fluid, int amount) {
		ItemStack stack = new ItemStack(item, count);
		stack.set(N3FluidProbe.CONTENT, SimpleFluidContent.copyOf(new FluidStack(fluid, amount)));
		return stack;
	}

	private static void expectFailure(GameTestHelper helper, Runnable action, String message) {
		boolean failed = false;
		try { action.run(); } catch (RuntimeException expected) { failed = true; }
		helper.assertTrue(failed, message);
	}
}
