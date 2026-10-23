package forestry.lifecycle;

import forestry.apiculture.features.ApicultureItems;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.wood.WoodAccess;
import forestry.core.features.CoreDataComponents;
import forestry.core.features.FluidsItems;
import forestry.core.platform.item.FluidContainerType;
import forestry.core.platform.tab.ForestryCreativeTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.List;

/** Checks actual tab generators and event contributions, without replacing their output collector. */
final class CreativeTabChecks {
	static void checkContents() {
		for (String name : List.of("forestry", "building_blocks", "apiculture", "arboriculture", "lepidopterology", "agriculture", "storage", "mail")) {
			var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(ForestryCreativeTabs.tabKey(name));
			if (tab.getDisplayItems().isEmpty() || tab.getIconItem().isEmpty()) {
				throw new AssertionError("Empty Forestry creative tab: " + name);
			}
		}
		var bees = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(ForestryCreativeTabs.tabKey("apiculture"));
		if (bees.getDisplayItems().stream().filter(stack -> stack.is(ApicultureItems.EXPERIENCE_DROP.item())).count() != 1) {
			throw new AssertionError("Experience drop must appear exactly once in apiculture");
		}
		var storage = ForestryCreativeTabs.STORAGE.creativeTab();
		for (String name : List.of("building_blocks", "arboriculture")) {
			var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(ForestryCreativeTabs.tabKey(name));
			for (var wood : WoodAccess.INSTANCE.getRegisteredWoodTypes()) {
				for (var kind : List.of(WoodBlockKind.DOOR, WoodBlockKind.TRAPDOOR, WoodBlockKind.BUTTON, WoodBlockKind.PRESSURE_PLATE)) {
					var item = WoodAccess.INSTANCE.getStack(wood, kind, false).getItem();
					if (tab.getDisplayItems().stream().filter(stack -> stack.is(item)).count() != 1) {
						throw new AssertionError("Shared wood shape must appear once: " + name + "/" + wood + "/" + kind);
					}
				}
				for (boolean fireproof : new boolean[]{false, true}) {
					if (!tab.contains(WoodAccess.INSTANCE.getStack(wood, WoodBlockKind.LOG, fireproof))) {
						throw new AssertionError("Missing wood log variant: " + name + "/" + wood + "/" + fireproof);
					}
				}
			}
		}
		for (var type : FluidContainerType.values()) {
			var item = FluidsItems.CONTAINERS.get(type).item();
			long empty = storage.getDisplayItems().stream().filter(stack -> stack.is(item)
				&& stack.getOrDefault(CoreDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.EMPTY).isEmpty()).count();
			long water = storage.getDisplayItems().stream().filter(stack -> stack.is(item)).filter(stack -> {
				var fluid = stack.getOrDefault(CoreDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.EMPTY);
				return fluid.is(Fluids.WATER) && fluid.getAmount() == FluidType.BUCKET_VOLUME;
			}).count();
			if (empty != 1 || water != 1) throw new AssertionError("Missing empty/filled creative container: " + type);
		}
		if (!CreativeModeTabs.searchTab().contains(ApicultureItems.EXPERIENCE_DROP.stack())) {
			throw new AssertionError("Experience drop missing from creative search");
		}
	}
}
