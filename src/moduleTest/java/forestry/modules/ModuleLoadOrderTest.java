package forestry.modules;

import java.util.ArrayList;
import java.util.List;

public class ModuleLoadOrderTest {
	public static void main(String[] args) {
		var core = entry("core");
		var bees = entry("bees", "core");
		var trees = entry("trees", "core");
		var butterflies = entry("butterflies", "bees", "trees");
		check(List.of("core", "bees", "trees", "butterflies"),
			resolve(List.of(butterflies, core, bees, trees)), "dependency order");
		check(List.of("core", "trees", "bees"), resolve(List.of(core, trees, bees)), "stable independent order");
		check(List.of(), resolve(List.of()), "empty installation");
		check(List.of("core"), resolve(List.of(entry("addon", "absent"), core)), "missing module");

		var integration = new ModuleLoadOrder.Entry<>("integration", List.of("core"), List.of("external"));
		var skipped = new ArrayList<String>();
		var chain = List.of(entry("dependent", "integration"), integration, core);
		check(List.of("core"), ModuleLoadOrder.resolve(chain, id -> false, (id, reason) -> skipped.add(id)), "missing mod cascade");
		check(List.of("integration", "dependent"), skipped, "each excluded module reported once");
		check(List.of("core", "integration", "dependent"), resolve(chain), "installed optional dependency");

		expectFailure("Duplicate", List.of(core, core));
		expectFailure("Cyclic", List.of(entry("a", "b"), entry("b", "a")));
		expectFailure("Cyclic", List.of(entry("self", "self")));
		System.out.println("Module load order: 10 checks passed");
	}

	private static ModuleLoadOrder.Entry<String> entry(String id, String... dependencies) {
		return new ModuleLoadOrder.Entry<>(id, List.of(dependencies), List.of());
	}

	private static List<String> resolve(List<ModuleLoadOrder.Entry<String>> entries) {
		return ModuleLoadOrder.resolve(entries, id -> true, (id, reason) -> {});
	}

	private static void expectFailure(String message, List<ModuleLoadOrder.Entry<String>> entries) {
		try {
			resolve(entries);
		} catch (IllegalStateException exception) {
			if (exception.getMessage().contains(message)) {
				return;
			}
			throw new AssertionError("Unexpected failure", exception);
		}
		throw new AssertionError("Expected " + message + " failure");
	}

	private static void check(Object expected, Object actual, String scenario) {
		if (!expected.equals(actual)) {
			throw new AssertionError(scenario + ": expected " + expected + ", got " + actual);
		}
	}
}
