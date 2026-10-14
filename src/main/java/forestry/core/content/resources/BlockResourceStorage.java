package forestry.core.content.resources;

import net.minecraft.world.level.block.Block;

public class BlockResourceStorage extends Block {
	private final EnumResourceType type;

	public BlockResourceStorage(Properties properties, EnumResourceType type) {
		super(properties.strength(3f, 5f));
		this.type = type;
	}

	public EnumResourceType getType() {
		return this.type;
	}
}
