package forestry.agriculture.multifarm.gui;

import forestry.api.core.climate.IClimateProvider;

public interface IFarmLedgerDelegate extends IClimateProvider {
	float getHydrationModifier();

	float getHydrationTempModifier();

	float getHydrationHumidModifier();

	float getHydrationRainfallModifier();

	double getDrought();
}
