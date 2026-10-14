package forestry.core.engine.genetics;

import forestry.Forestry;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IIndividualItem;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.core.genetics.IIndividualLiving;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.util.GeneticsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class ItemGE extends ItemForestry implements IIndividualItem {
	@Override
	public ILifeStage getLifeStage() {
		return this.stage;
	}

	@Override
	public ISpeciesType<?, ?> getSpeciesType() {
		return getType();
	}

	protected final ILifeStage stage;

	protected ItemGE(Item.Properties properties, ILifeStage stage) {
		super(properties.setNoCombineRepair());

		this.stage = stage;
	}

	protected abstract ISpecies<?> getSpecies(ItemStack stack);

	public abstract ISpeciesType<?, ?> getType();

	@Nullable
	public static IIndividual getIndividual(ItemStack stack) {
		return stack.getItem() instanceof ItemGE item ? item.getIndividualFromComponent(stack) : null;
	}

	@Nullable
	public static IGenome getGenome(ItemStack stack) {
		return stack.get(CoreDataComponents.GENOME);
	}

	@Nullable
	public static ILifeStage getLifeStage(ItemStack stack) {
		return stack.getItem() instanceof ItemGE item ? item.stage : null;
	}

	@Nullable
	public static ISpeciesType<?, ?> getSpeciesType(ItemStack stack) {
		return stack.getItem() instanceof ItemGE item ? item.getType() : null;
	}

	public IIndividual getIndividualFromComponent(ItemStack stack) {
		IGenome genome = getGenome(stack);
		ISpeciesType<?, ?> type = getType();

		if (genome == null) {
			return type.getDefaultSpecies().createIndividual();
		}
		if (genome.getKaryotype() != type.getKaryotype()) {
			return type.getDefaultSpecies().createIndividual();
		}

		IIndividual individual = genome.getActiveSpecies().createIndividual(genome);
		if (individual instanceof Individual<?, ?, ?> forestryIndividual) {
			forestryIndividual.loadPropertiesFromStack(stack);
		} else {
			individual.setMate(stack.get(CoreDataComponents.MATE_GENOME));
			if (stack.getOrDefault(CoreDataComponents.ANALYZED, Boolean.FALSE)) {
				individual.analyze();
			}
			if (individual instanceof IIndividualLiving living) {
				Integer health = stack.get(CoreDataComponents.HEALTH);
				if (health != null) {
					living.setHealth(health);
				}
			}
		}
		return individual;
	}

	public static boolean hasIndividual(ItemStack stack) {
		return IIndividualHandlerItem.hasIndividual(stack);
	}

	public static boolean isIndividual(ItemStack stack) {
		return IIndividualHandlerItem.isIndividual(stack);
	}

	public static void ifPresent(ItemStack stack, Consumer<IIndividual> action) {
		IIndividualHandlerItem.ifPresent(stack, action);
	}

	public static void ifPresent(ItemStack stack, BiConsumer<IIndividual, ILifeStage> action) {
		IIndividualHandlerItem.ifPresent(stack, action);
	}

	public static boolean filter(ItemStack stack, Predicate<IIndividual> predicate) {
		return IIndividualHandlerItem.filter(stack, predicate);
	}

	public static boolean filter(ItemStack stack, BiPredicate<IIndividual, ILifeStage> predicate) {
		return IIndividualHandlerItem.filter(stack, predicate);
	}

	@SuppressWarnings("unchecked")
	public static <S extends ISpecies<?>> S getSpecies(ItemStack stack, ISpeciesType<S, ?> type) {
		IIndividual individual = getIndividual(stack);
		return individual != null ? (S) individual.getSpecies() : type.getDefaultSpecies();
	}

	@Override
	public Component getName(ItemStack stack) {
		IIndividual individual = getIndividual(stack);
		ILifeStage lifeStage = getLifeStage(stack);
		return individual != null && lifeStage != null ? GeneticsUtil.getItemName(lifeStage, individual.getSpecies()) : super.getName(stack);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		if (!hasIndividual(stack)) { // villager trade wildcard bees
			return false;
		}
		ISpecies<?> species = getSpecies(stack);
		return species.hasGlint() && ForestryConfig.CLIENT.enableGlints.get();
	}

	public static void appendGeneticsTooltip(ItemStack stack, List<Component> tooltip) {
		if (!hasIndividual(stack)) {
			return;
		}

		MutableBoolean analyzed = new MutableBoolean();
		IIndividual individual = getIndividual(stack);
		if (individual != null) {
			if (individual.isAnalyzed()) {
				if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
					((ISpecies<IIndividual>) individual.getSpecies()).addTooltip(individual, tooltip);
				} else {
					tooltip.add(Component.translatable("for.gui.tooltip.tmi", "< %s >").withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
				}

				analyzed.setTrue();
			}
		}
		if (analyzed.isFalse()) {
			tooltip.add(Component.translatable("for.gui.unknown", "< %s >").withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		appendGeneticsTooltip(stack, tooltip);
	}

	@Override
	public String getCreatorModId(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack) {
		ISpecies<?> species = getSpecies(stack);
		return species.id().getNamespace();
	}

	public static <S extends ISpecies<I>, I extends IIndividual> void addCreativeItems(ILifeStage stage, List<ItemStack> subItems, boolean hideSecrets, ISpeciesType<S, I> type) {
		for (S species : type.getAllSpecies()) {
			// Don't show secrets unless ordered to.
			if (hideSecrets && species.isSecret() && !Forestry.DEBUG) {
				continue;
			}

			subItems.add(species.createStack(species.createIndividual(), stage));
		}
	}
}
