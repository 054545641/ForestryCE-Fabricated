package forestry.api.core.fluids;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;

/** Business recipes and legacy saves use mB; Fabric storage uses exact long droplets. */
public final class FluidUnits {
    public static final long DROPLETS_PER_MILLIBUCKET = FluidConstants.BUCKET / 1000;

    static {
        if (FluidConstants.BUCKET % 1000 != 0) throw new IllegalStateException("Fabric bucket is not divisible into milliBuckets");
    }

    private FluidUnits() {}

    public static long fromMilliBuckets(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative fluid amount");
        return Math.multiplyExact(amount, DROPLETS_PER_MILLIBUCKET);
    }

    public static long toMilliBucketsExact(long droplets) {
        if (droplets < 0 || droplets % DROPLETS_PER_MILLIBUCKET != 0) {
            throw new IllegalArgumentException("Fluid amount cannot be expressed as whole milliBuckets: " + droplets);
        }
        return droplets / DROPLETS_PER_MILLIBUCKET;
    }
}
