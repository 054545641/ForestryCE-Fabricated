package forestry.core.platform.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.item.FluidContainerItem;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.stream.Stream;

/** A filled Forestry capsule, decoded without constructing fluid or item stacks during reload. */
public record FluidContainerIngredient(Holder<Item> item, FluidStackTemplate fluid) implements ICustomIngredient {

	public static final MapCodec<FluidContainerIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Item.CODEC.fieldOf("item").forGetter(FluidContainerIngredient::item),
		FluidStackTemplate.CODEC.fieldOf("fluid").forGetter(FluidContainerIngredient::fluid)
	).apply(i, FluidContainerIngredient::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidContainerIngredient> STREAM_CODEC = StreamCodec.composite(
		Item.STREAM_CODEC, FluidContainerIngredient::item,
		FluidStackTemplate.STREAM_CODEC, FluidContainerIngredient::fluid, FluidContainerIngredient::new);
	public static final IngredientType<FluidContainerIngredient> TYPE = new IngredientType<>(CODEC, STREAM_CODEC);

	@Override
	public boolean test(ItemStack stack) {
		if (!stack.is(this.item)) return false;
		FluidStack contained = FluidContainerItem.getContained(stack);
		return contained.getAmount() == this.fluid.amount()
			&& FluidStack.isSameFluidSameComponents(contained, this.fluid.create());
	}

	@Override
	public Stream<Holder<Item>> items() { return Stream.of(this.item); }

	@Override
	public boolean isSimple() { return false; }

	@Override
	public IngredientType<?> getType() { return TYPE; }

	@Override
	public SlotDisplay display() { return new Display(this); }

	public ItemStack createDisplayStack() {
		ItemStack stack = new ItemStack(this.item);
		stack.set(CoreDataComponents.FLUID_CONTENT, SimpleFluidContent.copyOf(this.fluid.create()));
		return stack;
	}

	/** Recipe displays are also built before components bind. Resolve the filled stack only when displayed. */
	public record Display(FluidContainerIngredient ingredient) implements SlotDisplay {
		public static final SlotDisplay.Type<Display> TYPE = new SlotDisplay.Type<>(
			FluidContainerIngredient.CODEC.xmap(Display::new, Display::ingredient), FluidContainerIngredient.STREAM_CODEC.map(Display::new, Display::ingredient));

		@Override
		public SlotDisplay.Type<Display> type() { return TYPE; }

		@Override
		public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
			return factory instanceof DisplayContentsFactory.ForStacks<T> stacks
				? Stream.of(stacks.forStack(this.ingredient.createDisplayStack())) : Stream.empty();
		}
	}
}
