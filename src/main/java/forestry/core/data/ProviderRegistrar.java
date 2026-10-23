package forestry.core.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;

@FunctionalInterface
public interface ProviderRegistrar {
	void addProvider(DataGenerator generator, boolean run, DataProvider provider);
}
