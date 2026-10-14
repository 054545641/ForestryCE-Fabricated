package forestry.arboriculture.fruit;

import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.core.IProduct;
import forestry.api.core.genetics.IFruitBearer;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.features.ArboricultureTiles;
import forestry.core.platform.client.ClientsideCode;
import forestry.core.platform.network.IStreamable;
import forestry.core.platform.util.BlockUtil;
import forestry.core.platform.util.NBTUtilForestry;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import forestry.api.arboriculture.ForestryFruits;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class FruitPodBlockEntity extends BlockEntity implements IFruitBearer, IStreamable {
	private static final short MAX_MATURITY = 2;
	private static final IGenome defaultGenome = SpeciesUtil.TREE_TYPE.get().getDefaultSpecies().getDefaultGenome();

	public static final String NBT_MATURITY = "MT";
	public static final String NBT_YIELD = "SP";
	public static final String NBT_FRUIT = "UID";

	private IGenome genome = defaultGenome;
	@Nullable
	private IFruit fruit = null;
	// The id of the fruit reference value, tracked alongside the resolved object for serialization
	// (IFruit has no id() of its own, and reference chromosomes store the id, not the object).
	@Nullable
	private Identifier fruitId = null;
	private short maturity;
	private float yield;

	public FruitPodBlockEntity(BlockPos pos, BlockState state) {
		super(ArboricultureTiles.PODS.tileType(), pos, state);
	}

	public void setProperties(IGenome genome, IFruit allele, float yield) {
		this.genome = genome;
		this.fruit = allele;
		this.fruitId = genome.getActiveValue(TreeChromosomes.FRUIT);
		this.yield = yield;
		setChanged();
	}

	/* SAVING & LOADING */
	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		if (this.fruit != null) {
			data.writeBoolean(true);
			data.writeIdentifier(this.fruitId);
		} else {
			data.writeBoolean(false);
		}
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		if (data.readBoolean()) {
			Identifier fruitId = data.readIdentifier();
			IFruit fruit = SpeciesUtil.TREE_TYPE.get().getFruitSafe(fruitId);
			if (fruit != null) {
				this.fruit = fruit;
				this.fruitId = fruitId;
				ClientsideCode.markForUpdate(this.worldPosition);
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
		if (this.fruit != null) {
			compoundNBT.putString(NBT_FRUIT, this.fruitId.toString());
		}
		compoundNBT.putShort(NBT_MATURITY, this.maturity);
		compoundNBT.putFloat(NBT_YIELD, this.yield);
	
		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

		String fruitNbt = nbt.getStringOr(NBT_FRUIT, "");
		if (!fruitNbt.isEmpty()) {
			this.fruitId = Identifier.parse(fruitNbt);
			// Nullable lookup so an unregistered/stale id gracefully falls back to cocoa instead of crashing on load.
			this.fruit = SpeciesUtil.TREE_TYPE.get().getFruitSafe(this.fruitId);
		}
		if (this.fruit == null) {
			this.fruitId = ForestryFruits.COCOA;
			this.fruit = SpeciesUtil.TREE_TYPE.get().getFruit(ForestryFruits.COCOA);
		}

		this.maturity = nbt.getShortOr(NBT_MATURITY, (short) 0);
		this.yield = nbt.getFloatOr(NBT_YIELD, 0.0f);
	}

	/* UPDATING */
	public void onBlockTick(RandomSource rand) {
		if (canMature() && rand.nextFloat() <= this.yield) {
			addRipeness(0.5f);
		}
	}

	public boolean canMature() {
		return this.maturity < MAX_MATURITY;
	}

	public short getMaturity() {
		return this.maturity;
	}

	public ItemStack getPickBlock() {
		if (this.fruit == null) {
			return ItemStack.EMPTY;
		}
		List<IProduct> products = this.fruit.getProducts();

		ItemStack pickBlock = ItemStack.EMPTY;
		float maxChance = 0.0f;
		for (IProduct product : products) {
			if (maxChance < product.chance()) {
				maxChance = product.chance();
				pickBlock = product.createStack();
			}
		}

		pickBlock.setCount(1);
		return pickBlock;
	}

	public List<ItemStack> getDrops() {
		return this.fruit == null ? List.of() : this.fruit.getFruits(this.genome, this.level, this.maturity);
	}

	/* NETWORK */
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		return NBTUtilForestry.writeStreamableToNbt(this, tag, registries);
	}

	@Override
	public void handleUpdateTag(ValueInput input) {
		CompoundTag tag = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		NBTUtilForestry.readStreamableFromNbt(this, tag, registries);
	}

	@Override
	public void onDataPacket(Connection net, ValueInput input) {
		handleUpdateTag(input);
	}

	/* IFRUITBEARER */
	@Override
	public boolean hasFruit() {
		return true;
	}

	@Override
	public List<ItemStack> pickFruit(ItemStack tool) {
		List<ItemStack> fruits = getDrops();
        this.maturity = 0;

		BlockState oldState = getBlockState();
		BlockState newState = oldState.setValue(CocoaBlock.AGE, 0);
		BlockUtil.setBlockWithBreakSound(this.level, getBlockPos(), newState, oldState);

		return fruits;
	}

	@Override
	public float getRipeness() {
		return (float) this.maturity / MAX_MATURITY;
	}

	@Override
	public void addRipeness(float add) {
		int previousAge = this.maturity;

        this.maturity += MAX_MATURITY * add;
		if (this.maturity > MAX_MATURITY) {
            this.maturity = MAX_MATURITY;
		}

		int age = this.maturity;
		if (age - previousAge > 0) {
			BlockState state = getBlockState().setValue(CocoaBlock.AGE, age);
            this.level.setBlockAndUpdate(getBlockPos(), state);
		}
	}
}
