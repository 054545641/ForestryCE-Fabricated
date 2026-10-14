package forestry.arboriculture.leaves;

import forestry.api.IForestryApi;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.ILeafTickHandler;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.client.IForestryClientApi;
import forestry.api.core.climate.IBiomeProvider;
import forestry.api.core.HumidityType;
import forestry.api.core.ISpectacleBlock;
import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IFruitBearer;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.alleles.*;
import forestry.api.lepidopterology.IButterflyNursery;
import forestry.api.lepidopterology.genetics.IButterfly;
import forestry.api.lepidopterology.genetics.IButterflySpeciesType;
import forestry.arboriculture.features.ArboricultureTiles;
import forestry.arboriculture.network.IRipeningPacketReceiver;
import forestry.arboriculture.network.PacketRipeningUpdate;
import forestry.core.platform.client.ClientsideCode;
import forestry.core.platform.network.packets.PacketTileStream;
import forestry.core.platform.util.ColourUtil;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.List;
import forestry.api.ForestryConstants;
import forestry.arboriculture.trees.TileTreeContainer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class LeavesBlockEntity extends TileTreeContainer implements IFruitBearer, IButterflyNursery, IRipeningPacketReceiver, IBiomeProvider, ISpectacleBlock {
	private static final String NBT_RIPENING = "RT";
	private static final String NBT_DAMAGE = "ENC";
	private static final String NBT_FRUIT_LEAF = "FL";
	private static final String NBT_MATURATION = "CATMAT";
	private static final String NBT_CATERPILLAR = "CATER";

	public static final ModelProperty<ITreeSpecies> PROPERTY_SPECIES = new ModelProperty<>();
	public static final ModelProperty<Boolean> PROPERTY_POLLINATED = new ModelProperty<>();
	public static final ModelProperty<Identifier> PROPERTY_FRUIT_TEXTURE = new ModelProperty<>();

	private int colourFruits;

	@Nullable
	private Identifier fruitSprite;
	@Nullable
	private ITreeSpecies species;
	@Nullable
	private IButterfly caterpillar;
	// Set when a save holds a caterpillar but the butterfly jar is gone. Written back out untouched, so
	// removing the jar for one session does not wipe every caterpillar in the world.
	@Nullable
	private Tag unloadableCaterpillar;

	private boolean isFruitLeaf;
	private boolean isPollinatedState;
	private int ripeningTime;
	private short ripeningPeriod = Short.MAX_VALUE - 1;

	private int maturationTime;
	private int damage;

	private IEffectData[] effectData = new IEffectData[2];

	public LeavesBlockEntity(BlockPos pos, BlockState state) {
		super(ArboricultureTiles.LEAVES.tileType(), pos, state);
	}

	/* SAVING & LOADING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

		this.ripeningTime = nbt.getIntOr(NBT_RIPENING, 0);
		this.damage = nbt.getIntOr(NBT_DAMAGE, 0);
		this.isFruitLeaf = nbt.getBooleanOr(NBT_FRUIT_LEAF, false);
		boolean checkFruit = !nbt.contains(NBT_FRUIT_LEAF);

		Tag caterpillarNbt = nbt.get(NBT_CATERPILLAR);
		this.caterpillar = null;
		this.unloadableCaterpillar = null;
		this.maturationTime = 0;
		this.species = null;
		this.fruitSprite = null;
		if (caterpillarNbt != null) {
			this.maturationTime = nbt.getIntOr(NBT_MATURATION, 0);

			IButterflySpeciesType butterflyType = SpeciesUtil.getButterflyTypeSafe();
			if (butterflyType != null) {
				this.caterpillar = SpeciesUtil.deserializeIndividual(butterflyType, caterpillarNbt);
			} else {
				this.unloadableCaterpillar = caterpillarNbt;
			}
		}

		ITree tree = getTree();
		if (tree != null) {
			setTree(tree);

			if (checkFruit) {
				setFruit(tree, false);
			} else if (this.isFruitLeaf) {
				setFruit(tree, true);
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

		nbt.putInt(NBT_RIPENING, this.ripeningTime);
		nbt.putInt(NBT_DAMAGE, this.damage);
		nbt.putBoolean(NBT_FRUIT_LEAF, this.isFruitLeaf);

		if (this.caterpillar != null) {
			nbt.putInt(NBT_MATURATION, this.maturationTime);

			Tag caterpillarNbt = SpeciesUtil.serializeIndividual(this.caterpillar);
			if (caterpillarNbt != null) {
				nbt.put(NBT_CATERPILLAR, caterpillarNbt);
			}
		} else if (this.unloadableCaterpillar != null) {
			nbt.putInt(NBT_MATURATION, this.maturationTime);
			nbt.put(NBT_CATERPILLAR, this.unloadableCaterpillar);
		}

		ValueIOUtil.mergeInto(output, nbt);
	}

	@Override
	public void onBlockTick(Level worldIn, BlockPos pos, BlockState state, RandomSource rand) {
		ITree tree = getTree();
		if (tree == null) {
			return;
		}

		IGenome genome = tree.getGenome();
		ITreeSpecies primary = genome.resolveActive(TreeChromosomes.SPECIES);

		boolean isDestroyed = isDestroyed(tree, this.damage);
		for (ILeafTickHandler tickHandler : primary.getType().getLeafTickHandlers()) {
			if (tickHandler.onRandomLeafTick(tree, this.level, rand, getBlockPos(), isDestroyed)) {
				return;
			}
		}

		if (isDestroyed) {
			return;
		}

		if (this.damage > 0) {
            this.damage--;
			setChanged();
		}

		if (hasFruit() && getRipeningTime() < this.ripeningPeriod) {
			//ITreekeepingMode treekeepingMode = SpeciesUtil.TREE_TYPE.get().getTreekeepingMode(level);
			//float sappinessModifier = treekeepingMode.getSappinessModifier(genome, 1f);
			float sappiness = genome.getActiveValue(TreeChromosomes.SAPPINESS);// * sappinessModifier;

			if (rand.nextFloat() < sappiness) {
                this.ripeningTime++;
				setChanged();
				sendNetworkUpdateRipening();
			}
		}

		if (this.caterpillar != null) {
			matureCaterpillar();
		}

        this.effectData = tree.doEffect(this.effectData, this.level, getBlockPos());
	}

	@Override
	public void setTree(@Nullable ITree tree) {
		ITree oldTree = getTree();
		super.setTree(tree);
		if (tree == null) {
			this.species = null;
			this.fruitSprite = null;
			this.isFruitLeaf = false;
			this.isPollinatedState = false;
			this.effectData = new IEffectData[2];
			requestModelDataUpdate();
			setChanged();
			return;
		}

		IGenome genome = tree.getGenome();
		this.species = genome.resolveActive(TreeChromosomes.SPECIES);

		// update fruit state if genome changed
		if (oldTree != null && tree.getSpecies() != oldTree.getSpecies() || (this.level != null && this.level.isClientSide())) {
			setFruit(tree, false);
		}

		requestModelDataUpdate();
		setChanged();
	}

	// alwaysFruit: if true, random check for fruit chance will always succeed
	public void setFruit(ITree tree, boolean alwaysFruit) {
		IGenome genome = tree.getGenome();

		if (tree.hasFruitLeaves() && this.level != null && !this.level.isClientSide()) {
			IFruit fruitProvider = genome.resolveActive(TreeChromosomes.FRUIT);
			if (fruitProvider.isFruitLeaf()) {
				this.isFruitLeaf = alwaysFruit || fruitProvider.getFruitChance(genome, this.level) >= this.level.getRandom().nextFloat();
			}
		}

		if (this.isFruitLeaf) {
			IFruit fruit = genome.resolveActive(TreeChromosomes.FRUIT);
			if (this.level != null && this.level.isClientSide()) {
				this.fruitSprite = fruit.getSprite(genome, this.level, getBlockPos(), getRipeningTime());
			}

			this.ripeningPeriod = (short) fruit.getRipeningPeriod();
		} else if (this.level != null && this.level.isClientSide()) {
			this.fruitSprite = null;
		}
	}

	/* INFORMATION */
	private static boolean isDestroyed(@Nullable ITree tree, int damage) {
		return tree != null && damage > tree.getResilience();
	}

	public boolean isPollinated() {
		ITree tree = getTree();
		return tree != null && !isDestroyed(tree, this.damage) && tree.getMate() != null;
	}

	@OnlyIn(Dist.CLIENT)
	public int getFoliageColour() {
		final int baseColor = IForestryClientApi.INSTANCE.getTreeManager().getTint(this.species).get(this.level instanceof net.minecraft.client.renderer.block.BlockAndTintGetter tintLevel ? tintLevel : null, this.worldPosition);

		ITree tree = getTree();
		if (isDestroyed(tree, this.damage)) {
			return ColourUtil.addRGBComponents(baseColor, 92, 61, 0);
		} else if (this.caterpillar != null) {
			return ColourUtil.multiplyRGBComponents(baseColor, 1.5f);
		} else {
			return baseColor;
		}
	}

	public int getFruitColour() {
		if (this.colourFruits == 0 && hasFruit()) {
            this.colourFruits = determineFruitColour();
		}
		return this.colourFruits;
	}

	private int determineFruitColour() {
		ITree tree = getTree();
		if (tree == null) {
			// Fallback template for fruit color when this leaf block has no contained tree; fall back to the
			// default species instead of throwing if a datapack has since removed sour cherry.
			ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
			ITreeSpecies sourCherry = type.getSpeciesSafe(ForestryTreeSpecies.SOUR_CHERRY);
			tree = (sourCherry != null ? sourCherry : type.getDefaultSpecies()).createIndividual();
		}
		IGenome genome = tree.getGenome();
		IFruit fruit = genome.resolveActive(TreeChromosomes.FRUIT);
		return fruit.getColour(genome, this.level, getBlockPos(), getRipeningTime());
	}

	@Override
	public ModelData getModelData() {
		ModelData.Builder builder = ModelData.builder();
		builder.with(PROPERTY_SPECIES, this.species);
		builder.with(PROPERTY_POLLINATED, this.isPollinatedState);
		builder.with(PROPERTY_FRUIT_TEXTURE, this.fruitSprite);
		return builder.build();
	}

	public int getRipeningTime() {
		return this.ripeningTime;
	}

	public void setMate(ITree pollen) {
		getTree().setMate(pollen.getGenome());
		setChanged();
		if (!this.level.isClientSide()) {
			sendNetworkUpdate();
		}
	}

	/* NETWORK */
	public void sendNetworkUpdate() {
		NetworkUtil.sendNetworkPacket(new PacketTileStream(this), this.worldPosition, this.level);
	}

	private void sendNetworkUpdateRipening() {
		if (isRemoved()) {
			return;
		}
		int newColourFruits = determineFruitColour();
		if (newColourFruits == this.colourFruits) {
			return;
		}
        this.colourFruits = newColourFruits;

		PacketRipeningUpdate ripeningUpdate = new PacketRipeningUpdate(this);
		NetworkUtil.sendNetworkPacket(ripeningUpdate, this.worldPosition, this.level);
		setChanged();
	}

	// Flags for network data
	private static final short FLAG_HAS_FRUIT = 1;
	private static final short FLAG_IS_POLLINATED = 1 << 1;
	private static final short FLAG_HAS_ACTIVE_EFFECT = 1 << 2;
	private static final short FLAG_HAS_INACTIVE_EFFECT = 1 << 3;

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);

		byte leafState = 0;
		ITree tree = getTree();
		if (tree == null) {
			data.writeByte(0);
			return;
		}
		IGenome genome = tree.getGenome();
		AllelePair<Identifier> effects = genome.getAllelePair(TreeChromosomes.EFFECT);
		Identifier noneEffect = ForestryConstants.forestry("tree_effect_none");
		boolean hasActiveEffect = !effects.active().value().equals(noneEffect);
		boolean hasInactiveEffect = !effects.inactive().value().equals(noneEffect);
		boolean hasFruit = hasFruit();

		if (isPollinated()) {
			leafState |= FLAG_IS_POLLINATED;
		}

		if (hasFruit) {
			leafState |= FLAG_HAS_FRUIT;
		}

		if (hasActiveEffect) {
			leafState |= FLAG_HAS_ACTIVE_EFFECT;
		}
		if (hasInactiveEffect) {
			leafState |= FLAG_HAS_INACTIVE_EFFECT;
		}

		data.writeByte(leafState);

		if (hasFruit) {
			Identifier fruitId = genome.getActiveValue(TreeChromosomes.FRUIT);
			int colourFruits = getFruitColour();

			data.writeIdentifier(fruitId);
			data.writeInt(colourFruits);
		}

		// todo come up with a way to send numeric IDs instead of string IDs
		if (hasActiveEffect) {
			data.writeIdentifier(effects.active().value());
		}
		if (hasInactiveEffect) {
			data.writeIdentifier(effects.inactive().value());
		}
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		Identifier speciesId = null;
		if (data.readBoolean()) {
			speciesId = data.readIdentifier(); // this is called instead of super.readData, be careful!
		}
		byte leafState = data.readByte();

		this.isPollinatedState = (leafState & FLAG_IS_POLLINATED) != 0;
		this.isFruitLeaf = (leafState & FLAG_HAS_FRUIT) != 0;
		boolean hasActiveEffect = (leafState & FLAG_HAS_ACTIVE_EFFECT) != 0;
		boolean hasInactiveEffect = (leafState & FLAG_HAS_INACTIVE_EFFECT) != 0;
		Identifier fruitId = null;

		if (this.isFruitLeaf) {
			fruitId = data.readIdentifier();
            this.colourFruits = data.readInt();
		}

		Identifier activeEffectAlleleId = hasActiveEffect ? data.readIdentifier() : null;
		Identifier inactiveEffectAlleleId = hasInactiveEffect ? data.readIdentifier() : null;

		ITreeSpecies treeTemplate = speciesId == null ? null : SpeciesUtil.TREE_TYPE.get().getSpeciesSafe(speciesId);
		if (treeTemplate != null) {
			IdentityHashMap<IChromosome<?>, AllelePair<?>> alleles = new IdentityHashMap<>(2);

			// Fruit (used as both active and inactive)
			if (fruitId != null) {
				alleles.put(TreeChromosomes.FRUIT, AllelePair.both(Allele.reference(fruitId)));
			}

			// Effect (active and inactive are separate since they can stack)
			Allele<Identifier> activeEffectAllele = activeEffectAlleleId != null ? Allele.reference(activeEffectAlleleId) : null;
			Allele<Identifier> inactiveEffectAllele = inactiveEffectAlleleId != null ? Allele.reference(inactiveEffectAlleleId) : null;
			if (activeEffectAllele != null || inactiveEffectAllele != null) {
				alleles.put(TreeChromosomes.EFFECT, new AllelePair<>(
					activeEffectAllele != null ? activeEffectAllele : Allele.reference(ForestryConstants.forestry("tree_effect_none")),
					inactiveEffectAllele != null ? inactiveEffectAllele : Allele.reference(ForestryConstants.forestry("tree_effect_none"))
				));
			}

			ITree tree = treeTemplate.createIndividualFromPairs(alleles);
			if (this.isPollinatedState) {
				tree.setMate(tree.getGenome());
			}

			setTree(tree);

			ClientsideCode.markForUpdate(this.worldPosition);
		} else {
			setTree(null);
		}
	}

	@Override
	public void fromRipeningPacket(int newColourFruits) {
		if (newColourFruits == this.colourFruits) {
			return;
		}
        this.colourFruits = newColourFruits;
		ClientsideCode.markForUpdate(this.worldPosition);
	}

	/* IFRUITBEARER */
	@Override
	public List<ItemStack> pickFruit(ItemStack tool) {
		ITree tree = getTree();
		if (tree == null || !hasFruit()) {
			return List.of();
		}

		List<ItemStack> produceStacks = tree.produceStacks(this.level, this.worldPosition, getRipeningTime());
        this.ripeningTime = 0;
		setChanged();
		sendNetworkUpdateRipening();
		return produceStacks;
	}

	@Override
	public float getRipeness() {
		if (this.ripeningPeriod == 0) {
			return 1.0f;
		}
		if (getTree() == null) {
			return 0f;
		}
		return this.ripeningTime / (float) this.ripeningPeriod;
	}

	@Override
	public boolean hasFruit() {
		return this.isFruitLeaf && !isDestroyed(getTree(), this.damage);
	}

	@Override
	public void addRipeness(float add) {
		if (getTree() == null || !this.isFruitLeaf || getRipeningTime() >= this.ripeningPeriod) {
			return;
		}
		this.ripeningTime += this.ripeningPeriod * add;
		setChanged();
		sendNetworkUpdateRipening();
	}

	/* IBUTTERFLYNURSERY */

	private void matureCaterpillar() {
		if (this.caterpillar == null) {
			return;
		}
        this.maturationTime++;
		setChanged();

		ITree tree = getTree();
		boolean wasDestroyed = isDestroyed(tree, this.damage);
        this.damage += this.caterpillar.getGenome().getActiveValue(ButterflyChromosomes.METABOLISM);

		IGenome caterpillarGenome = this.caterpillar.getGenome();
		int caterpillarMatureTime = Math.round((float) caterpillarGenome.getActiveValue(ButterflyChromosomes.LIFESPAN) / (caterpillarGenome.getActiveValue(ButterflyChromosomes.FERTILITY) * 2));

		if (this.maturationTime >= caterpillarMatureTime) {
			if (SpeciesUtil.BUTTERFLY_TYPE.get().plantCocoon(this.level, this.worldPosition, this.caterpillar, 0, false) != null) {
				setCaterpillar(null);
			}
		} else if (!wasDestroyed && isDestroyed(tree, this.damage)) {
			sendNetworkUpdate();
		}
	}

	@Override
	@Nullable
	public IButterfly getCaterpillar() {
		return this.caterpillar;
	}

	@Override
	public IIndividual getNanny() {
		return getTree();
	}

	@Override
	public void setCaterpillar(@Nullable IButterfly caterpillar) {
        this.maturationTime = 0;
		this.caterpillar = caterpillar;
		this.unloadableCaterpillar = null;
		setChanged();
		sendNetworkUpdate();
	}

	@Override
	public boolean canNurse(IButterfly caterpillar) {
		ITree tree = getTree();
		return !isDestroyed(tree, this.damage) && this.caterpillar == null;
	}

	@Override
	public Holder<Biome> getBiome() {
		return this.level.getBiome(this.worldPosition);
	}

	@Override
	public TemperatureType temperature() {
		return IForestryApi.INSTANCE.getClimateManager().getTemperature(getBiome());
	}

	@Override
	public HumidityType humidity() {
		return IForestryApi.INSTANCE.getClimateManager().getHumidity(getBiome());
	}

	@Override
	public boolean isHighlighted(Player player) {
		return isPollinated();
	}
}
