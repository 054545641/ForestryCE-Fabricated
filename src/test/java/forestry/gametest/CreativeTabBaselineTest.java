package forestry.gametest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;

/**
 * Golden-master oracle for creative tab contents.
 *
 * <p>Creative tabs are built at runtime from {@code displayItems} callbacks, so neither datagen nor
 * any other test sees them. Losing an entry while moving a tab between modules is silent: no crash,
 * no failing test, just an item missing from a menu.
 *
 * <p>Enumerates tabs out of {@link BuiltInRegistries#CREATIVE_MODE_TAB} rather than naming the
 * holder class, so the tabs can move between modules without touching this test - which is the
 * point, since the refactor it guards does exactly that.
 *
 * <p>Records membership, not order: per tab, each item id with the number of stacks of it. Order is
 * deliberately not covered because {@code BuildCreativeModeTabContentsEvent} appends rather than
 * splices, so a jar contributing to a tab it does not own necessarily lands at the end. The count
 * matters because a tab lists one stack per species - 40-odd stacks of the same bee item - and a
 * dropped species would otherwise be invisible.
 *
 * <p>Run modes (system property {@code forestry.creativeTabBaseline}):
 * <ul>
 *     <li>{@code generate} - writes the live dump to {@code creative-tab-baseline.txt} in the run
 *     directory, to be copied into {@code src/test/resources/forestry/gametest/}.</li>
 *     <li>default (assert) - compares against the committed resource and fails with the first diff.</li>
 * </ul>
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class CreativeTabBaselineTest {
	private static final String BASELINE_RESOURCE = "/forestry/gametest/creative-tab-baseline.txt";
	private static final String OUTPUT_FILE = "creative-tab-baseline.txt";

	@GameTest(template = "empty")
	public static void creativeTabContentsMatchBaseline(GameTestHelper helper) {
		String actual = dumpForestryTabs(helper.getLevel()).strip();

		if ("generate".equalsIgnoreCase(System.getProperty("forestry.creativeTabBaseline"))) {
			try {
				Files.writeString(Path.of(OUTPUT_FILE), actual, StandardCharsets.UTF_8);
				System.out.println("[CreativeTabBaselineTest] Wrote baseline to " + new File(OUTPUT_FILE).getAbsolutePath());
			} catch (IOException e) {
				throw new RuntimeException("Failed to write creative tab baseline", e);
			}
			helper.succeed();
			return;
		}

		String expected = readBaselineResource();
		if (expected == null) {
			helper.fail("Baseline resource " + BASELINE_RESOURCE + " not found on the classpath. "
					+ "Run with -Pforestry.creativeTabBaseline=generate and copy creative-tab-baseline.txt into src/test/resources"
					+ BASELINE_RESOURCE);
			return;
		}

		String expectedNorm = expected.strip();
		if (!expectedNorm.equals(actual)) {
			helper.fail("Creative tab contents changed vs baseline. First diff:\n" + firstDiff(expectedNorm, actual));
			return;
		}

		helper.succeed();
	}

	private static String dumpForestryTabs(ServerLevel level) {
		var params = new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess());
		List<String> lines = new ArrayList<>();

		List<Identifier> tabIds = BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream()
				.filter(id -> ForestryConstants.MOD_ID.equals(id.getNamespace()))
				.sorted()
				.toList();

		for (Identifier tabId : tabIds) {
			CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(tabId);
			if (tab == null) {
				lines.add(tabId + " !! not resolvable from the registry");
				continue;
			}
			tab.buildContents(params);

			Map<String, Integer> counts = new LinkedHashMap<>();
			for (ItemStack stack : tab.getDisplayItems()) {
				String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
				counts.merge(itemId, 1, Integer::sum);
			}

			counts.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(e -> lines.add(tabId + " " + e.getKey() + " x" + e.getValue()));
		}

		return lines.stream().collect(Collectors.joining("\n"));
	}

	private static String readBaselineResource() {
		try (InputStream in = CreativeTabBaselineTest.class.getResourceAsStream(BASELINE_RESOURCE)) {
			if (in == null) {
				return null;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				return reader.lines().collect(Collectors.joining("\n"));
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read creative tab baseline", e);
		}
	}

	private static String firstDiff(String expected, String actual) {
		List<String> a = List.of(expected.split("\n", -1));
		List<String> b = List.of(actual.split("\n", -1));

		for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
			String left = i < a.size() ? a.get(i) : "<missing>";
			String right = i < b.size() ? b.get(i) : "<missing>";
			if (!left.equals(right)) {
				return "  line " + (i + 1) + "\n    baseline: " + left + "\n    actual:   " + right
						+ "\n  (" + a.size() + " baseline lines, " + b.size() + " actual)";
			}
		}
		return "  no line differs, but the strings are not equal";
	}
}
