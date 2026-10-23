package forestry.apiculture.worldgen;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/** Uses the three verified access-transformer fields; keeps codec and sampling views consistent. */
public final class TemplatePoolInsertion {
	private TemplatePoolInsertion() {
	}

	public static void append(StructureTemplatePool pool, StructurePoolElement piece, int weight) {
		if (weight < 1 || weight > 150) throw new IllegalArgumentException("Pool weight must be between 1 and 150");
		if (pool.getTemplates().stream().anyMatch(entry -> entry.getFirst().getClass() == piece.getClass() && entry.getFirst().toString().equals(piece.toString()))) return;
		var entries = new ArrayList<>(pool.getTemplates());
		entries.add(Pair.of(piece, weight));
		pool.rawTemplates = entries;
		for (int i = 0; i < weight; i++) pool.templates.add(piece);
		pool.maxSize = Integer.MIN_VALUE;
	}
}
