package forestry.arboriculture.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.arboriculture.IToolGrafter;
import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.IFruitBearer;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.leaves.DefaultFruitLeavesBlock;
import forestry.core.platform.util.SpeciesUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class GrafterLootModifier extends LootModifier {
	public static final MapCodec<GrafterLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, GrafterLootModifier::new));

	public GrafterLootModifier(LootItemCondition[] conditionsIn) {
		this(conditionsIn, IGlobalLootModifier.DEFAULT_PRIORITY);
	}

	public GrafterLootModifier(LootItemCondition[] conditionsIn, int priority) {
		super(conditionsIn, priority);
	}

	@Override
	protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
		if (state == null || !state.is(BlockTags.LEAVES)) {
			return generatedLoot;
		}
		// Durability must be applied to the actual mutable harvesting stack.
		if (!(context.getOptionalParameter(LootContextParams.TOOL) instanceof ItemStack harvestingTool) || harvestingTool.isEmpty()) {
			return generatedLoot;
		}
		Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
		if (!(entity instanceof ServerPlayer player)) {
			return generatedLoot;
		}
		if (generatedLoot.stream().noneMatch((stack) -> stack.is(ItemTags.SAPLINGS))) {
			handleLoot(generatedLoot, player, harvestingTool, state, context);
		}
		ItemStack beforeDamage = harvestingTool.copy();
		harvestingTool.hurtAndBreak(1, context.getLevel(), player, item -> {
			EventHooks.onPlayerDestroyItem(player, beforeDamage, InteractionHand.MAIN_HAND);
			player.onEquippedItemBroken(item, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
		});
		return generatedLoot;
	}

	public void handleLoot(List<ItemStack> generatedLoot, Player player, ItemStack harvestingTool, BlockState state, LootContext context) {
		Level world = player.level();
		BlockEntity tileEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		ITree tree = getTree(state, tileEntity);
		if (tree == null) {
			return;
		}
		Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
		if (origin == null) {
			return;
		}
		BlockPos pos = BlockPos.containing(origin);
		Item item = harvestingTool.getItem();
		float saplingModifier = 1.0f;
		if (item instanceof IToolGrafter) {
			saplingModifier = ((IToolGrafter) item).getSaplingModifier(harvestingTool, world, player, pos);
		}
		List<ITree> saplings = tree.getSaplings(world, pos, player.getGameProfile(), saplingModifier);
		for (ITree sapling : saplings) {
			if (sapling != null) {
				generatedLoot.add(sapling.createStack(TreeLifeStage.SAPLING));
			}
		}
		if (tileEntity instanceof IFruitBearer bearer) {
			generatedLoot.addAll(bearer.pickFruit(harvestingTool));
		}
		if (state.getBlock() instanceof DefaultFruitLeavesBlock) {
			IGenome genome = tree.getGenome();
			IFruit fruitProvider = genome.resolveActive(TreeChromosomes.FRUIT);
			if (fruitProvider.isFruitLeaf()) {
				generatedLoot.addAll(tree.produceStacks(world, pos, Integer.MAX_VALUE));
			}
		}
	}

	@Nullable
	private ITree getTree(BlockState state, @Nullable BlockEntity entity) {
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
		ITree tree = type.getVanillaIndividual(state);
		if (tree != null || entity == null) {
			return tree;
		} else {
			return type.getTree(entity);
		}
	}

	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
