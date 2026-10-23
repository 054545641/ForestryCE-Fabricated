package forestry.core.platform.inventory;

import forestry.api.ForestryTags;
import forestry.api.core.ForestryError;
import forestry.api.core.IError;
import forestry.api.core.IErrorSource;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.util.GeneticsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class PortableAnalyzerInventory extends ItemInventory implements IErrorSource {
	public static final int SLOT_ENERGY = 0;
	public static final int SLOT_SPECIMEN = 1;
	public static final int SLOT_ANALYZE_1 = 2;
	public static final int SLOT_ANALYZE_2 = 3;
	public static final int SLOT_ANALYZE_3 = 4;
	public static final int SLOT_ANALYZE_4 = 5;
	public static final int SLOT_ANALYZE_5 = 6;

	public PortableAnalyzerInventory(Player player, ItemStack itemstack) {
		super(player, 7, itemstack);
		updateChargesComponent();
	}

	@Override
	protected void writeInventoryToParent(ItemStack parent) {
		super.writeInventoryToParent(parent);
		updateChargesComponent(parent);
	}

	public static boolean isAlyzingFuel(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		return stack.is(ForestryTags.Items.DROP_HONEY);
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_ENERGY) {
			return isAlyzingFuel(stack);
		}

		// only allow one slot to be used at a time
		if (hasSpecimen() && getItem(slotIndex).isEmpty()) {
			return false;
		}

		stack = GeneticsUtil.convertToGeneticEquivalent(stack);
		return IIndividualHandlerItem.filter(stack, individual -> {
			if (slotIndex == SLOT_SPECIMEN) {
				return true;
			} else {
				return individual.isAnalyzed();
			}
		});
	}

	@Override
	public void setItem(int index, ItemStack itemStack) {
		super.setItem(index, itemStack);
		if (index == SLOT_SPECIMEN) {
			analyzeSpecimen(itemStack);
		} else if (index == SLOT_ENERGY) {
			analyzeSpecimen(getItem(SLOT_SPECIMEN));
		}
		if (index == SLOT_ENERGY) {
			updateChargesComponent();
		}
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		ItemStack removed = super.removeItem(index, count);
		if (index == SLOT_ENERGY) {
			updateChargesComponent();
		}
		return removed;
	}

	private void analyzeSpecimen(ItemStack specimen) {
		if (specimen.isEmpty()) {
			return;
		}

		ItemStack convertedSpecimen = GeneticsUtil.convertToGeneticEquivalent(specimen);
		if (!ItemStack.matches(specimen, convertedSpecimen)) {
			setItem(SLOT_SPECIMEN, convertedSpecimen);
			specimen = convertedSpecimen;
		}

		IIndividual individual = IIndividualHandlerItem.getIndividual(specimen);

		// Analyze if necessary
		if (individual != null) {
			if (!individual.isAnalyzed()) {
				// Requires energy
				if (!isAlyzingFuel(getItem(SLOT_ENERGY))) {
					return;
				}

				if (individual.analyze()) {
					IBreedingTracker breedingTracker = individual.getType().getBreedingTracker(this.player.level(), this.player.getGameProfile());
					breedingTracker.registerSpecies(individual.getSpecies());
					breedingTracker.registerSpecies(individual.getInactiveSpecies());

					individual.saveToStack(specimen);

					// Decrease energy
					removeItem(SLOT_ENERGY, 1);
					updateChargesComponent();
				}
			}
		}

		setItem(SLOT_ANALYZE_1, specimen);
		setItem(SLOT_SPECIMEN, ItemStack.EMPTY);
	}

	@Override
	public Set<IError> getErrors() {
		ItemStack specimen = getSpecimen();

		if (specimen.isEmpty()) {
			return Set.of(ForestryError.NO_SPECIMEN);
		} else {
			if (IIndividualHandlerItem.isIndividual(specimen) && !isAlyzingFuel(getItem(SLOT_ENERGY))) {
				return Set.of(ForestryError.NO_HONEY);
			}
		}

		return Set.of();
	}

	public ItemStack getSpecimen() {
		for (int i = SLOT_SPECIMEN; i <= SLOT_ANALYZE_5; i++) {
			ItemStack itemStack = getItem(i);
			if (!itemStack.isEmpty()) {
				return itemStack;
			}
		}
		return ItemStack.EMPTY;
	}

	public boolean hasSpecimen() {
		return !getSpecimen().isEmpty();
	}

	private void updateChargesComponent() {
		updateChargesComponent(getParent());
	}

	private void updateChargesComponent(ItemStack parent) {
		ItemStack energy = getItem(PortableAnalyzerInventory.SLOT_ENERGY);
		int amount = 0;
		if (!energy.isEmpty()) {
			amount = energy.getCount();
		}
		if (amount > 0) {
			parent.set(CoreDataComponents.ALYZER_CHARGES.get(), amount);
		} else {
			parent.remove(CoreDataComponents.ALYZER_CHARGES.get());
		}
	}
}
