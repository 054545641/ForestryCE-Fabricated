package forestry.core.engine.genetics;

import javax.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.resources.Identifier;

import forestry.api.apiculture.ForestryFlowerTypes;
import forestry.api.apiculture.IFlowerType;
import forestry.api.core.genetics.IFlowerTypeManager;

/**
 * The live flower types. Owned by base rather than by apiculture because both bees and butterflies
 * carry a flower type chromosome, and a butterfly must resolve its flowers whether or not the
 * apiculture jar is installed.
 *
 * <p>Holds two maps. The code-registered base comes from plugins through
 * {@code IGeneticRegistration.registerFlowerType}; the effective map is that base overlaid by the
 * datapack-loaded (or sync-delivered) definitions, datapack winning on id.
 */
public class ForestryFlowerTypeManager implements IFlowerTypeManager {
	private ImmutableMap<Identifier, IFlowerType> codeFlowerTypes = ImmutableMap.of();
	private ImmutableMap<Identifier, IFlowerType> flowerTypes = ImmutableMap.of();

	@Nullable
	@Override
	public IFlowerType getFlowerType(Identifier id) {
		return this.flowerTypes.get(id);
	}

	@Override
	public IFlowerType getFlowerTypeSafe(Identifier id) {
		IFlowerType type = this.flowerTypes.get(id);
		return type != null ? type : this.flowerTypes.get(ForestryFlowerTypes.VANILLA);
	}

	@Override
	public Map<Identifier, IFlowerType> getAllFlowerTypes() {
		return this.flowerTypes;
	}

	@Override
	public void setFlowerTypes(Map<Identifier, IFlowerType> flowerTypes) {
		this.flowerTypes = ImmutableMap.copyOf(flowerTypes);
	}

	/**
	 * Called once during plugin registration with the flower types registered in code.
	 *
	 * @param codeFlowerTypes The code-registered flower types, keyed by id
	 */
	public void setCodeFlowerTypes(ImmutableMap<Identifier, IFlowerType> codeFlowerTypes) {
		this.codeFlowerTypes = codeFlowerTypes;
		// bootstrap: code base alone until the first datapack load
		this.flowerTypes = codeFlowerTypes;
	}

	/**
	 * Installs the effective flower types: the code-registered base overlaid by the datapack-loaded
	 * or sync-delivered definitions, datapack winning on id.
	 *
	 * @param dataDefinitions The datapack-loaded flower types, keyed by id
	 */
	public void rebuild(Map<Identifier, IFlowerType> dataDefinitions) {
		Map<Identifier, IFlowerType> effective = new LinkedHashMap<>(this.codeFlowerTypes);
		effective.putAll(dataDefinitions);
		this.flowerTypes = ImmutableMap.copyOf(effective);
	}
}
