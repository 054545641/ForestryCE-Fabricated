package forestry.core.platform.util;

import com.google.common.base.Preconditions;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class ModUtil {
	public static boolean isModLoaded(String modname) {
		return FabricLoader.getInstance().isModLoaded(modname);
	}

	public static boolean isModLoaded(String modname, @Nullable String versionRangeString) {
		var container = FabricLoader.getInstance().getModContainer(modname);
		if (container.isEmpty()) return false;
		if (versionRangeString == null) return true;
		try {
			// Use the same predicates as fabric.mod.json dependencies (for example ">=1.0 <2.0").
			return VersionPredicate.parse(versionRangeString).test(container.get().getMetadata().getVersion());
		} catch (VersionParsingException exception) {
			throw new IllegalArgumentException("Invalid Fabric version predicate: " + versionRangeString, exception);
		}
	}

	public static Identifier getRegistryName(Fluid o) {
		return BuiltInRegistries.FLUID.getKey(o);
	}

	public static Identifier getRegistryName(Block o) {
		return BuiltInRegistries.BLOCK.getKey(o);
	}

	public static Identifier getRegistryName(Item o) {
		return BuiltInRegistries.ITEM.getKey(o);
	}

	public static Identifier getRegistryName(ParticleType<?> o) {
		return BuiltInRegistries.PARTICLE_TYPE.getKey(o);
	}

	// todo use in more parts of the mod
	public static void checkNotEmpty(@Nullable Item item) {
		Preconditions.checkArgument(item != null && item != Items.AIR);
	}

	// todo use in more parts of the mod
	public static void checkNotEmpty(@Nullable Block block) {
		Preconditions.checkArgument(block != null && block != Blocks.AIR);
	}

	public static Identifier withSuffix(Identifier id, String suffix) {
		return Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + suffix);
	}
}
