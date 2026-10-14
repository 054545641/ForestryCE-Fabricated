package forestry.lepidopterology.butterflies;

import forestry.api.core.genetics.ISpeciesType;
import forestry.api.lepidopterology.IButterflyNursery;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import forestry.api.lepidopterology.genetics.IButterfly;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.engine.genetics.ItemGE;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.util.BlockUtil;
import forestry.core.platform.util.EntityUtil;
import forestry.core.platform.util.NBTUtilForestry;
import forestry.core.platform.util.SpeciesUtil;
import forestry.arboriculture.trees.TreeUtil;
import forestry.lepidopterology.entities.EntityButterfly;
import forestry.lepidopterology.features.LepidopterologyEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ItemButterflyGE extends ItemGE implements ITintedItem {
	public static final String NBT_AGE = "Age";

	public ItemButterflyGE(ButterflyLifeStage stage, Properties properties) {
		super(properties, stage);
	}

	@Override
	protected IButterflySpecies getSpecies(ItemStack stack) {
		return IIndividualHandlerItem.getSpecies(stack, SpeciesUtil.BUTTERFLY_TYPE.get());
	}

	@Override
	public ISpeciesType<?, ?> getType() {
		return SpeciesUtil.BUTTERFLY_TYPE.get();
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
		if (this.stage != ButterflyLifeStage.BUTTERFLY) {
			return false;
		}
		Level level = entityItem.level();
		if (level.isClientSide() || entityItem.tickCount < 80) {
			return false;
		}
		if (level.getRandom().nextInt(24) != 0) {
			return false;
		}

		IButterfly butterfly = (IButterfly) IIndividualHandlerItem.getIndividual(entityItem.getItem());
		if (butterfly == null) {
			return false;
		}

		if (!butterfly.canTakeFlight(level, entityItem.getX(), entityItem.getY(), entityItem.getZ())) {
			return false;
		}

		if (EntityUtil.spawnEntity(entityItem.level(), EntityButterfly.create(LepidopterologyEntities.BUTTERFLY.entityType(), entityItem.level(), butterfly, entityItem.blockPosition()), entityItem.getX(), entityItem.getY(), entityItem.getZ()) == null) {
			return false;
		}
		ItemStack remainder = entityItem.getItem().copy();
		remainder.shrink(1);
		if (remainder.isEmpty()) {
			entityItem.discard();
		} else {
			entityItem.setItem(remainder);
		}
		return true;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();
		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}

		ItemStack stack = player.getItemInHand(context.getHand());

		IButterfly flutter = (IButterfly) IIndividualHandlerItem.getIndividual(stack);

		BlockState blockState = level.getBlockState(pos);
		if (this.stage == ButterflyLifeStage.COCOON) {
			pos = SpeciesUtil.BUTTERFLY_TYPE.get().plantCocoon(level, pos, flutter, getAge(stack), true);
			if (pos != null) {
				BlockUtil.sendPlaceSound(level, pos, blockState);

				if (!player.isCreative()) {
					stack.shrink(1);
				}
				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.PASS;
			}
		} else if (this.stage == ButterflyLifeStage.CATERPILLAR) {
			IButterflyNursery nursery = TreeUtil.getOrCreateNursery(level, pos, true);
			if (nursery != null) {
				if (!nursery.canNurse(flutter)) {
					return InteractionResult.PASS;
				}

				nursery.setCaterpillar(flutter);

				BlockUtil.sendDestroyEffects(level, pos, blockState);

				if (!player.isCreative()) {
					stack.shrink(1);
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		} else {
			return InteractionResult.PASS;
		}
	}

	public static int getAge(ItemStack cocoon) {
		if (cocoon.isEmpty()) {
			return 0;
		}
		if (SpeciesUtil.BUTTERFLY_TYPE.get().getLifeStage(cocoon) != ButterflyLifeStage.COCOON) {
			return 0;
		}
		CompoundTag tagCompound = NBTUtilForestry.getItemStackTag(cocoon);
		if (tagCompound == null) {
			return 0;
		}
		return Mth.clamp(tagCompound.getIntOr(NBT_AGE, 0), 0, 2);
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		if (tintIndex == 1 && hasIndividual(stack)) {
			IButterflySpecies species = getSpecies(stack);
			return species.getSerumColor();
		}
		return 0xffffff;
	}
}
