package forestry.apiculture.bees.genetics;

import com.mojang.authlib.GameProfile;
import forestry.api.IForestryApi;
import forestry.api.apiculture.IApiaristTracker;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.IMutationManager;
import forestry.api.core.genetics.ISpecies;
import forestry.core.engine.genetics.BreedingTracker;
import forestry.core.platform.advancements.ForestryAdvancementTriggers;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class ApiaristTracker extends BreedingTracker implements IApiaristTracker {
	private int queensTotal = 0;
	private int dronesTotal = 0;
	private int princessesTotal = 0;

	public ApiaristTracker() {
		super(ForestrySpeciesTypes.BEE);
	}

	@Override
	public void save(CompoundTag nbt) {
		super.save(nbt);

		writeUpdateData(nbt);
	}

	@Override
	protected void writeUpdateData(CompoundTag nbt) {
		nbt.putInt("QueensTotal", this.queensTotal);
		nbt.putInt("PrincessesTotal", this.princessesTotal);
		nbt.putInt("DronesTotal", this.dronesTotal);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);

		this.queensTotal = nbt.getIntOr("QueensTotal", 0);
		this.princessesTotal = nbt.getIntOr("PrincessesTotal", 0);
		this.dronesTotal = nbt.getIntOr("DronesTotal", 0);
	}

	@Override
	public void registerPickup(ISpecies<?> species) {
		IMutationManager<ISpecies<?>> manager = IForestryApi.INSTANCE.getGeneticManager().getMutations(SpeciesUtil.BEE_TYPE.get());

		if (manager.getMutationsFrom(species).isEmpty()) {
			registerSpecies(species);
		}
	}

	@Override
	public void registerQueen(IBee bee) {
		this.queensTotal++;
		registerBirth(bee.getSpecies());
		setDirty();
	}

	@Override
	public int getQueenCount() {
		return this.queensTotal;
	}

	@Override
	public void registerPrincess(IBee bee) {
		this.princessesTotal++;
		registerBirth(bee.getSpecies());
		setDirty();
	}

	@Override
	public int getPrincessCount() {
		return this.princessesTotal;
	}

	@Override
	public void registerDrone(IBee bee) {
		this.dronesTotal++;
		registerBirth(bee.getSpecies());
		setDirty();
	}

	@Override
	public int getDroneCount() {
		return this.dronesTotal;
	}

	@Override
	public void registerProgress(Level level, GameProfile profile, ISpecies<?> species) {
		double researchPercentage = (double) getSpeciesBred() / species.getType().getSpeciesCount();
		ForestryAdvancementTriggers.APICULTURE_RESEARCH.trigger(level, profile, researchPercentage);
	}
}
