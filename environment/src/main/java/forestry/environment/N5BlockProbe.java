package forestry.environment;

import forestry.apiculture.bees.BlockWax;
import forestry.arboriculture.leaves.BlockExtendedLeaves;
import forestry.core.content.decorative.*;
import forestry.core.content.lighting.*;
import forestry.core.platform.block.IBlockTooltip;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.tile.DropAwareBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Production lifecycle classes; native fixture entities supply inventory and callback observations. */
public final class N5BlockProbe {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, EnvironmentProbe.ID);
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnvironmentProbe.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, EnvironmentProbe.ID);
    private static <B extends Block> Supplier<B> block(String name, Function<BlockBehaviour.Properties,B> factory) {
        return BLOCKS.register(name, id -> factory.apply(BlockBehaviour.Properties.of().noLootTable().setId(ResourceKey.create(Registries.BLOCK,id))));
    }
    public static final Supplier<BlockExtendedLeaves> LEAVES = block("probe_leaves", p -> new BlockExtendedLeaves(p.randomTicks()));
    public static final Supplier<RemovalBlock> REMOVAL = block("probe_removal", RemovalBlock::new);
    public static final Supplier<BlockEntityType<RemovalEntity>> REMOVAL_TYPE = TYPES.register("probe_removal", () -> new BlockEntityType<>(RemovalEntity::new, REMOVAL.get()));
    public static final Supplier<BlockBigCandle> BIG = block("probe_big", p -> new BlockBigCandle(p, BlockTypeBigCandle.NORMAL));
    public static final Supplier<BlockJumboCandle> JUMBO = block("probe_jumbo", p -> new BlockJumboCandle(p, BlockTypeJumboCandle.NORMAL));
    public static final Supplier<BlockWaterloggableTorch> TORCH = block("probe_torch", BlockWaterloggableTorch::new);
    public static final Supplier<BlockWaterloggableWallTorch> WALL_TORCH = block("probe_wall_torch", BlockWaterloggableWallTorch::new);
    public static final Supplier<BlockWax> WAX = block("probe_wax", p -> new BlockWax(p, true, () -> Fluids.WATER));
    public static final Supplier<BlockWax> REFRACTORY = block("probe_refractory", p -> new BlockWax(p, false, null));
    public static final Supplier<BlockPlywoodBlock> PLYWOOD = block("probe_plywood", BlockPlywoodBlock::new);
    public static final Supplier<TooltipBlock> TOOLTIP_BLOCK = block("probe_tooltip", TooltipBlock::new);
    public static final Supplier<Item> TOOLTIP_ITEM = ITEMS.register("probe_tooltip", id -> new ItemBlockForestry<>(TOOLTIP_BLOCK.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));
    public static final Supplier<Item> LEGACY_ITEM = ITEMS.register("probe_legacy_tooltip", id -> new ItemForestry(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))) {
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal("Legacy item callback"));
        }
    });

    public static void removal(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1,2,1));
        level.setBlock(pos, REMOVAL.get().defaultBlockState(), 3);
        var entity = (RemovalEntity) level.getBlockEntity(pos);
        ItemStack cargo = new ItemStack(Items.DIAMOND, 7);
        cargo.set(DataComponents.CUSTOM_NAME, Component.literal("Removal cargo"));
        entity.setItem(0,cargo);
        level.setBlock(pos, level.getBlockState(pos).setValue(BlockStateProperties.POWERED,true), 3);
        h.assertTrue(level.getBlockEntity(pos) == entity && entity.extraDrops == 0 && entity.getItem(0).getCount() == 7,
            "Property-only update retains entity and inventory");
        level.removeBlock(pos,false);
        h.assertTrue(entity.extraDrops == 1 && entity.sawInventoryEmpty && entity.sawRegistered && entity.isRemoved(),
            "Native inventory drain precedes additional callback, before entity removal");
        var drops = level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum() == 7,
            "Removal drops inventory exactly once");
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.DIAMOND)).allMatch(e -> Component.literal("Removal cargo").equals(e.getItem().getCustomName())),
            "Dropped stacks retain components");
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.EMERALD)).mapToInt(e -> e.getItem().getCount()).sum() == 1,
            "Additional drop callback runs once");
        level.removeBlock(pos,false);
        h.assertTrue(entity.extraDrops == 1,"Repeated removal of air does not repeat callbacks");
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        level.setBlock(pos,REMOVAL.get().defaultBlockState(),3);
        var suppressed = (RemovalEntity)level.getBlockEntity(pos);
        suppressed.setItem(0,new ItemStack(Items.DIAMOND,2));
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3 | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        h.assertTrue(suppressed.extraDrops == 0 && suppressed.getItem(0).getCount() == 2
            && level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).isEmpty(),"Native suppression flag skips all drops");
        h.succeed();
    }

    public static void leaves(GameTestHelper h) {
        var level = h.getLevel();
        var start = h.absolutePos(new BlockPos(1,3,1));
        level.setBlock(start,Blocks.OAK_LOG.defaultBlockState(),18);
        for(int x=1;x<=11;x++) {
            var pos = start.east(x);
            level.setBlock(pos,LEAVES.get().defaultBlockState().setValue(BlockExtendedLeaves.DISTANCE,Math.min(x,7)),18);
            level.scheduleTick(pos,LEAVES.get(),1);
        }
        h.runAfterDelay(3, () -> {
            var supported = level.getBlockState(start.east(10));
            h.assertTrue(supported.getValue(BlockExtendedLeaves.SUPPORTED),"Four-leaf extension supports distance ten");
            var outside = level.getBlockState(start.east(11));
            h.assertFalse(outside.getValue(BlockExtendedLeaves.SUPPORTED),"Distance eleven is outside support range");
            var ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
            var restored = BlockState.CODEC.parse(ops,BlockState.CODEC.encodeStart(ops,supported).getOrThrow()).getOrThrow();
            h.assertTrue(restored == supported,"Support and vanilla leaf properties persist through state codec");
            supported.randomTick(level,start.east(10),level.getRandom());
            h.assertTrue(level.getBlockState(start.east(10)).is(LEAVES.get()),"Supported leaves resist decay");
            outside.randomTick(level,start.east(11),level.getRandom());
            h.assertTrue(level.getBlockState(start.east(11)).isAir(),"Unsupported leaves decay through native random tick");
            level.removeBlock(start,false);
        });
        h.runAfterDelay(35, () -> {
            var pos = start.east(10);
            var state = level.getBlockState(pos);
            h.assertTrue(state.isAir() || !state.getValue(BlockExtendedLeaves.SUPPORTED),"Removing log propagates loss of extended support through scheduled ticks");
            var persistent = LEAVES.get().defaultBlockState().setValue(BlockExtendedLeaves.PERSISTENT,true);
            level.setBlock(pos,persistent,18);
            persistent.randomTick(level,pos,level.getRandom());
            h.assertTrue(level.getBlockState(pos).is(LEAVES.get()),"Player-placed persistent leaves do not decay");
            h.succeed();
        });
    }

    public static void hooks(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1,2,1));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        var candle = BIG.get();
        level.setBlock(pos,candle.defaultBlockState(),3);
        var charge = new ItemStack(Items.FIRE_CHARGE,2);
        player.setItemInHand(InteractionHand.MAIN_HAND,charge);
        level.getBlockState(pos).useItemOn(charge,level,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(level.getBlockState(pos).getValue(BlockBigCandle.LIT) && charge.getCount() == 1,"Ignition consumes one charge");
        level.getBlockState(pos).useWithoutItem(level,player,hit);
        h.assertTrue(level.getBlockState(pos).getValue(BlockBigCandle.LIT),"Unrelated held-item fallback cannot extinguish candle");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        level.getBlockState(pos).useWithoutItem(level,player,hit);
        h.assertFalse(level.getBlockState(pos).getValue(BlockBigCandle.LIT),"Native empty-hand entry extinguishes candle");
        level.setBlock(pos,candle.defaultBlockState().setValue(BlockBigCandle.LIT,true),3);
        h.assertTrue(candle.placeLiquid(level,pos,level.getBlockState(pos),Fluids.WATER.defaultFluidState()),"Candle accepts water");
        h.assertTrue(level.getBlockState(pos).getValue(BlockBigCandle.WATERLOGGED) && !level.getBlockState(pos).getValue(BlockBigCandle.LIT),"Water placement extinguishes immediately");
        h.assertTrue(level.getFluidTicks().hasScheduledTick(pos,Fluids.WATER),"Water placement schedules a fluid tick");
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,TORCH.get().defaultBlockState().setValue(BlockWaterloggableTorch.WATERLOGGED,true),3);
        level.removeBlock(pos.below(),false);
        h.assertTrue(!level.getBlockState(pos).is(TORCH.get()),"Waterlogged torch delegates support loss to native updateShape");
        level.setBlock(pos,REFRACTORY.get().defaultBlockState(),3);
        h.assertFalse(level.getBlockState(pos).onCaughtFire(level,pos,Direction.UP,null),"Refractory wax rejects ignition");
        h.assertTrue(level.getBlockState(pos).is(REFRACTORY.get()),"Rejected ignition leaves refractory block intact");
        level.setBlock(pos,WAX.get().defaultBlockState(),3);
        h.assertTrue(level.getBlockState(pos).onCaughtFire(level,pos,Direction.UP,null) && level.getFluidState(pos).is(Fluids.WATER),"Melt callback returns success and installs supplied fluid");
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,PLYWOOD.get().defaultBlockState(),3);
        h.assertTrue(level.getBlockState(pos).onCaughtFire(level,pos,Direction.UP,null) && level.getBlockState(pos).is(Blocks.FIRE),"Plywood fire callback retains transformation");
        var jumbo = JUMBO.get();
        level.setBlock(pos,jumbo.defaultBlockState().setValue(BlockJumboCandle.LIT,true),3);
        level.setBlock(pos.above(),jumbo.defaultBlockState(),3);
        h.assertTrue(level.getBlockState(pos).getValue(BlockJumboCandle.SHAPE) == BlockJumboCandle.CandleShape.BOTTOM
            && !level.getBlockState(pos).getValue(BlockJumboCandle.LIT),"Jumbo neighbor update removes covered flame and changes shape");
        level.removeBlock(pos.above(),false);
        h.assertTrue(level.getBlockState(pos).getValue(BlockJumboCandle.SHAPE) == BlockJumboCandle.CandleShape.SINGLE,"Removing stacked candle restores single shape");
        var lines = new java.util.ArrayList<Component>();
        // Invoke through Item's native overload to catch silently orphaned legacy callbacks.
        LEGACY_ITEM.get().appendHoverText(new ItemStack(LEGACY_ITEM.get()),Item.TooltipContext.EMPTY,TooltipDisplay.DEFAULT,lines::add,TooltipFlag.NORMAL);
        TOOLTIP_ITEM.get().appendHoverText(new ItemStack(TOOLTIP_ITEM.get()),Item.TooltipContext.EMPTY,TooltipDisplay.DEFAULT,lines::add,TooltipFlag.NORMAL);
        h.assertTrue(lines.stream().anyMatch(c -> c.getString().equals("Legacy item callback"))
            && lines.stream().anyMatch(c -> c.getString().equals("Block tooltip callback")),"Native tooltip dispatch reaches item and block callbacks");
        h.succeed();
    }

    public static final class RemovalBlock extends Block implements EntityBlock {
        RemovalBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED,false)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(BlockStateProperties.POWERED); }
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new RemovalEntity(pos,state); }
    }
    public static final class RemovalEntity extends DropAwareBlockEntity implements Container {
        private final SimpleContainer inventory = new SimpleContainer(1);
        int extraDrops;
        boolean sawInventoryEmpty, sawRegistered;
        RemovalEntity(BlockPos pos,BlockState state) { super(REMOVAL_TYPE.get(),pos,state); }
        @Override protected void dropAdditionalContents(ServerLevel level) {
            extraDrops++; sawInventoryEmpty=isEmpty(); sawRegistered=level.getBlockEntity(getBlockPos())==this;
            Containers.dropItemStack(level,getBlockPos().getX(),getBlockPos().getY(),getBlockPos().getZ(),new ItemStack(Items.EMERALD));
        }
        @Override public int getContainerSize() { return inventory.getContainerSize(); }
        @Override public boolean isEmpty() { return inventory.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return inventory.getItem(slot); }
        @Override public ItemStack removeItem(int slot,int count) { return inventory.removeItem(slot,count); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return inventory.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot,ItemStack stack) { inventory.setItem(slot,stack); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { inventory.clearContent(); }
    }
    public static final class TooltipBlock extends Block implements IBlockTooltip {
        TooltipBlock(Properties properties) { super(properties); }
        @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag) { lines.add(Component.literal("Block tooltip callback")); }
    }
}
