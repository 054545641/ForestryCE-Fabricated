package forestry.core.content.worktable.network.packets;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.content.worktable.recipes.MemorizedRecipe;
import forestry.core.content.worktable.tiles.WorktableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
/** Captures owned bytes before mutable worktable state can change again. */
public record PacketWorktableRecipeUpdate(BlockPos pos, byte[] payload) implements CustomPacketPayload {
    public static final Type<PacketWorktableRecipeUpdate> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("worktable_crafting_update"));
    public PacketWorktableRecipeUpdate { pos = pos.immutable(); payload = payload.clone(); NetworkUtil.boundedLength(payload.length, NetworkUtil.MAX_PAYLOAD_BYTES); }
    @Override public byte[] payload() { return payload.clone(); }
    public PacketWorktableRecipeUpdate(WorktableTile tile) { this(tile.getBlockPos(), NetworkUtil.encodePayload(tile.getLevel().registryAccess(), buffer -> NetworkUtil.writeStreamable(buffer, tile.getCurrentRecipe()))); }
    @Override public Type<PacketWorktableRecipeUpdate> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketWorktableRecipeUpdate msg) { buffer.writeBlockPos(msg.pos); buffer.writeInt(msg.payload.length); buffer.writeBytes(msg.payload); }
    public static PacketWorktableRecipeUpdate decode(RegistryFriendlyByteBuf buffer) { return new PacketWorktableRecipeUpdate(buffer.readBlockPos(), NetworkUtil.readPayloadBytes(buffer)); }
    public static void handle(PacketWorktableRecipeUpdate msg, Player player) {
        TileUtil.actOnTile(player.level(), msg.pos, WorktableTile.class, tile -> NetworkUtil.applyPayload(player.registryAccess(), msg.payload, buffer -> tile.setCurrentRecipe(NetworkUtil.<MemorizedRecipe>readStreamable(buffer, MemorizedRecipe::new))));
    }
}
