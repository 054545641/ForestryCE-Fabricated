package forestry.apiculture.alveary.multiblock;

import forestry.api.apiculture.*;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.HumidityType;
import forestry.api.core.IErrorLogic;
import forestry.api.core.TemperatureType;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.api.core.multiblock.IMultiblockController;
import forestry.apiculture.alveary.AlvearyBlock;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.apiculture.alveary.AlvearyMenu;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.multiblock.MultiblockController;
import forestry.core.platform.multiblock.MultiblockTileEntityForestry;
import forestry.core.platform.multiblock.pattern.MultiblockPattern;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.owner.IOwnedTile;
import forestry.core.platform.owner.IOwnerHandler;
import forestry.core.platform.tile.ITitled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import forestry.core.platform.inventory.InventoryTransferView;

import javax.annotation.Nullable;

public abstract class AbstractAlvearyBlockEntity extends MultiblockTileEntityForestry<AlvearyMultiblockLogic> implements IBeeHousing, IAlvearyComponent<AlvearyMultiblockLogic>, IOwnedTile, IStreamableGui, ITitled, IClimateProvider {
	private final String translationKey;

	// For Forestry only
	public AbstractAlvearyBlockEntity(AlvearyBlock.Type type, BlockPos pos, BlockState state) {
		this(type.tileFeature().tileType(), ApicultureBlocks.ALVEARY.get(type).getTranslationKey(), pos, state);
	}

	// For addons
	public AbstractAlvearyBlockEntity(BlockEntityType<?> type, String translationKey, BlockPos pos, BlockState state) {
		super(type, pos, state, new AlvearyMultiblockLogic());

		this.translationKey = translationKey;
	}

	@Override
	public MultiblockController createController(Level level) {
		return new AlvearyController(level);
	}

	@Override
	public MultiblockPattern getPattern() {
		return AlvearyPattern.ALVEARY_PATTERN;
	}

	@Override
	public String patternTypeId() {
		return AlvearyPattern.PART;
	}

	@Override
	public void onMachineAssembled(IMultiblockController multiblockController, BlockPos minCoord, BlockPos maxCoord) {
		Block block = getBlockState().getBlock();
		if (block instanceof AlvearyBlock alveary) {
			this.level.setBlockAndUpdate(getBlockPos(), alveary.getNewState(this));
		}
	}

	@Override
	public void onMachineBroken() {
		Block block = getBlockState().getBlock();
		if (block instanceof AlvearyBlock alveary) {
			this.level.setBlockAndUpdate(getBlockPos(), alveary.getNewState(this));
		}
		setChanged();
	}

	@Nullable
	public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction facing) {
		return InventoryTransferView.automation(getInternalInventory(), facing);
	}

	/* IHousing */
	@Override
	public Holder<Biome> getBiome() {
		return getMultiblockLogic().getController().getBiome();
	}

	/* IBeeHousing */
	@Override
	public Iterable<IBeeModifier> getBeeModifiers() {
		return getMultiblockLogic().getController().getBeeModifiers();
	}

	@Override
	public Iterable<IBeeListener> getBeeListeners() {
		return getMultiblockLogic().getController().getBeeListeners();
	}

	@Override
	public IBeeHousingInventory getBeeInventory() {
		return getMultiblockLogic().getController().getBeeInventory();
	}

	@Override
	public IBeekeepingLogic getBeekeepingLogic() {
		return getMultiblockLogic().getController().getBeekeepingLogic();
	}

	@Override
	public Vec3 getBeeFXCoordinates() {
		return getMultiblockLogic().getController().getBeeFXCoordinates();
	}

	@Override
	public TemperatureType temperature() {
		return getMultiblockLogic().getController().temperature();
	}

	@Override
	public HumidityType humidity() {
		return getMultiblockLogic().getController().humidity();
	}

	@Override
	public int getBlockLightValue() {
		return getMultiblockLogic().getController().getBlockLightValue();
	}

	@Override
	public boolean canBlockSeeTheSky() {
		return getMultiblockLogic().getController().canBlockSeeTheSky();
	}

	@Override
	public boolean isRaining() {
		return getMultiblockLogic().getController().isRaining();
	}

	@Override
	public IErrorLogic getErrorLogic() {
		return getMultiblockLogic().getController().getErrorLogic();
	}

	@Override
	public IOwnerHandler getOwnerHandler() {
		return getMultiblockLogic().getController().getOwnerHandler();
	}

	@Override
	public IInventoryAdapter getInternalInventory() {
		return getMultiblockLogic().getController().getInternalInventory();
	}

	@Override
	public Component getTitle() {
		return Component.translatable(this.translationKey);
	}

	/* IStreamableGui */
	@Override
	public void writeGuiData(RegistryFriendlyByteBuf data) {
		getMultiblockLogic().getController().writeGuiData(data);
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
		getMultiblockLogic().getController().readGuiData(data);
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new AlvearyMenu(windowId, player.getInventory(), this);
	}

	@Override
	public Component getDisplayName() {
		return getTitle();
	}
}
