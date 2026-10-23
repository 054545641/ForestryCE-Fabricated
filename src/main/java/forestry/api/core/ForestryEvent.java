package forestry.api.core;

import com.mojang.authlib.GameProfile;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.event.EventFactory;
import org.jetbrains.annotations.ApiStatus;

// todo move to forestry.api.event
public abstract class ForestryEvent {
    @FunctionalInterface public interface Listener { void onEvent(ForestryEvent event); }
    public static final net.fabricmc.fabric.api.event.Event<Listener> EVENTS = EventFactory.createArrayBacked(Listener.class,
        listeners -> event -> { for (Listener listener : listeners) listener.onEvent(event); });
    public static void post(ForestryEvent event) { EVENTS.invoker().onEvent(event); }

    /** Fired after a Forestry worktable crafts an item, alongside vanilla crafting callbacks. */
    public static final class ItemCrafted extends ForestryEvent {
        public final Player player;
        public final net.minecraft.world.item.ItemStack stack;
        public final net.minecraft.world.Container ingredients;
        public ItemCrafted(Player player, net.minecraft.world.item.ItemStack stack, net.minecraft.world.Container ingredients) {
            this.player = player;
            this.stack = stack;
            this.ingredients = ingredients;
        }
    }
	/**
	 * Supertype for events concerning breeding and mutation.
	 */
	@ApiStatus.Internal
	private static abstract class BreedingEvent extends ForestryEvent {
		public final ISpeciesType<?, ?> root;
		public final IBreedingTracker tracker;
		public final GameProfile username;

		private BreedingEvent(ISpeciesType<?, ?> root, GameProfile username, IBreedingTracker tracker) {
			this.root = root;
			this.username = username;
			this.tracker = tracker;
		}
	}

	/**
	 * Fired before a queen is created as a result of breeding a princess and a drone.
	 *
	 * @since 2.4.4
	 */
	public static class BeeMatingEvent extends ForestryEvent {
        private boolean canceled;
        public boolean isCanceled() { return canceled; }
        public void setCanceled(boolean canceled) { this.canceled = canceled; }
		private final IBeeHousing housing;
		private IBee princess;
		private final IBee drone;

		public BeeMatingEvent(IBeeHousing housing, IBee princess, IBee drone) {
			this.housing = housing;
			this.princess = princess;
			this.drone = drone;
		}

		public IBeeHousing getHousing() {
			return this.housing;
		}

		public IBee getPrincess() {
			return this.princess;
		}

		/**
		 * Used to override the princess individual, which will become the queen individual.
		 *
		 * @param princess The new princess individual to replace the queen with.
		 */
		public void setPrincess(IBee princess) {
			this.princess = princess;
		}

		public IBee getDrone() {
			return this.drone;
		}
	}

	public static class SpeciesDiscovered extends BreedingEvent {
		public final ISpecies<?> species;

		public SpeciesDiscovered(ISpeciesType<?, ?> root, GameProfile username, ISpecies<?> species, IBreedingTracker tracker) {
			super(root, username, tracker);
			this.species = species;
		}
	}

	public static class MutationDiscovered extends BreedingEvent {
		public final IMutation<?> allele;

		public MutationDiscovered(ISpeciesType<?, ?> root, GameProfile username, IMutation<?> allele, IBreedingTracker tracker) {
			super(root, username, tracker);
			this.allele = allele;
		}
	}

	public static class SyncedBreedingTracker extends ForestryEvent {
		public final IBreedingTracker tracker;
		public final Player player;

		public SyncedBreedingTracker(IBreedingTracker tracker, Player player) {
			this.tracker = tracker;
			this.player = player;
		}
	}
}
