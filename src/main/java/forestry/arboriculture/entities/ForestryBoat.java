package forestry.arboriculture.entities;

import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.features.ArboricultureEntities;
import forestry.arboriculture.features.ArboricultureItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ForestryBoat extends Boat {
	private static final EntityDataAccessor<Integer> DATA_ID_WOOD_TYPE = SynchedEntityData.defineId(ForestryBoat.class, EntityDataSerializers.INT);

	public ForestryBoat(EntityType<? extends Boat> type, Level level) {
		this(type, level, new DropItem());
	}

	private ForestryBoat(EntityType<? extends Boat> type, Level level, DropItem dropItem) {
		super(type, level, dropItem);
		// AbstractBoat stores the supplier; bind it after construction so client sync and loading both select the current wood.
		dropItem.boat = this;
	}

	private static final class DropItem implements java.util.function.Supplier<Item> {
		private ForestryBoat boat;
		@Override public Item get() { return this.boat.getBoatItem(); }
	}

	public ForestryBoat(Level level, double x, double y, double z) {
		this(ArboricultureEntities.BOAT.entityType(), level);

		setPos(x, y, z);
		this.xo = x;
		this.yo = y;
		this.zo = z;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ID_WOOD_TYPE, ForestryWoodType.CAMELTHORN.ordinal());
	}

	protected Item getBoatItem() {
		return ArboricultureItems.BOAT.item(getWoodType());
	}

	// Fall handling is inherited from current vanilla: boats no longer break into planks on landing.

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putString("type", getWoodType().name());
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
		super.readAdditionalSaveData(nbt);
		try {
			setWoodType(ForestryWoodType.valueOf(nbt.getStringOr("type", "CAMELTHORN")));
		} catch (IllegalArgumentException e) {
			setWoodType(ForestryWoodType.CAMELTHORN);
		}
	}

	public void setWoodType(ForestryWoodType woodType) {
		this.entityData.set(DATA_ID_WOOD_TYPE, woodType.ordinal());
	}

	public ForestryWoodType getWoodType() {
		int ordinal = this.entityData.get(DATA_ID_WOOD_TYPE);
		return ordinal >= 0 && ordinal < ForestryWoodType.VALUES.length ? ForestryWoodType.VALUES[ordinal] : ForestryWoodType.CAMELTHORN;
	}
}
