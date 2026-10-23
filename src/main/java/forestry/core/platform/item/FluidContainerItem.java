package forestry.core.platform.item;

import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.features.CoreDataComponents;

import forestry.core.platform.util.ModUtil;
import forestry.core.platform.util.Translator;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import forestry.api.core.fluids.FluidVolume;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import javax.annotation.Nullable;

public class FluidContainerItem extends ItemForestry implements ITintedItem {
	private final FluidContainerType type;

	public FluidContainerItem(FluidContainerType type, Properties properties) {
		super(properties);
		this.type = type;
	}

	public static FluidVolume getContained(ItemStack itemStack) {
		return itemStack.getOrDefault(CoreDataComponents.FLUID_CONTENT.get(), FluidVolume.EMPTY).copy();
	}

	@Override
	public Component getName(ItemStack stack) {
		FluidVolume fluid = getContained(stack);
		if (!fluid.isEmpty()) {
			String exactTranslationKey = "item.forestry." + this.type.getSerializedName() + '.' + ModUtil.getRegistryName(fluid.getFluid());
			return Translator.tryTranslate(exactTranslationKey, () -> {
				String grammarKey = "item.forestry." + this.type.getSerializedName() + ".grammar";
				return Component.translatable(grammarKey, FluidVariantAttributes.getName(fluid.variant()));
			});
		} else {
			String translationKey = "item.forestry." + this.type.getSerializedName() + ".empty";
			return Component.translatable(translationKey);
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
		DrinkProperties drinkProperties = getDrinkProperties(stack);
		if (drinkProperties != null) {
			if (entityLiving instanceof Player player && !player.isCreative()) {
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}

				if (!worldIn.isClientSide()) {
					FoodData foodStats = player.getFoodData();
					foodStats.eat(drinkProperties.getHealAmount(), drinkProperties.getSaturationModifier());
					worldIn.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, worldIn.getRandom().nextFloat() * 0.1F + 0.9F);
				}

				player.awardStat(Stats.ITEM_USED.get(this));
			}
		}
		return stack;
	}

	@Nullable
	protected DrinkProperties getDrinkProperties(ItemStack itemStack) {
		FluidVolume contained = getContained(itemStack);
		if (!contained.isEmpty()) {
			ForestryFluids definition = ForestryFluids.getFluidDefinition(contained.getFluid());
			if (definition != null) {
				return definition.getDrinkProperties();
			}
		}
		return null;
	}

	@Override
	public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
		DrinkProperties drinkProperties = getDrinkProperties(itemstack);
		if (drinkProperties != null) {
			return drinkProperties.getMaxItemUseDuration();
		} else {
			return super.getUseDuration(itemstack, entity);
		}
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack itemstack) {
		DrinkProperties drinkProperties = getDrinkProperties(itemstack);
		if (drinkProperties != null) {
			return ItemUseAnimation.DRINK;
		} else {
			return ItemUseAnimation.NONE;
		}
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand handIn) {
		ItemStack heldItem = player.getItemInHand(handIn);
		DrinkProperties drinkProperties = getDrinkProperties(heldItem);
		if (drinkProperties != null) {
			if (player.canEat(false)) {
				player.startUsingItem(handIn);
				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.FAIL;
			}
		} else {
			BlockHitResult target = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);
			if (target.getType() != HitResult.Type.BLOCK) {
				return InteractionResult.PASS;
			}
			if (!world.mayInteract(player, target.getBlockPos())
				|| !player.mayUseItemAt(target.getBlockPos().relative(target.getDirection()), target.getDirection(), heldItem)) {
				return InteractionResult.FAIL;
			}
			if (world.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
            // BucketPickup mutates the world immediately; item use owns the root transaction.
            if (Transaction.getCurrentUnsafe() != null) return InteractionResult.FAIL;
            var pos = target.getBlockPos(); var state = world.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup && state.getFluidState().isSource()) {
                var handler = createFluidHandler(ContainerItemContext.forPlayerInteraction(player, handIn));
                var fluid = FluidVariant.of(state.getFluidState().getType());
                try (var tx = Transaction.openOuter()) {
                    if (handler.insert(fluid, FluidConstants.BUCKET, tx) == FluidConstants.BUCKET) {
                        var picked = pickup.pickupBlock(player, world, pos, state);
                        if (!picked.isEmpty()) {
                            tx.commit();
                            player.awardStat(Stats.ITEM_USED.get(this));
                            world.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP, pos);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
			return super.use(world, player, handIn);
		}
	}

	public FluidHandlerItemForestry createFluidHandler(ContainerItemContext access) {
		return new FluidHandlerItemForestry(access, this.type);
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		return tintIndex == 1 && !getContained(stack).isEmpty() ? forestry.core.platform.client.ClientsideCode.getFluidColor(getContained(stack).getFluid()) : -1;
	}
}
