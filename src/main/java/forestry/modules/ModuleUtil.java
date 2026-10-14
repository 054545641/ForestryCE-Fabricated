package forestry.modules;

import forestry.Forestry;
import forestry.core.platform.registration.FeatureProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.util.TreeSet;
import java.util.function.Consumer;

public class ModuleUtil {

	public static void loadFeatureProviders() {
		forEachAnnotated(Type.getType(FeatureProvider.class), klass -> Forestry.LOGGER.debug("Loaded feature provider {}", klass));
	}

	static void forEachAnnotated(Type annotationType, Consumer<Class<?>> action) {
		TreeSet<String> classes = new TreeSet<>();
		for (ModFileScanData scanData : ModList.get().getAllScanData()) {
			for (ModFileScanData.AnnotationData data : scanData.getAnnotations()) {
				if (data.annotationType().equals(annotationType)) {
					classes.add(data.clazz().getClassName());
				}
			}
		}
		for (String className : classes) {
			try {
				action.accept(Class.forName(className));
			} catch (ReflectiveOperationException | ExceptionInInitializerError e) {
				throw new RuntimeException("Failed to load annotated class " + className, e);
			}
		}
	}

	public static IEventBus getModBus(String modid) {
		ModContainer container = ModList.get().getModContainerById(modid)
			.orElseThrow(() -> new IllegalStateException("Missing mod container for " + modid));
		IEventBus bus = container.getEventBus();
		if (bus == null) {
			throw new IllegalStateException("Mod container has no event bus: " + modid);
		}
		return bus;
	}
}
