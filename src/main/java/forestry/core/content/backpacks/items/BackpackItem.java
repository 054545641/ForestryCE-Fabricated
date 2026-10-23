package forestry.core.content.backpacks.items;

import com.google.common.base.Preconditions;
import forestry.api.core.backpacks.BackpackStowEvent;
import forestry.api.core.backpacks.EnumBackpackType;
import forestry.api.core.backpacks.IBackpackDefinition;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.inventory.ItemHandlerInventoryManipulator;
import forestry.core.platform.inventory.ItemInventory;
import forestry.core.platform.inventory.StandardStackFilters;
import forestry.core.platform.item.WithScreenItem;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.content.backpacks.BackpackMode;
import forestry.core.content.backpacks.gui.ContainerBackpack;
import forestry.core.content.backpacks.inventory.BackpackInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import forestry.core.features.CoreDataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import javax.annotation.Nullable;
import java.util.List;

public class BackpackItem extends WithScreenItem implements ITintedItem {
	public static final int SLOTS_BACKPACK_DEFAULT = 15;
	public static final int SLOTS_BACKPACK_WOVEN = 45;
	public static final int SLOTS_BACKPACK_APIARIST = 128;
	private final IBackpackDefinition definition;
	private final EnumBackpackType type;

	public BackpackItem(IBackpackDefinition definition, EnumBackpackType type, Item.Properties properties) {
		super(properties);

		this.definition = definition;
		this.type = type;
	}

	protected static void writeParentIdentity(ItemStack stack, InteractionHand hand, RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(hand == InteractionHand.MAIN_HAND);
        buffer.writeInt(java.util.Objects.requireNonNull(stack.get(CoreDataComponents.ITEM_INVENTORY_UID.get())));
    }
    public static ItemStack readParentIdentity(Player player, net.minecraft.network.FriendlyByteBuf buffer) {
        var stack = player.getItemInHand(buffer.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        int uid = buffer.readInt();
        if (!(stack.getItem() instanceof BackpackItem)) throw new IllegalArgumentException("Opened backpack is no longer held");
        stack.set(CoreDataComponents.ITEM_INVENTORY_UID.get(), uid);
        return stack;
    }
    public IBackpackDefinition getDefinition() {
		return this.definition;
	}

	@Override
	protected void writeContainerData(ServerPlayer player, ItemStack stack, InteractionHand hand, RegistryFriendlyByteBuf buffer) {
		writeParentIdentity(stack, hand, buffer);
        NetworkUtil.writeEnum(buffer, this.type == EnumBackpackType.WOVEN ? ContainerBackpack.Size.T2 : ContainerBackpack.Size.DEFAULT);
	}

	@Override
	public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (!playerIn.isShiftKeyDown()) {
			return super.use(worldIn, playerIn, handIn);
		} else {
			ItemStack heldItem = playerIn.getItemInHand(handIn);
			if (!worldIn.isClientSide()) switchMode(heldItem);
			return InteractionResult.SUCCESS;
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		if (getInventoryHit(level, ctx.getClickedPos(), ctx.getClickedFace()) != null) {
			return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
		}
		return InteractionResult.PASS;
	}

	public InteractionResult useBeforeBlock(ItemStack stack, UseOnContext context) {
		Player player = context.getPlayer();
		// We only do this when shift is clicked
		if (player != null && player.isShiftKeyDown()) {
			ItemStack heldItem = player.getItemInHand(context.getHand());
			return evaluateTileHit(heldItem, player, context.getLevel(), context.getClickedPos(), context.getClickedFace()) ? InteractionResult.PASS : InteractionResult.FAIL;
		}
		return InteractionResult.PASS;
	}

	public static void tryStowing(Player player, ItemStack backpackStack, ItemStack stack) {
		if (getMode(backpackStack) == BackpackMode.LOCKED) {
			return;
		}

		BackpackItem backpack = (BackpackItem) backpackStack.getItem();
		ItemInventory inventory = new BackpackInventory(player, backpack.getBackpackSize(), backpackStack);

		if (forestry.api.core.backpacks.BackpackEvent.dispatch(new BackpackStowEvent(player, backpack.getDefinition(), inventory, stack)).isCanceled()) {
			return;
		}
		if (stack.isEmpty()) {
			return;
		}

		Storage<ItemVariant> itemHandler = inventory.getItemHandler();
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openNested(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.getCurrentUnsafe())) {
            new forestry.core.platform.transfer.StackCountParticipant(stack).updateSnapshots(tx);
            long inserted = itemHandler.insert(ItemVariant.of(stack), stack.getCount(), tx);
            stack.shrink(Math.toIntExact(inserted));
            tx.commit();
        }
	}

