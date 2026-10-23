package forestry.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.core.features.CoreDataComponents;
import forestry.mail.features.MailDataComponents;

/**
 * Guard for data component registration. A {@code DeferredRegister} that is never bound to the mod
 * event bus registers nothing, and the failure is silent: the holder still exists, items simply never
 * carry the component and their data vanishes on save.
 *
 * <p>This matters most when a component moves between modules, since the new module's registry may
 * not be wired. Nothing else in the suite exercises letters.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class DataComponentRegistrationTest {
	@GameTest(template = "empty")
	public static void everyForestryDataComponentIsRegistered(GameTestHelper helper) {
		List<String> broken = new ArrayList<>();

		check(broken, "letter_data", MailDataComponents.LETTER_DATA.get());
		check(broken, "genome", CoreDataComponents.GENOME.get());
		check(broken, "mate_genome", CoreDataComponents.MATE_GENOME.get());

		if (!broken.isEmpty()) {
			helper.fail(broken.size() + " data component(s) are not registered as expected:\n  "
				+ String.join("\n  ", broken));
			return;
		}
		helper.succeed();
	}

	private static void check(List<String> broken, String path, DataComponentType<?> type) {
		Identifier expected = ForestryConstants.forestry(path);
		Identifier actual = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);

		if (actual == null) {
			broken.add(expected + " resolved to a component that is not in the registry at all");
		} else if (!expected.equals(actual)) {
			broken.add(expected + " is registered under " + actual + " instead");
		}
	}
}
