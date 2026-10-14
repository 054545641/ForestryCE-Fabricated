package forestry.api.core;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Keeps track of error information used by Forestry.
 * Register errors using {@link forestry.api.plugin.IForestryPlugin#registerErrors}.
 */
public interface IErrorManager {
	/**
	 * Retrieves an error by its numeric ID.
	 *
	 * @param id The numeric ID of the error.
	 * @return The error with the given ID, or {@code null} if no error is registered with that ID.
	 */
	@Nullable
	IError getError(short id);

	/**
	 * Retrieves an error by its unique ID. Safe for NBT serialization.
	 *
	 * @param errorId The ID of the error.
	 * @return The error with the ID.
	 */
	@Nullable
	IError getError(Identifier errorId);

	/**
	 * @return A set of all registered errors.
	 */
	List<IError> getErrors();

	/**
	 * @return A new instance of an IErrorLogic.
	 */
	IErrorLogic createErrorLogic();

	/**
	 * Retrieves the numeric ID of this error. Only use for network synchronization.
	 *
	 * @param error The error.
	 * @return The numeric ID of the error.
	 */
	short getNumericId(IError error);
}
