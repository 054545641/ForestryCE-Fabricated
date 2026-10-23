package forestry.mail.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.mail.letters.Letter;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

/**
 * Mail-side {@link DataComponentType} registrations.
 */
@FeatureProvider
public class MailDataComponents {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.MAIL);
	private static final RegistrationBatch<DataComponentType<?>> DATA_COMPONENT_TYPES = REGISTRY.getRegistry(Registries.DATA_COMPONENT_TYPE);

	public static final RegistryEntry<DataComponentType<?>, DataComponentType<Letter>> LETTER_DATA =
		DATA_COMPONENT_TYPES.register(
			"letter_data",
			() -> DataComponentType.<Letter>builder()
				.persistent(Letter.CODEC)
				.networkSynchronized(ByteBufCodecs.fromCodec(Letter.CODEC))
				.build());
}
