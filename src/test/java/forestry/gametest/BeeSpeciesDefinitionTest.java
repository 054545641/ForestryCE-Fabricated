package forestry.gametest;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import com.google.gson.JsonElement;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.ForestryBeeEffects;
import forestry.api.apiculture.ForestryBeeJubilances;
import forestry.api.core.HumidityType;
import forestry.api.core.Product;
import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AlleleOverride;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.alleles.ForestryAlleles;
import forestry.apiculture.bees.genetics.BeeSpeciesDefinition;

/**
 * Behavioral oracle for {@link BeeSpeciesDefinition}: proves that a definition with a data-chromosome genome
 * override (speed), a reference-chromosome genome override (bee effect), a heterozygous override (fertility), and
 * a one-sided override (lifespan) survives both the lazily-built JSON codec and the lazily-built network stream
 * codec unchanged.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class BeeSpeciesDefinitionTest {
	@GameTest(template = "empty")
	public static void definitionCodecRoundTrip(GameTestHelper helper) {
		BeeSpeciesDefinition definition = new BeeSpeciesDefinition(
			"Testus",
			"exampleus",
			true,
			false,
			false,
			2,
			"TestAuthority",
			0x123456,
			TemperatureType.WARM,
			HumidityType.DAMP,
			0x112233,
			0x445566,
			0x778899,
			List.of(Product.of(Items.HONEY_BOTTLE)),
			List.of(),
			ForestryBeeJubilances.HERMIT,
			Map.of(
				BeeChromosomes.SPEED.id(), AlleleOverride.both(ForestryAlleles.SPEED_SLOWER),
				BeeChromosomes.EFFECT.id(), AlleleOverride.both(Allele.reference(ForestryBeeEffects.BEATIFIC)),
				// heterozygous: the two sides differ in both value and dominance
				BeeChromosomes.FERTILITY.id(), new AlleleOverride<>(ForestryAlleles.FERTILITY_4, ForestryAlleles.FERTILITY_1),
				// one-sided: the active allele keeps whatever the karyotype default produced
				BeeChromosomes.LIFESPAN.id(), AlleleOverride.onlyInactive(ForestryAlleles.LIFESPAN_SHORT)
			)
		);

		// JSON codec.
		Codec<BeeSpeciesDefinition> codec = BeeSpeciesDefinition.codec();
		JsonElement json = codec.encodeStart(JsonOps.INSTANCE, definition).getOrThrow();
		BeeSpeciesDefinition fromJson = codec.parse(JsonOps.INSTANCE, json).getOrThrow();
		if (!fromJson.equals(definition)) {
			helper.fail("JSON codec round-trip changed the bee species definition: " + definition + " -> " + fromJson);
			return;
		}

		// Network stream codec.
		StreamCodec<RegistryFriendlyByteBuf, BeeSpeciesDefinition> streamCodec = BeeSpeciesDefinition.streamCodec();
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		streamCodec.encode(buf, definition);
		BeeSpeciesDefinition fromBuf = streamCodec.decode(buf);
		if (!fromBuf.equals(definition)) {
			helper.fail("Stream codec round-trip changed the bee species definition: " + definition + " -> " + fromBuf);
			return;
		}

		helper.succeed();
	}
}
