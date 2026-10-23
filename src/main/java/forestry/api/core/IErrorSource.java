package forestry.api.core;

import java.util.Set;

public interface IErrorSource {
	IErrorSource EMPTY = Set::of;

	/**
	 * @return The current errors in this error source, or an empty set if there are no errors.
	 */
	Set<IError> getErrors();
}
