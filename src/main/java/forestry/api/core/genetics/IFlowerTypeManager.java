package forestry.api.core.genetics;

import javax.annotation.Nullable;

import java.util.Map;

import net.minecraft.resources.Identifier;

import forestry.api.apiculture.IFlowerType;

/**
 * Used to look up the flower types that pollinating species search for. Bees and butterflies both
 * carry a flower type chromosome, so this is owned by base rather than by either module.
 *
 * @since 2.10.0
 */
public interface IFlowerTypeManager {
	/**
	 * Used to get a flower type by its id.
	 *
	 * @param id The id of the flower type
	 * @return The flower type, or null if no flower type was registered with that id
	 */
	@Nullable
	IFlowerType getFlowerType(Identifier id);

	/**
	 * Used to get a flower type by its id, falling back to the vanilla flower type.
	 *
	 * @param id The id of the flower type
	 * @return The flower type, or the vanilla flower type if none was registered with that id
	 */
	IFlowerType getFlowerTypeSafe(Identifier id);

	/**
	 * Used to get every registered flower type.
	 *
	 * @return The flower types, keyed by id
	 */
	Map<Identifier, IFlowerType> getAllFlowerTypes();

	/**
	 * Used to install the effective flower types after a datapack load or a server sync.
	 *
	 * @param flowerTypes The flower types, keyed by id
	 */
	void setFlowerTypes(Map<Identifier, IFlowerType> flowerTypes);
}
