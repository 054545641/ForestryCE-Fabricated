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
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class ModUtil {
	public static boolean isModLoaded(String modname) {
		return ModList.get().isLoaded(modname);
	}

	public static boolean isModLoaded(String modname, @Nullable String versionRangeString) {
		if (!isModLoaded(modname)) {
			return false;
		}

		if (versionRangeString != null) {
			Optional<? extends ModContainer> cont = ModList.get().getModContainerById(modname);
			if (cont.isPresent()) {
				ModContainer modContainer = cont.get();

				ArtifactVersion modVersion = modContainer.getModInfo().getVersion();

				VersionRange range = VersionRange.createFromVersion(versionRangeString);
				DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion(versionRangeString);    //TODO - check

				return requiredVersion.compareTo(modVersion) > 0; //TODO - this comparison is incorrect
			}
		}

		return true;
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
