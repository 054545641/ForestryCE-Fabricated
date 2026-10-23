package forestry.gametest;

import forestry.api.ForestryConstants;
import forestry.core.platform.multiblock.MultiblockController;
import forestry.core.platform.multiblock.MultiblockIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Regression coverage for concurrent client and server access to {@link MultiblockIndex}.
 *
 * <p>The integrated server and the client tick their own {@code Level} on separate threads, and both sides
 * reach the same static index. One side registers a controller while the other registers or unloads its
 * own level. A plain {@code HashMap} throws {@code ConcurrentModificationException} out of
 * {@code computeIfAbsent} under that interleave, and silently loses or duplicates entries when it does not.
 *
 * <p>The test races the two production paths that structurally modify the outer level map: the
 * {@code computeIfAbsent} in {@code MultiblockIndex.mapFor}, reached by every {@code register}, and
 * {@link MultiblockIndex#clear}, reached by the level unload event. Every iteration re-enters the insert
 * path so every iteration races. Inserting once and then looping leaves the key present, and a present key
 * returns before the guard that throws, so all but the first iteration would be dead.
 *
 * <p>The test asserts behavior, not the map class. Any correct fix satisfies it.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultiblockIndexConcurrencyTest {
	private static final int WORKERS = 16;
	private static final int ITERATIONS = 1_000;

	private MultiblockIndexConcurrencyTest() {
	}

	@GameTest(template = "empty", timeoutTicks = 200)
	public static void outerLevelIndexSurvivesConcurrentClientAndServerLoading(GameTestHelper helper) throws Exception {
		Map<LevelAccessor, Map<BlockPos, MultiblockController>> levels = outerLevelIndex();

		// One key per worker, since each side owns a distinct Level. Proxies are identity-keyed, so they
		// never collide with a real level
		List<LevelAccessor> testLevels = new ArrayList<>(WORKERS);
		for (int i = 0; i < WORKERS; i++) {
			testLevels.add(testLevel("multiblock-index-concurrency-" + i));
		}

		ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
		CountDownLatch ready = new CountDownLatch(WORKERS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>(WORKERS);
		try {
			for (LevelAccessor level : testLevels) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					if (!start.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("Timed out waiting for the concurrent burst to start");
					}
					for (int iteration = 0; iteration < ITERATIONS; iteration++) {
						// mapFor, the call that crashed
						levels.computeIfAbsent(level, ignored -> new HashMap<>());
						// the level unload path
						MultiblockIndex.clear(level);
					}
					return null;
				}));
			}

			if (!ready.await(5, TimeUnit.SECONDS)) {
				helper.fail("Worker threads did not become ready for the MultiblockIndex concurrency test");
				return;
			}
			start.countDown();

			for (Future<?> future : futures) {
				try {
					future.get(10, TimeUnit.SECONDS);
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					helper.fail("Concurrent access to the MultiblockIndex level map threw "
							+ cause.getClass().getSimpleName() + ": " + cause.getMessage());
					return;
				}
			}

			// A racing resize can lose a remove without throwing. Every worker cleared its own key last, so
			// nothing of ours may survive
			for (LevelAccessor level : testLevels) {
				if (levels.containsKey(level)) {
					helper.fail("MultiblockIndex level map silently kept a cleared level (" + level
							+ "), so concurrent access corrupted it without throwing");
					return;
				}
			}
		} finally {
			start.countDown();
			executor.shutdownNow();
			executor.awaitTermination(5, TimeUnit.SECONDS);
			for (LevelAccessor level : testLevels) {
				levels.remove(level);
			}
		}

		helper.succeed();
	}

	// Reflection because the public API takes Level, which has no cheap stand-in, and the race needs one
	// distinct level key per thread
	@SuppressWarnings("unchecked")
	private static Map<LevelAccessor, Map<BlockPos, MultiblockController>> outerLevelIndex() throws Exception {
		Field field = MultiblockIndex.class.getDeclaredField("LEVELS");
		field.setAccessible(true);
		return (Map<LevelAccessor, Map<BlockPos, MultiblockController>>) field.get(null);
	}

	private static LevelAccessor testLevel(String name) {
		return (LevelAccessor) Proxy.newProxyInstance(
			MultiblockIndexConcurrencyTest.class.getClassLoader(),
			new Class<?>[]{LevelAccessor.class},
			(proxy, method, args) -> switch (method.getName()) {
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == args[0];
				case "toString" -> name;
				default -> defaultValue(method.getReturnType());
			});
	}

	private static Object defaultValue(Class<?> type) {
		if (!type.isPrimitive()) return null;
		if (type == boolean.class) return false;
		if (type == char.class) return '\0';
		if (type == byte.class) return (byte) 0;
		if (type == short.class) return (short) 0;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == float.class) return 0.0F;
		if (type == double.class) return 0.0D;
		throw new IllegalArgumentException("Unsupported primitive type: " + type);
	}
}
