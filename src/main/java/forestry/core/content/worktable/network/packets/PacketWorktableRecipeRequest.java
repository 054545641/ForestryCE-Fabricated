package forestry.core.content.worktable.network.packets;
import forestry.core.content.worktable.screens.WorktableMenu;
import forestry.core.content.worktable.inventory.WorktableCraftingContainer;
import forestry.core.content.worktable.recipes.MemorizedRecipe;
import forestry.core.platform.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import javax.annotation.Nullable;
import java.util.List;
/** A request supplies only a ghost pattern and a preferred ID; outputs are always computed by the server. */
public record PacketWorktableRecipeRequest(int containerId, BlockPos pos, List<ItemStack> matrix, @Nullable Identifier selected) implements CustomPacketPayload {
    public static final Type<PacketWorktableRecipeRequest> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("worktable_recipe_request"));
    public PacketWorktableRecipeRequest { if (matrix.size() != 9) throw new IllegalArgumentException("Crafting pattern must have nine slots"); matrix = matrix.stream().map(stack -> stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1)).toList(); }
    public PacketWorktableRecipeRequest(BlockPos pos, MemorizedRecipe recipe) {
        this(forestry.core.platform.client.ClientsideCode.currentContainerId(), pos, InventoryUtil.getStacks(recipe.getCraftMatrix()), recipe.getSelectedRecipe() == null ? null : recipe.getSelectedRecipe().id().identifier());
    }
    @Override public Type<PacketWorktableRecipeRequest> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketWorktableRecipeRequest msg) {
        buffer.writeVarInt(msg.containerId); buffer.writeBlockPos(msg.pos);
        for (var stack : msg.matrix) ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
        buffer.writeBoolean(msg.selected != null); if (msg.selected != null) buffer.writeIdentifier(msg.selected);
    }
    public static PacketWorktableRecipeRequest decode(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readVarInt(); var pos = buffer.readBlockPos(); var stacks = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        return new PacketWorktableRecipeRequest(id, pos, stacks, buffer.readBoolean() ? buffer.readIdentifier() : null);
    }
    public static void handle(PacketWorktableRecipeRequest msg, ServerPlayer player) {
        if (!(player.containerMenu instanceof WorktableMenu menu) || menu.containerId != msg.containerId || !menu.stillValid(player) || !menu.getTile().getBlockPos().equals(msg.pos)) return;
        var matrix = new WorktableCraftingContainer();
        for (int i = 0; i < 9; i++) matrix.setItem(i, msg.matrix.get(i).copy());
        var matches = RecipeUtils.getRecipes(RecipeType.CRAFTING, matrix.asCraftInput(), player.level());
        var recipe = new MemorizedRecipe(matrix, matches); if (msg.selected != null) recipe.selectRecipe(msg.selected);
        menu.getTile().setCurrentRecipe(recipe); menu.updateCraftMatrix();
        NetworkUtil.sendNetworkPacket(new PacketWorktableRecipeUpdate(menu.getTile()), msg.pos, player.level());
    }
}
