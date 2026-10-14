package forestry.arboriculture.entities;

import forestry.arboriculture.features.ArboricultureEntities;
import forestry.arboriculture.features.ArboricultureItems;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;

public class ForestryChestBoat extends ForestryBoat implements HasCustomInventoryScreen, ContainerEntity {
	private static final int CONTAINER_SIZE = 27;

	public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event,
			net.minecraft.world.entity.EntityType<ForestryChestBoat> type) {
		event.registerEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY, type,
			(boat, context) -> boat.isAlive() ? net.neoforged.neoforge.transfer.item.VanillaContainerWrapper.of(boat) : null);
		event.registerEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY_AUTOMATION, type,
			(boat, side) -> boat.isAlive() ? net.neoforged.neoforge.transfer.item.VanillaContainerWrapper.of(boat) : null);
	}

	private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
	@Nullable
	private ResourceKey<LootTable> lootTable;
	private long lootTableSeed;

	public ForestryChestBoat(EntityType<? extends Boat> type, Level level) {
		super(type, level);
	}

	public ForestryChestBoat(Level level, double x, double y, double z) {
		this(ArboricultureEntities.CHEST_BOAT.entityType(), level);

		setPos(x, y, z);
		this.xo = x;
		this.yo = y;
		this.zo = z;
	}

	@Override
	protected Item getBoatItem() {
		return ArboricultureItems.CHEST_BOAT.item(getWoodType());
	}

	// <editor-fold desc="Vanilla copy from ChestBoat">
	@Override
	protected float getSinglePassengerXOffset() {
		return 0.15F;
	}

	@Override
	protected int getMaxPassengers() {
		return 1;
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput pCompound) {
		super.addAdditionalSaveData(pCompound);
		addChestVehicleSaveData(pCompound);
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput pCompound) {
		super.readAdditionalSaveData(pCompound);
		readChestVehicleSaveData(pCompound);
	}

	@Override
	public void destroy(net.minecraft.server.level.ServerLevel level, DamageSource pDamageSource) {
		super.destroy(level, pDamageSource);
		chestVehicleDestroyed(pDamageSource, level, this);
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		if (!level().isClientSide() && reason.shouldDestroy()) {
			Containers.dropContents(level(), this, this);
		}

		super.remove(reason);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 location) {
		InteractionResult result = super.interact(player, hand, location);
		if (result != InteractionResult.PASS) return result;
		if (canAddPassenger(player) && !player.isSecondaryUseActive()) return InteractionResult.PASS;
		result = interactWithContainerVehicle(player);
		if (result.consumesAction() && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			gameEvent(GameEvent.CONTAINER_OPEN, player);
			PiglinAi.angerNearbyPiglins(serverLevel, player, true);
		}
		return result;
	}

	@Override
	public void openCustomInventoryScreen(Player player) {
		player.openMenu(this);
		if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			gameEvent(GameEvent.CONTAINER_OPEN, player);
			PiglinAi.angerNearbyPiglins(serverLevel, player, true);
		}
	}

	@Override
	public void clearContent() {
		clearChestVehicleContent();
	}

	@Override
	public int getContainerSize() {
		return CONTAINER_SIZE;
	}

	@Override
	public ItemStack getItem(int slot) {
		return getChestVehicleItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return removeChestVehicleItem(slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return removeChestVehicleItemNoUpdate(slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		setChestVehicleItem(slot, stack);
	}

	@Override
	public SlotAccess getSlot(int slot) {
		return getChestVehicleSlot(slot);
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return isChestVehicleStillValid(player);
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player) {
		if (this.lootTable != null && player.isSpectator()) {
			return null;
		} else {
			unpackChestVehicleLootTable(playerInv.player);
			return ChestMenu.threeRows(windowId, playerInv, this);
		}
	}

	@Nullable
	@Override
	public ResourceKey<LootTable> getContainerLootTable() {
		return this.lootTable;
	}

	@Override
	public void setContainerLootTable(@Nullable ResourceKey<LootTable> lootTable) {
		this.lootTable = lootTable;
	}

	@Override
	public long getContainerLootTableSeed() {
		return this.lootTableSeed;
	}

	@Override
	public void setContainerLootTableSeed(long lootTableSeed) {
		this.lootTableSeed = lootTableSeed;
	}

	@Override
	public NonNullList<ItemStack> getItemStacks() {
		return this.items;
	}

	@Override
	public void clearItemStacks() {
		this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
	}
	// </editor-fold>

	public void stopOpen(ContainerUser user) {
		level().gameEvent(GameEvent.CONTAINER_CLOSE, position(), GameEvent.Context.of(user.getLivingEntity()));
	}
}
