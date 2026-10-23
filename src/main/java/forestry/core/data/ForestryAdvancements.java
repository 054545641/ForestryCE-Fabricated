package forestry.core.data;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import forestry.api.ForestryConstants;

/**
 * The shape every Forestry advancement below the root is built from. One parent, one icon, and the
 * pair of lang keys the id names, so a jar's provider is a list of what each advancement asks for
 * rather than the same nine lines fifty-odd times.
 */
public final class ForestryAdvancements {
	private ForestryAdvancements() {
	}

	/**
	 * Adds one advancement that a single criterion earns, drawn with a plain task frame.
	 *
	 * @param writer    The sink the provider writes to
	 * @param name      The advancement id's path, which also names its two lang keys
	 * @param icon      The stack the advancement tree draws it with
	 * @param parent    The advancement it hangs under
	 * @param criterion The criterion that earns it
	 * @return The advancement, to hang a child under
	 */
	public static AdvancementHolder add(Consumer<AdvancementHolder> writer, String name, ItemStack icon, AdvancementHolder parent, Criterion<?> criterion) {
		return add(writer, name, icon, parent, criterion, AdvancementType.TASK, false);
	}

	/**
	 * Adds one advancement that a single criterion earns.
	 *
	 * @param writer    The sink the provider writes to
	 * @param name      The advancement id's path, which also names its two lang keys
	 * @param icon      The stack the advancement tree draws it with
	 * @param parent    The advancement it hangs under
	 * @param criterion The criterion that earns it
	 * @param type      The frame the advancement tree draws around the icon
	 * @param hidden    Whether the tree shows the advancement before it is earned
	 * @return The advancement, to hang a child under
	 */
	public static AdvancementHolder add(Consumer<AdvancementHolder> writer, String name, ItemStack icon, AdvancementHolder parent, Criterion<?> criterion, AdvancementType type, boolean hidden) {
		return save(writer, name, display(name, icon, parent, type, hidden)
			.addCriterion(ForestryConstants.forestry(name).toString(), criterion));
	}

	/**
	 * Used to start an advancement that more than one criterion earns. The caller adds the criteria
	 * and hands the builder back to {@link #save}.
	 *
	 * @param name   The advancement id's path, which also names its two lang keys
	 * @param icon   The stack the advancement tree draws it with
	 * @param parent The advancement it hangs under
	 * @param type   The frame the advancement tree draws around the icon
	 * @param hidden Whether the tree shows the advancement before it is earned
	 * @return The builder, with everything but its criteria set
	 */
	public static Advancement.Builder display(String name, ItemStack icon, AdvancementHolder parent, AdvancementType type, boolean hidden) {
		return Advancement.Builder.advancement()
			.parent(parent)
			.display(
				net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(icon),
				Component.translatable("advancements.forestry." + name + ".title"),
				Component.translatable("advancements.forestry." + name + ".description"),
				null,
				type,
				true,
				true,
				hidden
			);
	}

	/**
	 * Adds an advancement started by {@link #display}.
	 *
	 * @param writer  The sink the provider writes to
	 * @param name    The advancement id's path
	 * @param builder The builder to write out
	 * @return The advancement, to hang a child under
	 */
	public static AdvancementHolder save(Consumer<AdvancementHolder> writer, String name, Advancement.Builder builder) {
		// Deviation from 1.20.1: save takes the id as a String and parses it, so the id is built here
		// rather than concatenated at every call site
		return builder.save(writer, ForestryConstants.forestry(name).toString());
	}

	/**
	 * Used to name a parent that another jar's provider builds. Only the id reaches the child, so the
	 * advancement this carries is never written and never read.
	 *
	 * @param name The advancement id's path this stands in for
	 * @return A holder of that id, for a parent this jar does not build
	 */
	// Deviation from 1.20.1: a parent is passed as an Advancement in 1.20.1 and as an AdvancementHolder
	// in 1.21. The overload taking a bare id is deprecated for removal, so a holder stands in instead
	public static AdvancementHolder reference(String name) {
		Identifier id = ForestryConstants.forestry(name);
		Advancement empty = new Advancement(Optional.empty(), Optional.empty(), AdvancementRewards.EMPTY, Map.of(), AdvancementRequirements.EMPTY, false);
		return new AdvancementHolder(id, empty);
	}
}
