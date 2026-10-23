package forestry.core.platform.client;

import forestry.core.platform.client.ClientBreedingHandler;
import forestry.core.engine.genetics.root.ServerBreedingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

// Rule of thumb for safely calling client code: client classes must be in the body of a static method
// in another class (like this one), guarded by an if statement checking FMLEnvironment.getDist() == Dist.CLIENT
// Calls to this class must be guarded by an if statement.
// DistExecutor will be deprecated for removal in 1.20, and it doesn't work anyway.
public class ClientsideCode {
    public static int getLineHeight(int maxWidth, net.minecraft.network.chat.FormattedText... strings) {
        var font = Minecraft.getInstance().font;
        int lineCount = 0;
        for (var text : strings) lineCount += font.split(text, maxWidth).size();
        return lineCount * font.lineHeight;
    }
    public static boolean isFuel(net.minecraft.world.item.ItemStack stack) {
        var level = Minecraft.getInstance().level;
        return level != null && level.fuelValues().isFuel(stack);
    }
	public static int getFluidColor(Fluid fluid) {
		var model = net.minecraft.client.Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
		int color = model.tintSource() == null ? -1 : model.tintSource().color(fluid.defaultFluidState().createLegacyBlock());
		forestry.core.platform.fluids.ForestryFluids definition = forestry.core.platform.fluids.ForestryFluids.getFluidDefinition(fluid);
		if (color == -1) {
			color = 0x0000ff;
			if (definition != null) {
				color = definition.getParticleColor();
			}
		}
		return color;
	}
    public static boolean isPaused() { return Minecraft.getInstance().isPaused(); }
    public static int currentContainerId() { var player = Minecraft.getInstance().player; return player == null ? -1 : player.containerMenu.containerId; }
    public static boolean hasShiftDown() { return Minecraft.getInstance().hasShiftDown(); }
    public static int getLeafTint(@org.jetbrains.annotations.Nullable forestry.api.arboriculture.ITreeSpecies species,
            @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level, @org.jetbrains.annotations.Nullable BlockPos pos) {
        if (species == null) return net.minecraft.world.level.FoliageColor.FOLIAGE_DEFAULT;
        return forestry.api.client.IForestryClientApi.INSTANCE.getTreeManager().getTint(species)
            .get(level instanceof net.minecraft.client.renderer.block.BlockAndTintGetter tintLevel ? tintLevel : null, pos);
    }
	public static ServerBreedingHandler newBreedingHandler() {
		return new ClientBreedingHandler();
	}

	private static RecipeMap recipes = RecipeMap.EMPTY;

	public static RecipeMap getRecipeMap() {
		return forestry.core.platform.recipes.RecipeManagers.getRecipeMap();
	}

	public static void setRecipes(RecipeMap recipes) {
		forestry.core.platform.recipes.RecipeManagers.replace(recipes);
	}

	public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload packet) {
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packet);
	}

	public static Registry<Fluid> getFluidRegistry() {
		return Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.FLUID);
	}

	public static void markForUpdate(BlockPos pos) {
		var level = Minecraft.getInstance().level;
		if (level != null) {
			level.setSectionDirtyWithNeighbors(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
		}
	}

	public static RegistryAccess getRegistryAccess() {
		return Minecraft.getInstance().level.registryAccess();
	}

	public static Optional<Resource> getResource(Identifier path) {
		return Minecraft.getInstance().getResourceManager().getResource(path);
	}
}
