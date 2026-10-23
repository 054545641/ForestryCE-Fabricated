package forestry.api.lepidopterology;

import forestry.api.core.genetics.pollen.IPollen;
import forestry.api.lepidopterology.genetics.IButterfly;
import net.minecraft.world.entity.PathfinderMob;

import javax.annotation.Nullable;

public interface IEntityButterfly {
	void changeExhaustion(int change);

	int getExhaustion();

	IButterfly getButterfly();

	/**
	 * @return The entity as an EntityCreature to save casting.
	 */
	PathfinderMob getEntity();

	@Nullable
	IPollen<?> getPollen();

	void setPollen(@Nullable IPollen<?> pollen);

	boolean canMateWith(IEntityButterfly butterfly);

	boolean canMate();
}