	private static void switchMode(ItemStack itemstack) {
		getMode(itemstack).next(ForestryConfig.SERVER.enableBackpackResupply.get())
			.write(itemstack, CoreDataComponents.BACKPACK_MODE.get());
	}

	@Nullable
	private static Storage<ItemVariant> getInventoryHit(Level world, BlockPos pos, Direction side) {
		BlockEntity targeted = TileUtil.getTile(world, pos);
		return TileUtil.getInventoryFromTile(targeted, side);
	}

	private boolean evaluateTileHit(ItemStack stack, Player player, Level world, BlockPos pos, Direction side) {

		// Shift right-clicking on an inventory tile will attempt to transfer
		// items contained in the backpack
		Storage<ItemVariant> inventory = getInventoryHit(world, pos, side);
		// Process only inventories
		if (inventory != null) {

			// Must have inventory slots
			if (!inventory.iterator().hasNext()) {
				return true;
			}

			if (!world.isClientSide()) {
				// Create our own backpack inventory
				BackpackInventory backpackInventory = new BackpackInventory(player, getBackpackSize(), stack);

				BackpackMode mode = getMode(stack);
				if (mode == BackpackMode.RECEIVE) {
					receiveFromChest(backpackInventory, inventory);
				} else {
					transferToChest(backpackInventory, inventory);
				}
			}

			return true;
		}

		return false;
	}

	private static void transferToChest(BackpackInventory backpackInventory, Storage<ItemVariant> target) {
		ItemHandlerInventoryManipulator manipulator = new ItemHandlerInventoryManipulator(backpackInventory.getItemHandler());
		manipulator.transferStacks(target, StandardStackFilters.ALL);
	}

	private void receiveFromChest(BackpackInventory backpackInventory, Storage<ItemVariant> target) {
		ItemHandlerInventoryManipulator manipulator = new ItemHandlerInventoryManipulator(target);
		manipulator.transferStacks(backpackInventory.getItemHandler(), this.definition.getFilter());
	}

	public int getBackpackSize() {
		return getSlotsForType(this.type);
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);

		int occupied = ItemInventory.getOccupiedSlotCount(itemstack);

		BackpackMode mode = getMode(itemstack);
		String infoKey = mode.getTranslationKey();
		if (infoKey != null) {
			list.add(Component.translatable(infoKey).withStyle(ChatFormatting.GRAY));
		}
		list.add(Component.translatable("for.gui.slots", String.valueOf(occupied), String.valueOf(getBackpackSize())).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public Component getName(ItemStack itemstack) {
		return this.definition.getName(itemstack);
	}

	@Override
	public int getColorFromItemStack(ItemStack itemstack, int layer) {
		if (layer == 0) {
			return this.definition.getPrimaryColour();
		} else if (layer == 1) {
			return this.definition.getSecondaryColour();
		} else {
			return 0xffffff;
		}
	}

	private static int getSlotsForType(EnumBackpackType type) {
		return switch (type) {
			case NATURALIST -> SLOTS_BACKPACK_APIARIST;
			case WOVEN -> SLOTS_BACKPACK_WOVEN;
			case NORMAL -> SLOTS_BACKPACK_DEFAULT;
		};
	}

	public static BackpackMode getMode(ItemStack backpack) {
		if (!(backpack.getItem() instanceof BackpackItem)) {
			return BackpackMode.NEUTRAL;
		}

		return BackpackMode.read(backpack, CoreDataComponents.BACKPACK_MODE.get());
	}

	public static EnumBackpackType getType(ItemStack backpack) {
		Preconditions.checkArgument(backpack.getItem() instanceof BackpackItem, "Item must be a backpack");
		return ((BackpackItem) backpack.getItem()).type;
	}


	@Override
	@Nullable
	public AbstractContainerMenu getContainer(int containerId, Player player, ItemStack heldItem) {
		Item item = heldItem.getItem();
		if (!(item instanceof BackpackItem backpack)) {
			return null;
		}
		return switch (backpack.type) {
			case NORMAL -> new ContainerBackpack(containerId, player, ContainerBackpack.Size.DEFAULT, heldItem);
			case WOVEN -> new ContainerBackpack(containerId, player, ContainerBackpack.Size.T2, heldItem);
			default -> null;
		};
	}
}
