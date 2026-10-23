package forestry.gametest;

import java.util.Map;

import com.mojang.serialization.JsonOps;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import io.netty.buffer.Unpooled;

import net.minecraft.resources.RegistryOps;

import forestry.api.ForestryConstants;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.ForestryTaxa;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AlleleOverride;
import forestry.api.core.genetics.alleles.ForestryAlleles;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.trees.genetics.TreeSpeciesDefinition;

@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class TreeSpeciesDefinitionTest {
	private static TreeSpeciesDefinition sample() {
		return new TreeSpeciesDefinition(
			ForestryTaxa.GENUS_QUERCUS,
			ForestryTaxa.SPECIES_OAK,
			true,
			false,
			false,
			0,
			"Sengir",
			0x619a3c,
			TemperatureType.NORMAL,
			HumidityType.NORMAL,
			0.0f,
			Map.of(
				// one inline-value chromosome
				TreeChromosomes.HEIGHT.id(), AlleleOverride.both(ForestryAlleles.HEIGHT_AVERAGE),
				// one reference chromosome
				TreeChromosomes.FRUIT.id(), AlleleOverride.both(Allele.reference(forestry.api.arboriculture.ForestryFruits.APPLE)),
				// one heterozygous override
				TreeChromosomes.GIRTH.id(), new AlleleOverride<>(ForestryAlleles.GIRTH_2, ForestryAlleles.GIRTH_1)
			)
		);
	}

	@GameTest(template = "empty")
	public static void codecRoundTrips(GameTestHelper helper) {
		TreeSpeciesDefinition def = sample();
		var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		var json = TreeSpeciesDefinition.codec().encodeStart(ops, def).getOrThrow();
		TreeSpeciesDefinition decoded = TreeSpeciesDefinition.codec().parse(ops, json).getOrThrow();
		if (!decoded.equals(def)) {
			helper.fail("Codec round-trip mismatch: " + decoded + " != " + def);
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void streamCodecRoundTrips(GameTestHelper helper) {
		TreeSpeciesDefinition def = sample();
		// Same idiom MutationRecipeTest uses for its stream-codec round trip.
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		TreeSpeciesDefinition.streamCodec().encode(buf, def);
		TreeSpeciesDefinition decoded = TreeSpeciesDefinition.streamCodec().decode(buf);
		if (!decoded.equals(def)) {
			helper.fail("Stream codec round-trip mismatch: " + decoded + " != " + def);
			return;
		}
		helper.succeed();
	}
}
