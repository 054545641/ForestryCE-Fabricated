package forestry.core.engine.climate;

import forestry.api.core.climate.ClimateState;
import forestry.api.core.climate.IClimateManager;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.core.platform.data.LogicalData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

public class ForestryClimateManager implements IClimateManager {
	private static final ClimateState DEFAULT = new ClimateState(TemperatureType.NORMAL, HumidityType.NORMAL);
	private volatile Map<ResourceKey<Biome>, ClimateState> serverClimates = Map.of();
	private volatile Map<ResourceKey<Biome>, ClimateState> clientClimates = Map.of();

	@Override
	public TemperatureType getTemperature(Holder<Biome> biome) {
		for (TemperatureType temperature : TemperatureType.VALUES) {
			if (biome.is(temperature.tag)) return temperature;
		}
		return TemperatureType.getFromValue(biome.value().getBaseTemperature());
	}

	@Override
	public TemperatureType getTemperature(ResourceKey<Biome> biome) {
		return climates().getOrDefault(biome, DEFAULT).temperature();
	}

	@Override
	public HumidityType getHumidity(Holder<Biome> biome) {
		for (HumidityType humidity : HumidityType.VALUES) {
			if (biome.is(humidity.tag)) return humidity;
		}
		return HumidityType.getFromValue(biome.value().climateSettings.downfall());
	}

	@Override
	public HumidityType getHumidity(ResourceKey<Biome> biome) {
		return climates().getOrDefault(biome, DEFAULT).humidity();
	}

	private Map<ResourceKey<Biome>, ClimateState> climates() {
		return LogicalData.isServer() ? this.serverClimates : this.clientClimates;
	}

	@Override
	public IClimateProvider createClimateProvider(LevelReader world, BlockPos pos) {
		return new ClimateProvider(world, pos);
	}

	@Override
	public IClimateProvider createDummyClimateProvider() {
		return FakeClimateProvider.INSTANCE;
	}

	@Override
	public ClimateState getState(ServerLevel level, BlockPos pos) {
		// todo implement climate modifiers
		return getBiomeState(level, pos);
	}

	@Override
	public ClimateState getBiomeState(LevelReader level, BlockPos coordinates) {
		Holder<Biome> biome = level.getBiome(coordinates);
		return new ClimateState(getTemperature(biome), getHumidity(biome));
	}

	@ApiStatus.Internal
	public void onBiomesReloaded(Registry<Biome> registry) {
		onBiomesReloaded(registry, !LogicalData.isServer());
	}

	@ApiStatus.Internal
	public void onBiomesReloaded(Registry<Biome> registry, boolean clientSide) {
		Map<ResourceKey<Biome>, ClimateState> climates = new HashMap<>();
		registry.listElements().forEach(holder -> climates.put(holder.key(), new ClimateState(getTemperature(holder), getHumidity(holder))));
		if (clientSide) this.clientClimates = Map.copyOf(climates);
		else this.serverClimates = Map.copyOf(climates);
	}

	@ApiStatus.Internal
	public void clearClient() {
		this.clientClimates = Map.of();
	}
}
