package forestry.core.engine.circuits;

public interface IEngineUpgradeable {
	void applyEngineUpgrade(float outputBoost, float efficiencyMult, int heat);

	void removeEngineUpgrade(float outputBoost, float efficiencyMult, int heat);
}
