package forestry.agriculture.farmlogic;

public enum FarmingStage {
	CULTIVATE, HARVEST;

	public FarmingStage next() {
		if (this == CULTIVATE) {
			return HARVEST;
		} else {
			return CULTIVATE;
		}
	}
}
