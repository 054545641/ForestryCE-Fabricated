package forestry.core.content.backpacks;

import forestry.api.core.backpacks.IBackpackDefinition;
import forestry.core.platform.util.NBTUtilForestry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class BackpackDefinition implements IBackpackDefinition {
	private final int primaryColor;
	private final int secondaryColor;
	private final Predicate<ItemStack> filter;

	public BackpackDefinition(int primaryColor, int secondaryColor, Predicate<ItemStack> filter) {
		this.primaryColor = primaryColor;
		this.secondaryColor = secondaryColor;
		this.filter = filter;
	}

	@Override
	public Predicate<ItemStack> getFilter() {
		return this.filter;
	}

	@Override
	public Component getName(ItemStack backpack) {
		Component customName = backpack.get(DataComponents.CUSTOM_NAME);
		if (customName != null) {
			return customName;
		}
		Item item = backpack.getItem();
		Component display = Component.translatable(item.getDescriptionId());

		CompoundTag tagCompound = NBTUtilForestry.getItemStackTag(backpack);
		if (tagCompound != null && tagCompound.getCompound("display").isPresent()) {
			CompoundTag nbt = tagCompound.getCompoundOrEmpty("display");

			if (nbt.getString("Name").isPresent()) {
				display = Component.literal(nbt.getStringOr("Name", ""));
			}
		}

		return display;
	}

	@Override
	public int getPrimaryColour() {
		return this.primaryColor;
	}

	@Override
	public int getSecondaryColour() {
		return this.secondaryColor;
	}
}
