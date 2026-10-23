package forestry.core.platform.registration;

import forestry.core.platform.item.DrinkProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class FluidProperties {
	public final int density;
	public final int viscosity;
	public final int temperature;
	public final int particleColor;
	public final int flammability;
	public final int luminosity;
	public final boolean spreadsFire;
	@Nullable
	public final DrinkProperties properties;
	public final Identifier[] resources = new Identifier[2];
	public final Supplier<Item> bucket;

	public FluidProperties(FeatureFluid.Builder builder) {
		this.density = builder.density;
		this.viscosity = builder.viscosity;
		this.temperature = builder.temperature;
		this.particleColor = builder.particleColor;
		this.flammability = builder.flammability;
		this.luminosity = builder.luminosity;
		this.spreadsFire = builder.spreadsFire;
		this.properties = builder.properties;
		this.resources[0] = builder.registry.getModuleId().withPath("block/liquid/" + builder.identifier + "_still");
		this.resources[1] = builder.registry.getModuleId().withPath("block/liquid/" + builder.identifier + "_flow");
		this.bucket = builder.bucket;
	}

}
