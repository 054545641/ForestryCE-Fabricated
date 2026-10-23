package forestry.api.apiculture;

import com.mojang.authlib.GameProfile;
import forestry.api.core.climate.IBiomeProvider;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.IErrorLogicSource;
import forestry.api.core.ILocationProvider;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface IBeeHousing extends IErrorLogicSource, IClimateProvider, IBiomeProvider, ILocationProvider {

	/**
	 * Used by the beekeeping logic
	 * to combine bee modifiers from several sources that can change over time.
	 *
	 * @return IBeeModifiers from the housing, frames, etc.
	 */
	Iterable<IBeeModifier> getBeeModifiers();

	/**
	 * Used by the beekeeping logic
	 * to combine bee listeners from several sources that can change over time.
	 *
	 * @return IBeeListeners from the housing, multiblock parts, etc.
	 */
	Iterable<IBeeListener> getBeeListeners();

	IBeeHousingInventory getBeeInventory();

	IBeekeepingLogic getBeekeepingLogic();

	int getBlockLightValue();

	boolean canBlockSeeTheSky();

	boolean isRaining();

	@Nullable
	GameProfile getOwner();

	/**
	 * @return exact coordinates where bee particle FX should spawn from
	 * @since Forestry 4.2
	 */
	Vec3 getBeeFXCoordinates();
}
