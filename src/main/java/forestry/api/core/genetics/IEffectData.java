package forestry.api.core.genetics;

import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;

/**
 * Container to hold some temporary data for bee, tree and butterfly effects.
 *
 * @author SirSengir
 */
public interface IEffectData extends INbtWritable, INbtReadable {
	void setInteger(int index, int val);

	void setBoolean(int index, boolean val);

	int getInteger(int index);

	boolean getBoolean(int index);
}
