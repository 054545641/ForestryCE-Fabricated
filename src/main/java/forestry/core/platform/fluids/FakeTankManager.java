package forestry.core.platform.fluids;

public final class FakeTankManager extends TankManager {
	public static final FakeTankManager instance = new FakeTankManager();

	private FakeTankManager() {
	}

	@Override
	public void add(StandardTank tank) {
		throw new UnsupportedOperationException("Empty tank manager");
	}
}
