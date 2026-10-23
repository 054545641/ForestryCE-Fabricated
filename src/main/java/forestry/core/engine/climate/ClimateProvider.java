package forestry.core.engine.climate;

import forestry.api.IForestryApi;
import forestry.api.core.climate.IBiomeProvider;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

public class ClimateProvider implements IClimateProvider, IBiomeProvider {
	private final Holder<Biome> biome;

	public ClimateProvider(LevelReader level, BlockPos pos) {
		this.biome = level.getBiome(pos);
	}

	@Override
	public Holder<Biome> getBiome() {
		return this.biome;
	}

	@Override
	public TemperatureType temperature() {
		return IForestryApi.INSTANCE.getClimateManager().getTemperature(this.biome);
	}

	@Override
	public HumidityType humidity() {
		return IForestryApi.INSTANCE.getClimateManager().getHumidity(this.biome);
	}
}
