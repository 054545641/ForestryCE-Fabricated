package forestry.api.agriculture;

import forestry.api.core.circuits.ICircuit;

public interface IFarmCircuit extends ICircuit {
	IFarmType getProperties();

	boolean isManual();
}
