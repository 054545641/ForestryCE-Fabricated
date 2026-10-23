package forestry.core.data;

import forestry.api.ForestryConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import top.theillusivec4.curios.api.CuriosDataProvider;

import java.util.concurrent.CompletableFuture;

public class ForestryCuriosProvider extends CuriosDataProvider {
	public ForestryCuriosProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(ForestryConstants.MOD_ID, output, registries);
	}

	@Override
	public void generate(HolderLookup.Provider provider) {
		createSlot("head")
			.addCosmetic(true);

		createEntities("slots")
			.addPlayer()
			.addSlots("head");
	}
}
