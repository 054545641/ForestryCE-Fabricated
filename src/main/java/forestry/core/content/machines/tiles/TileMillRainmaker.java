package forestry.core.content.machines.tiles;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import forestry.core.platform.transfer.CommitParticipant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import forestry.api.ForestryConstants;
import forestry.core.platform.advancements.AdvancementHelper;
import forestry.core.platform.client.ParticleRender;
import forestry.core.content.machines.TileMill;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.inventory.InventoryRainmaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;

import javax.annotation.Nullable;
import forestry.api.ForestryDataMaps;
import forestry.api.core.machines.fuels.RainmakerSubstrate;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileMillRainmaker extends TileMill {
	private record ChargeState(int charge, float speed, int duration, boolean reverse) {}
	private final CommitParticipant<ChargeState> chargeJournal = new CommitParticipant<>() {
		@Override protected ChargeState createSnapshot() { return new ChargeState(charge, speed, duration, reverse); }
		@Override protected void readSnapshot(ChargeState value) {
			charge = value.charge(); speed = value.speed(); duration = value.duration(); reverse = value.reverse();
		}
		@Override protected void onCommitted(ChargeState value) { setChanged(); sendNetworkUpdate(); }
	};
	private static final Identifier USE_RAINMAKER = ForestryConstants.forestry("use_rainmaker");
	private static final int MAX_PARTICLE_COUNT = 64;

	private int duration;
	private boolean reverse;

	// height of the cloud column climbing above the rainmaker, in sendParticles calls
	private int yParticle;
	private int particleCount = MAX_PARTICLE_COUNT;

	public TileMillRainmaker(BlockPos pos, BlockState state) {
		super(FactoryTiles.RAINMAKER.tileType(), pos, state);
        this.speed = 0.01f;
		setInternalInventory(new InventoryRainmaker(this));
	}

	@Override
	public void openGui(ServerPlayer player, InteractionHand hand, BlockPos pos) {
		if (!player.level().isClientSide()) {
			ItemStack heldItem = player.getItemInHand(hand);

			// We don't have a gui, but we can be activated
			RainmakerSubstrate substrate = ForestryDataMaps.RAINMAKER_FUELS.get(heldItem.typeHolder());
			if (substrate != null && this.charge == 0) {
				addCharge(substrate);
				if (!player.isCreative()) {
					heldItem.shrink(1);
				}
				AdvancementHelper.tryUnlock(player, USE_RAINMAKER);
			}
			sendNetworkUpdate();
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

        this.charge = compoundNBT.getIntOr("Charge", 0);
        this.progress = compoundNBT.getFloatOr("Progress", 0.0f);
        this.stage = compoundNBT.getIntOr("Stage", 0);
        this.duration = compoundNBT.getIntOr("Duration", 0);
        this.reverse = compoundNBT.getBooleanOr("Reverse", false);
		this.speed = compoundNBT.getFloatOr("Speed", 0.01f);
	}


	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

		compoundNBT.putInt("Charge", this.charge);
		compoundNBT.putFloat("Progress", this.progress);
		compoundNBT.putInt("Stage", this.stage);
		compoundNBT.putInt("Duration", this.duration);
		compoundNBT.putBoolean("Reverse", this.reverse);
		compoundNBT.putFloat("Speed", this.speed);

		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	public void addCharge(RainmakerSubstrate substrate) {
		try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			this.chargeJournal.updateSnapshots(transaction);
			this.charge = 1;
			this.speed = substrate.speed();
			this.duration = substrate.duration();
			this.reverse = substrate.reverse();
			transaction.commit();
		}
	}

	public boolean processSubstrate() {
		ItemStack stack = getItem(0);
		if (stack.isEmpty() || !getInternalInventory().canSlotAccept(0, stack)) return false;
		try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			if (getTransferHandler().getSlot(0).extract(ItemVariant.of(stack), 1, transaction) != 1) return false;
			addCharge(ForestryDataMaps.RAINMAKER_FUELS.get(stack.typeHolder()));
			transaction.commit();
			return true;
		}
	}

	@Override
	protected void update(Level level, BlockPos pos, boolean isSimulating) {
		if (isSimulating) processSubstrate();
		super.update(level, pos, isSimulating);
		if (isSimulating && (this.charge != 0 || this.progress != 0)) setChanged();

		if (this.particleCount < MAX_PARTICLE_COUNT && level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.CLOUD,
				pos.getX() + 0.5,
				this.yParticle,
				pos.getZ() + 0.5,
				10,
				0.025f,
				1f,
				0.025f,
				0.01f
			);

			this.yParticle += 2;
			this.particleCount++;
		}
	}

	@Override
	public void activate(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10000.0F, 0.8F + level.getRandom().nextFloat() * 0.2F);

			float f = pos.getX() + 0.5F;
			float f1 = pos.getY() + level.getRandom().nextFloat() * 6F / 16F;
			float f2 = pos.getZ() + 0.5F;
			float f3 = 0.52F;
			float f4 = level.getRandom().nextFloat() * 0.6F - 0.3F;

			ParticleRender.addEntityExplodeFX(level, f - f3, f1, f2 + f4);
			ParticleRender.addEntityExplodeFX(level, f + f3, f1, f2 + f4);
			ParticleRender.addEntityExplodeFX(level, f + f4, f1, f2 - f3);
			ParticleRender.addEntityExplodeFX(level, f + f4, f1, f2 + f3);
		} else {
			// restart the cloud column so it climbs again from this activation
			this.particleCount = 0;
			this.yParticle = pos.getY() + 2;

			if (this.reverse) {
				((net.minecraft.server.level.ServerLevel) level).getWeatherData().setRaining(false);
			} else {
				((net.minecraft.server.level.ServerLevel) level).getWeatherData().setRaining(true);
				((net.minecraft.server.level.ServerLevel) level).getWeatherData().setRainTime(this.duration);
			}
            this.charge = 0;
            this.duration = 0;
            this.reverse = false;
			setChanged();
			sendNetworkUpdate();
		}
	}

	@Override
	@Nullable
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return null;
	}

	@Override
	protected boolean hasGui() {
		return false;
	}
}
