package forestry.arboriculture.loot;

import forestry.api.arboriculture.IToolGrafter;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.IFruitBearer;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class FabricGrafterLoot {
    public static void initialize() {
        LootTableEvents.MODIFY_DROPS.register((key, context, drops) -> {
            var state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
            var toolParameter = context.getOptionalParameter(LootContextParams.TOOL);
            var origin = context.getOptionalParameter(LootContextParams.ORIGIN);
            if (state == null || !state.is(BlockTags.LEAVES) || !(toolParameter instanceof net.minecraft.world.item.ItemStack tool) || !(tool.getItem() instanceof IToolGrafter grafter)
                    || origin == null || !(context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return;
            var harvestTool = tool.copy();
            tool.hurtAndBreak(1, context.getLevel(), player, item -> player.onEquippedItemBroken(item, net.minecraft.world.entity.EquipmentSlot.MAINHAND));
            forestry.api.arboriculture.genetics.ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
            var tree = type.getVanillaIndividual(state);
            var entity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (tree == null && entity != null) tree = type.getTree(entity);
            if (tree == null || drops.stream().anyMatch(stack -> stack.is(ItemTags.SAPLINGS))) return;
            var pos = BlockPos.containing(origin);
            for (var sapling : tree.getSaplings(context.getLevel(), pos, player.getGameProfile(), grafter.getSaplingModifier(harvestTool, context.getLevel(), player, pos))) {
                if (sapling != null) drops.add(sapling.createStack(TreeLifeStage.SAPLING));
            }
            if (entity instanceof IFruitBearer fruit) drops.addAll(fruit.pickFruit(harvestTool));
            forestry.api.arboriculture.genetics.IFruit fruit = tree.getGenome().resolveActive(forestry.api.core.genetics.alleles.TreeChromosomes.FRUIT);
            if (state.getBlock() instanceof forestry.arboriculture.leaves.DefaultFruitLeavesBlock && fruit.isFruitLeaf()) {
                drops.addAll(tree.produceStacks(context.getLevel(), pos, Integer.MAX_VALUE));
            }
        });
    }
}
