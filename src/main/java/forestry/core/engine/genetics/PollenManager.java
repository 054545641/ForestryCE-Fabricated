package forestry.core.engine.genetics;

import com.google.common.collect.ImmutableMap;
import forestry.api.core.genetics.pollen.IPollen;
import forestry.api.core.genetics.pollen.IPollenManager;
import forestry.api.core.genetics.pollen.IPollenType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;


public class PollenManager implements IPollenManager {
	private final ImmutableMap<Identifier, IPollenType<?>> types;

	public PollenManager(ImmutableMap<Identifier, IPollenType<?>> types) {
		this.types = types;
	}

	@Override
	public boolean canPollinate(LevelAccessor level, BlockPos pos, @org.jetbrains.annotations.Nullable Object pollinator) {
		for (IPollenType<?> type : this.types.values()) {
			if (type.canPollinate(level, pos, pollinator)) {
				return true;
			}
		}

		return false;
	}

	@Nullable
	@Override
	public IPollen<?> getPollen(LevelAccessor level, BlockPos pos, @Nullable Object pollinator) {
		for (IPollenType<?> type : this.types.values()) {
			IPollen<?> pollen = type.tryCollectPollen(level, pos, pollinator);

			if (pollen != null) {
				return pollen;
			}
		}

		return null;
	}

	@Nullable
	@Override
	public IPollen<?> getPollenOfType(LevelAccessor level, BlockPos pos, @Nullable Object pollinator, Set<Identifier> pollenTypes) {
		for (Identifier id : pollenTypes) {
			IPollenType<?> type = this.types.get(id);

			if (type != null) {
				IPollen<?> pollen = type.tryCollectPollen(level, pos, pollinator);

				if (pollen != null) {
					return pollen;
				}
			}
		}

		return null;
	}

	@Nullable
	@Override
	public IPollenType<?> getPollenType(Identifier id) {
		return this.types.get(id);
	}

	@Override
	public Collection<IPollenType<?>> getAllPollenTypes() {
		return this.types.values();
	}
}
