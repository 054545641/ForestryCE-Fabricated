package forestry.fabric.test;
import forestry.core.content.sorting.*;
import forestry.core.content.sorting.features.SortingBlocks;
import forestry.core.content.sorting.tiles.TileGeneticFilter;
import forestry.core.content.sorting.network.packets.*;
import forestry.core.content.worktable.features.WorktableBlocks;
import forestry.core.content.worktable.tiles.WorktableTile;
import forestry.core.content.worktable.inventory.WorktableCraftingContainer;
import forestry.core.content.worktable.recipes.*;
import forestry.core.content.worktable.screens.WorktableMenu;
import forestry.core.content.worktable.network.packets.*;
import forestry.core.platform.util.NetworkUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
public final class F2StorageTests {
    @GameTest public void worktableReloadKeepsSelectedRecipeById(GameTestHelper h) {
        var previous = forestry.core.platform.recipes.RecipeManagers.getRecipeMap();
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) RecipeUtilsForTest().value();
        var first = new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Identifier.parse("forestry:test_a")), recipe);
        var selected = new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Identifier.parse("forestry:test_b")), recipe);
        var last = new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Identifier.parse("forestry:test_c")), recipe);
        try {
            forestry.core.platform.recipes.RecipeManagers.replace(net.minecraft.world.item.crafting.RecipeMap.create(java.util.List.of(first, selected, last)));
            var memory = new MemorizedRecipe(new WorktableCraftingContainer(), java.util.List.of(first, selected, last)); memory.selectRecipe(selected.id().identifier());
            forestry.core.platform.recipes.RecipeManagers.replace(net.minecraft.world.item.crafting.RecipeMap.create(java.util.List.of(selected, last)));
            h.assertTrue(memory.getSelectedRecipe().id().equals(selected.id()), "Removing an earlier conflict changed the selected recipe");
            var loaded = new MemorizedRecipe(memory.write(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(loaded.getSelectedRecipe().id().equals(selected.id()), "Recipe choice changed after save/load following reload");
        } finally { forestry.core.platform.recipes.RecipeManagers.replace(previous); }
        h.succeed();
    }
    private static net.minecraft.world.item.crafting.RecipeHolder<?> RecipeUtilsForTest() { return forestry.core.platform.util.RecipeUtils.getRecipe(Identifier.parse("minecraft:oak_planks")); }

    @GameTest public void sortingNativeRoutingAbortSaveAndTick(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2)); var level = h.getLevel();
        level.setBlockAndUpdate(pos, SortingBlocks.FILTER.defaultState());
        level.setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState());
        var tile = (TileGeneticFilter) level.getBlockEntity(pos);
        var port = ItemStorage.SIDED.find(level, pos, Direction.UP);
        try (var tx = Transaction.openOuter()) { h.assertTrue(port.insert(ItemVariant.of(Items.DIAMOND), 7, tx) == 0, "Closed filter accepted items"); }
        tile.getLogic().setRule(Direction.EAST, DefaultFilterRuleType.ITEM);
        try (var tx = Transaction.openOuter()) { h.assertTrue(port.insert(ItemVariant.of(Items.DIAMOND), 7, tx) == 7, "Native ITEM filter did not accept ordinary items"); }
        h.assertTrue(tile.getItem(Direction.EAST.ordinal()).isEmpty(), "Aborted filter insertion persisted");
        try (var tx = Transaction.openOuter()) { port.insert(ItemVariant.of(Items.DIAMOND), 7, tx); tx.commit(); }
        var saved = tile.saveCustomOnly(level.registryAccess());
        var restored = new TileGeneticFilter(pos, tile.getBlockState()); restored.setLevel(level); restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), saved));
        h.assertTrue(restored.getLogic().getRule(Direction.EAST) == DefaultFilterRuleType.ITEM && restored.getItem(Direction.EAST.ordinal()).getCount() == 7, "Filter rules or buffered items were lost on save");
        h.runAfterDelay(20, () -> {
            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(pos.east());
            h.assertTrue(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 7 && tile.getItem(Direction.EAST.ordinal()).isEmpty(), "Real filter ticker failed to forward items exactly once");
            h.succeed();
        });
    }
    @GameTest public void filterSnapshotsAndSpeciesIdsSurviveReload(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2)); var level = h.getLevel();
        level.setBlockAndUpdate(pos, SortingBlocks.FILTER.defaultState()); var tile = (TileGeneticFilter) level.getBlockEntity(pos);
        var species = forestry.core.platform.util.SpeciesUtil.getBeeSpecies(forestry.api.apiculture.ForestryBeeSpecies.FOREST);
        tile.getLogic().setRule(Direction.EAST, DefaultFilterRuleType.ANYTHING);
        tile.getLogic().setGenomeFilter(Direction.EAST, 0, true, species);
        var packet = tile.getLogic().createGuiUpdatePacket(pos);
        tile.getLogic().setRule(Direction.EAST, DefaultFilterRuleType.CLOSED);
        tile.getLogic().setGenomeFilter(Direction.EAST, 0, true, null);
        NetworkUtil.applyPayload(level.registryAccess(), packet.payload(), tile.getLogic()::readGuiData);
        h.assertTrue(tile.getLogic().getRule(Direction.EAST) == DefaultFilterRuleType.ANYTHING && tile.getLogic().getGenomeFilter(Direction.EAST, 0, true).id().equals(species.id()), "Filter packet retained mutable source arrays");
        var bytes = packet.payload(); bytes[0] ^= 0x7f;
        NetworkUtil.applyPayload(level.registryAccess(), packet.payload(), tile.getLogic()::readGuiData);
        h.assertTrue(!tile.getLogic().setGenomeFilter(Direction.UP, -1, true, species), "Negative filter index accepted");
        h.succeed();
    }
    @GameTest public void worktableServerRecipesPermissionsSnapshotsAndSave(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(3, 2, 3));
        level.setBlockAndUpdate(pos, WorktableBlocks.WORKTABLE.defaultState()); var tile = (WorktableTile) level.getBlockEntity(pos);
        var player = net.fabricmc.fabric.api.entity.FakePlayer.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("6ae6b7cd-d55d-4fc7-bf42-336b28a408ed"), "F2Worktable"));
        player.setPos(pos.getX(), pos.getY(), pos.getZ()); player.containerMenu = new WorktableMenu(42, player.getInventory(), tile);
        var matrix = new WorktableCraftingContainer(); matrix.setItem(0, new ItemStack(Items.OAK_LOG));
        var pattern = forestry.core.platform.util.InventoryUtil.getStacks(matrix);
        PacketWorktableRecipeRequest.handle(new PacketWorktableRecipeRequest(41, pos, pattern, Identifier.parse("minecraft:diamond_block")), player);
        h.assertTrue(tile.getCurrentRecipe() == null, "Wrong menu request changed recipe");
        PacketWorktableRecipeRequest.handle(new PacketWorktableRecipeRequest(42, pos, pattern, Identifier.parse("minecraft:diamond_block")), player);
        h.assertTrue(tile.getCurrentRecipe() != null && tile.getCurrentRecipe().getOutputIcon(level).is(Items.OAK_PLANKS), "Server trusted forged output recipe ID");
        var update = new PacketWorktableRecipeUpdate(tile);
        tile.setItem(0, new ItemStack(Items.OAK_LOG, 2));
        h.assertTrue(tile.onCraftingStart(player) && tile.getItem(0).getCount() == 1, "Worktable consumed wrong ingredients");
        tile.onCraftingComplete(player); tile.getMemory().toggleLock(1, 0); tile.getMemory().toggleLock(2, -1);
        var memory = new PacketWorktableMemoryUpdate(tile);
        tile.getMemory().toggleLock(3, 0);
        NetworkUtil.applyPayload(level.registryAccess(), memory.payload(), buffer -> h.assertTrue(new RecipeMemory(buffer).isLocked(0), "Memory packet retained a mutable recipe"));
        var restored = new WorktableTile(pos, tile.getBlockState()); restored.setLevel(level); restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tile.saveCustomOnly(level.registryAccess())));
        h.assertTrue(restored.getMemory().getRecipe(0).getOutputIcon(level).is(Items.OAK_PLANKS) && restored.getItem(0).getCount() == 1, "Worktable inventory or recipe memory failed save/load");
        NetworkUtil.applyPayload(level.registryAccess(), update.payload(), buffer -> {
            var recipe = NetworkUtil.<MemorizedRecipe>readStreamable(buffer, MemorizedRecipe::new);
            h.assertTrue(recipe.getOutputIcon(level).is(Items.OAK_PLANKS), "Recipe update changed after crafting");
        });
        player.containerMenu = player.inventoryMenu; h.succeed();
    }
}
