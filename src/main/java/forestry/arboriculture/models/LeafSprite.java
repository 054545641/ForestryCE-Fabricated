package forestry.arboriculture.models;

import forestry.api.client.arboriculture.ILeafSprite;
import net.minecraft.resources.Identifier;

public class LeafSprite implements ILeafSprite {
	private final Identifier fast;
	private final Identifier fancy;
	private final Identifier pollinatedFast;
	private final Identifier pollinatedFancy;

	public LeafSprite(Identifier fast, Identifier fancy, Identifier pollinatedFast, Identifier pollinatedFancy) {
		this.fast = fast;
		this.fancy = fancy;
		this.pollinatedFast = pollinatedFast;
		this.pollinatedFancy = pollinatedFancy;
	}

	public static LeafSprite create(Identifier id) {
		String namespace = id.getNamespace();
		String path = "block/leaves/" + id.getPath();

		return new LeafSprite(
			Identifier.fromNamespaceAndPath(namespace, path + "_fast"),
			Identifier.fromNamespaceAndPath(namespace, path),
			Identifier.fromNamespaceAndPath(namespace, path + "_pollinated_fast"),
			Identifier.fromNamespaceAndPath(namespace, path + "_pollinated")
		);
	}

	@Override
	public Identifier get(boolean pollinated, boolean fancy) {
		if (pollinated) {
			return fancy ? this.pollinatedFancy : this.pollinatedFast;
		} else {
			return fancy ? this.fancy : this.fast;
		}
	}

	@Override
	public Identifier getParticle() {
		return this.fancy;
	}
}
