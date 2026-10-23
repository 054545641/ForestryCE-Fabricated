package forestry.modules;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import java.util.List;

public final class FabricModuleOrderTests {
    @GameTest
    public void orderAndInvalidGraphs(GameTestHelper test) {
        var core = new ModuleLoadOrder.Entry<>("core", List.<String>of(), List.<String>of());
        var mail = new ModuleLoadOrder.Entry<>("mail", List.of("core"), List.<String>of());
        var order = ModuleLoadOrder.resolve(List.of(mail, core), mod -> true, (id, missing) -> { throw new AssertionError(missing); });
        if (!order.equals(List.of("core", "mail"))) throw new AssertionError("Dependency order changed");
        expectFailure(() -> ModuleLoadOrder.resolve(List.of(core, core), mod -> true, (id, missing) -> {}));
        expectFailure(() -> ModuleLoadOrder.resolve(List.of(
            new ModuleLoadOrder.Entry<>("a", List.of("b"), List.<String>of()),
            new ModuleLoadOrder.Entry<>("b", List.of("a"), List.<String>of())), mod -> true, (id, missing) -> {}));
        test.succeed();
    }
    private static void expectFailure(Runnable action) {
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError("Invalid module graph accepted");
    }
}
