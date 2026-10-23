package forestry.core.engine.genetics;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import forestry.api.IForestryApi;
import forestry.api.core.ForestryEvent;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.platform.advancements.ForestryAdvancementTriggers;
import forestry.core.platform.network.packets.PacketGenomeTrackerSync;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public abstract class BreedingTracker extends SavedData implements IBreedingTracker {
	private static final String SPECIES_KEY = "SD";
	private static final String MUTATIONS_KEY = "MD";
	private static final String RESEARCHED_KEY = "RD";
	private static final String MUTATION_FORMAT = "%s-%s=%s";

	// used for network deserialization
	public static final String TYPE_KEY = "TYPE";

	private final Identifier typeId;
	private final Set<Identifier> discoveredSpecies = new HashSet<>();
	private final Set<String> discoveredMutations = new HashSet<>();
	private final Set<String> researchedMutations = new HashSet<>();

	@Nullable
	private GameProfile username;
	@Nullable
	private Level level;

	protected BreedingTracker(Identifier typeId) {
		this.typeId = typeId;
	}

	public void setUsername(@Nullable GameProfile username) {
		this.username = username;
	}

	public void setLevel(@Nullable Level level) {
		this.level = level;
	}

	@Override
	public void syncToPlayer(Player player) {
		if (player instanceof ServerPlayer serverPlayer && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(serverPlayer, PacketGenomeTrackerSync.TYPE)) {
			CompoundTag nbt = new CompoundTag();
			save(nbt);
			PacketGenomeTrackerSync packet = new PacketGenomeTrackerSync(nbt);
			NetworkUtil.sendToPlayer(packet, (ServerPlayer) player);
		}
	}

	// Sends the given species and mutations to client. Use to sync serverside breeding updates to the client.
	private void sendUpdate(Collection<Identifier> discoveredSpecies, Collection<String> discoveredMutations, Collection<String> researchedMutations) {
		if (this.level != null && this.username != null && this.username.name() != null) {
			Player player = this.level.getPlayerByUUID(this.username.id());

			if (player instanceof ServerPlayer serverPlayer && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(serverPlayer, PacketGenomeTrackerSync.TYPE)) {
				CompoundTag nbt = new CompoundTag();
				writeAllValues(nbt, discoveredSpecies, discoveredMutations, researchedMutations);
				writeUpdateData(nbt);
				PacketGenomeTrackerSync packet = new PacketGenomeTrackerSync(nbt);
				NetworkUtil.sendToPlayer(packet, (ServerPlayer) player);
			}
		}
	}


	/* HELPER FUNCTIONS TO PREVENT OBFUSCATION OF INTERFACE METHODS */
	@OverridingMethodsMustInvokeSuper
	@Override
	public void save(CompoundTag nbt) {
		writeAllValues(nbt, this.discoveredSpecies, this.discoveredMutations, this.researchedMutations);
	}

	// Used to sync additional data to the client when breeding statistics change
	protected void writeUpdateData(CompoundTag nbt) {
	}

	@OverridingMethodsMustInvokeSuper
	@Override
	public void load(CompoundTag nbt) {
        var species = new HashSet<Identifier>();
        var mutations = new HashSet<String>();
        var researched = new HashSet<String>();
        readValuesFromNBT(nbt, value -> species.add(Identifier.parse(value)), SPECIES_KEY);
        readValuesFromNBT(nbt, mutations::add, MUTATIONS_KEY);
        readValuesFromNBT(nbt, researched::add, RESEARCHED_KEY);
        this.discoveredSpecies.addAll(species);
        this.discoveredMutations.addAll(mutations);
        this.researchedMutations.addAll(researched);
	}

	// helper method to call the three writeValuesToNbt
	private void writeAllValues(CompoundTag nbt, Collection<Identifier> discoveredSpecies, Collection<String> discoveredMutations, Collection<String> researchedMutations) {
		// Required for network deserialization in PacketGenomeTrackerSync
		nbt.putString(TYPE_KEY, this.typeId.toString());

		writeValuesToNBT(nbt, Iterables.transform(discoveredSpecies, Identifier::toString), SPECIES_KEY);
		writeValuesToNBT(nbt, discoveredMutations, MUTATIONS_KEY);
		writeValuesToNBT(nbt, researchedMutations, RESEARCHED_KEY);
	}

	// helper method to write a list of strings to an NBT list
	private static void writeValuesToNBT(CompoundTag nbt, Iterable<String> values, String key) {
		ListTag nbtList = new ListTag();
		for (String value : values) {
			nbtList.add(StringTag.valueOf(value));
		}
		nbt.put(key, nbtList);
	}

	// helper method to read strings from an NBT list
	private static void readValuesFromNBT(CompoundTag nbt, Consumer<String> values, String key) {
		if (nbt.contains(key)) {
			ListTag nbtList = nbt.getList(key).orElseThrow(() -> new IllegalArgumentException("Expected breeding list: " + key));
            NetworkUtil.boundedLength(nbtList.size(), NetworkUtil.MAX_COLLECTION_SIZE);
			for (Tag stringTag : nbtList) {
                String value = stringTag.asString().orElseThrow(() -> new IllegalArgumentException("Expected breeding identifier"));
                NetworkUtil.boundedLength(value.length(), 1024);
                values.accept(value);
			}
		}
	}

	// serializes mutations to strings
	private static String getMutationString(IMutation<?> mutation) {
		String species0 = mutation.getFirstParent().id().toString();
		String species1 = mutation.getSecondParent().id().toString();
		String resultSpecies = mutation.getResult().id().toString();
		return String.format(MUTATION_FORMAT, species0, species1, resultSpecies);
	}

	@Override
	public void registerMutation(IMutation<?> mutation) {
		String mutationString = getMutationString(mutation);
		if (!this.discoveredMutations.contains(mutationString)) {
            this.discoveredMutations.add(mutationString);
			setDirty();

			ISpeciesType<?, ?> speciesRoot = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(this.typeId);
			ForestryEvent event = new ForestryEvent.MutationDiscovered(speciesRoot, this.username, mutation, this);
			ForestryEvent.post(event);

			sendUpdate(List.of(), List.of(mutationString), List.of());
		}
	}

	@Override
	public boolean isDiscovered(IMutation<?> mutation) {
		String mutationString = getMutationString(mutation);
		return this.discoveredMutations.contains(mutationString) || this.researchedMutations.contains(mutationString);
	}

	@Override
	public boolean isDiscovered(ISpecies<?> species) {
		return this.discoveredSpecies.contains(species.id());
	}

	@Override
	public Set<Identifier> getDiscoveredSpecies() {
		return this.discoveredSpecies;
	}

	@Override
	public int getSpeciesBred() {
		return this.discoveredSpecies.size();
	}

	@Override
	public void registerBirth(ISpecies<?> species) {
		registerSpecies(species);
	}

	@Override
	public void registerSpecies(ISpecies<?> species) {
		Identifier speciesId = species.id();

		discover(species);

		if (!this.discoveredSpecies.contains(speciesId)) {
            this.discoveredSpecies.add(speciesId);
			setDirty();

			ISpeciesType<?, ?> speciesType = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(this.typeId);
			ForestryEvent event = new ForestryEvent.SpeciesDiscovered(speciesType, this.username, species, this);
			ForestryEvent.post(event);

			sendUpdate(List.of(speciesId), List.of(), List.of());
		}
	}

	@Override
	public void researchMutation(IMutation<?> mutation) {
		String mutationString = getMutationString(mutation);
		if (!this.researchedMutations.contains(mutationString)) {
            this.researchedMutations.add(mutationString);
			setDirty();

			registerMutation(mutation);

			sendUpdate(List.of(), List.of(), List.of(mutationString));
		}
	}

	@Override
	public boolean isResearched(IMutation<?> mutation) {
		String mutationString = getMutationString(mutation);
		return this.researchedMutations.contains(mutationString);
	}

	/**
	 * Used purely for tracking advancements.
	 *
	 * @param species The species that was discovered
	 */
	public void discover(ISpecies<?> species) {
		// A tracker read back from disk has neither until the player who owns it logs in
		if (this.level == null || this.username == null) {
			return;
		}

		ForestryAdvancementTriggers.DISCOVER_SPECIES.trigger(this.level, this.username, species.id());
		registerProgress(this.level, this.username, species);
	}

	/**
	 * Called when a species is discovered, after the discovery itself is reported. The base
	 * implementation does nothing, for the species types that track no research progress.
	 *
	 * @param level   The level the discovering player is in
	 * @param profile The profile of the discovering player
	 * @param species The species that was discovered
	 */
	// Deviation from 1.20.1: abstract there, where every tracker lived in one jar. A content jar's
	// tracker now compiles on its own, so this is a hook with a do-nothing default
	public void registerProgress(Level level, GameProfile profile, ISpecies<?> species) {
	}
}
