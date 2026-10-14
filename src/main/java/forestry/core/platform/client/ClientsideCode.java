package forestry.core.platform.client;

import forestry.core.engine.genetics.root.ClientBreedingHandler;
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
	public static ServerBreedingHandler newBreedingHandler() {
		return new ClientBreedingHandler();
	}

	private static RecipeMap recipes = RecipeMap.EMPTY;

	public static RecipeMap getRecipeMap() {
		return recipes;
	}

	public static void setRecipes(RecipeMap recipes) {
		ClientsideCode.recipes = recipes;
	}

	public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload packet) {
		net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(packet);
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
