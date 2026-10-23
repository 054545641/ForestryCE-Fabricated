package forestry.core.platform.network.packets;

import forestry.core.platform.recipes.RecipeManagers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public class RecipeCachePacket implements CustomPacketPayload {
    public static final Type<RecipeCachePacket> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("recipe_cache"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, RecipeCachePacket msg) {
	}

	public static RecipeCachePacket decode(RegistryFriendlyByteBuf buffer) {
		return new RecipeCachePacket();
	}

	public static void handle(RecipeCachePacket msg, Player player) {
		RecipeManagers.invalidateCaches();
	}
}
