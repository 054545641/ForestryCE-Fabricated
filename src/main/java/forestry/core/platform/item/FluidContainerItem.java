package forestry.core.platform.item;

import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.models.FluidContainerModel;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import javax.annotation.Nullable;

public class FluidContainerItem extends ItemForestry implements ITintedItem {
	private final FluidContainerType type;

	public FluidContainerItem(FluidContainerType type, Properties properties) {
		super(properties);
		this.type = type;
	}

	public static FluidStack getContained(ItemStack itemStack) {
		return itemStack.getOrDefault(CoreDataComponents.FLUID_CONTENT, SimpleFluidContent.EMPTY).copy();
	}

	@Override
	public Component getName(ItemStack stack) {
		FluidStack fluid = getContained(stack);
		if (!fluid.isEmpty()) {
			String exactTranslationKey = "item.forestry." + this.type.getSerializedName() + '.' + ModUtil.getRegistryName(fluid.getFluid());
			return Translator.tryTranslate(exactTranslationKey, () -> {
				String grammarKey = "item.forestry." + this.type.getSerializedName() + ".grammar";
				return Component.translatable(grammarKey, fluid.getHoverName());
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
		FluidStack contained = getContained(itemStack);
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
			var handler = createFluidHandler(ItemAccess.forPlayerInteraction(player, handIn).oneByOne());
			if (!FluidUtil.tryPickupFluid(handler, player, world, target.getBlockPos(), target.getDirection()).isEmpty()) {
				return InteractionResult.SUCCESS;
			}
			return super.use(world, player, handIn);
		}
	}

	public FluidHandlerItemForestry createFluidHandler(ItemAccess access) {
		return new FluidHandlerItemForestry(access, CoreDataComponents.FLUID_CONTENT.get(), this.type);
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		return tintIndex == 1 ? net.neoforged.neoforge.client.color.item.FluidContentsTint.INSTANCE.calculate(stack, null, null) : -1;
	}
}
