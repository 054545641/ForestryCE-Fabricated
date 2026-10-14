package forestry.apiculture.hives;

import forestry.api.apiculture.IActivityType;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.util.TickHelper;
import forestry.apiculture.network.packets.PacketBeeLogicActive;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import forestry.apiculture.bees.HasFlowersCache;

// the naturally-spawning wild bee hive logic
public class HiveBeekeepingLogic implements IBeekeepingLogic {
	private final HiveBlockEntity housing;
	private final IEffectData[] effectData = new IEffectData[2];
	private final HasFlowersCache hasFlowersCache = new HasFlowersCache(2);
	private final TickHelper tickHelper;

	// Client
	private boolean active;

	public HiveBeekeepingLogic(HiveBlockEntity housing) {
		this.housing = housing;
		this.tickHelper = new TickHelper(housing.getBlockPos().hashCode());
	}

	// / SAVING & LOADING
	@Override
	public void read(CompoundTag CompoundNBT, HolderLookup.Provider registries) {
		setActive(CompoundNBT.getBooleanOr("Active", false));
        this.hasFlowersCache.read(CompoundNBT, registries);
	}

	@Override
	public CompoundTag write(CompoundTag CompoundNBT, HolderLookup.Provider registries) {
		CompoundNBT.putBoolean("Active", this.active);
        this.hasFlowersCache.write(CompoundNBT, registries);

		return CompoundNBT;
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		data.writeBoolean(this.active);
		if (this.active) {
            this.hasFlowersCache.writeData(data);
		}
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		boolean active = data.readBoolean();
		setActive(active);
		if (active) {
            this.hasFlowersCache.readData(data);
		}
	}

	/* Activatable */
	private void setActive(boolean active) {
		if (this.active == active) {
			return;
		}
		this.active = active;

		syncToClient();
	}

	/* UPDATING */

	@Override
	public boolean canWork() {
        this.tickHelper.onTick();

		if (this.tickHelper.updateOnInterval(200)) {
			IBee queen = this.housing.getContainedBee();
            this.hasFlowersCache.update(queen, this.housing);
			Level level = this.housing.getLevel();
			IGenome genome = queen.getGenome();
			boolean canWork = genome.<IActivityType>resolveActive(BeeChromosomes.ACTIVITY).isActive(level.getGameTime(), IActivityType.getBeeDayTime(level), this.housing.getBlockPos()) &&
				(!this.housing.isRaining() || genome.getActiveValue(BeeChromosomes.TOLERATES_RAIN));
			boolean flowerCacheNeedsSync = this.hasFlowersCache.needsSync();

			if (this.active != canWork) {
				setActive(canWork);
			} else if (flowerCacheNeedsSync) {
				syncToClient();
			}
		}

		return this.active;
	}

	@Override
	public void doWork() {

	}

	@Override
	public void clearCachedValues() {

	}

	/* CLIENT */

	@Override
	public void syncToClient() {
		Level world = this.housing.getLevel();
		if (world != null && !world.isClientSide()) {
			NetworkUtil.sendNetworkPacket(new PacketBeeLogicActive(this.housing), this.housing.getBlockPos(), world);
		}
	}

	@Override
	public void syncToClient(ServerPlayer player) {
		Level world = this.housing.getLevel();
		if (world != null && !world.isClientSide()) {
			NetworkUtil.sendToPlayer(new PacketBeeLogicActive(this.housing), player);
		}
	}

	@Override
	public int getBeeProgressPercent() {
		return 0;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean canDoBeeFX() {
		return !Minecraft.getInstance().isPaused() && this.active;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void doBeeFX() {
		IBee queen = this.housing.getContainedBee();
		queen.doFX(this.effectData, this.housing);
	}

	@Override
	public List<BlockPos> getFlowerPositions() {
		return this.hasFlowersCache.getFlowerCoords();
	}

}
