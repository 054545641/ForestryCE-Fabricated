package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Supplier;

public class FeatureRecipeType<R extends Recipe<?>> extends ModFeature implements IModFeature {
	private final RegistryEntry<RecipeType<?>, RecipeType<R>> type;
	private final RegistryEntry<RecipeSerializer<?>, RecipeSerializer<? extends R>> serializer;

	protected FeatureRecipeType(IFeatureRegistry registry, Identifier moduleId, String name, Supplier<RecipeSerializer<? extends R>> serializer) {
		super(moduleId, name);

		this.type = registry.getRegistry(Registries.RECIPE_TYPE).register(name, () -> new RecipeType<R>() {
            @Override public String toString() { return moduleId.withPath(name).toString(); }
        });
		this.serializer = registry.getRegistry(Registries.RECIPE_SERIALIZER).register(name, serializer);
	}

	public RecipeType<R> type() {
		return this.type.get();
	}

	public RecipeSerializer<? extends R> serializer() {
		return this.serializer.get();
	}

	@Override
	public ResourceKey<? extends Registry<?>> getRegistry() {
		return Registries.RECIPE_TYPE;
	}
}
