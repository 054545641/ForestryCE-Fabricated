package forestry.environment;

import com.mojang.datafixers.util.Pair;
import forestry.apiculture.apiarist.ItemSmoker;
import forestry.apiculture.worldgen.TemplatePoolInsertion;
import forestry.core.content.backpacks.BackpackTransfers;
import forestry.core.content.backpacks.BackpackMode;
import net.minecraft.core.component.DataComponentType;
import forestry.core.platform.inventory.InventoryPlain;
import forestry.core.platform.inventory.InventoryTransferView;
import forestry.core.platform.inventory.StandardStackFilters;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.List;
import java.util.function.Supplier;

public final class N5CompletionProbe {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,EnvironmentProbe.ID);
    public static final Supplier<DataComponentType<BackpackMode>> MODE = COMPONENTS.register("backpack_mode",BackpackMode::componentType);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM,EnvironmentProbe.ID);
    public static final Supplier<ItemSmoker> SMOKER = ITEMS.register("probe_smoker", id -> new ItemSmoker(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));

    public static void verify(GameTestHelper h) {
        var level = h.getLevel();
        var fallback = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL)
            .getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,Identifier.withDefaultNamespace("empty")));
        var pool = new StructureTemplatePool(fallback,List.of(Pair.of(EmptyPoolElement.INSTANCE,2)));
        var manager = level.getServer().getStructureManager();
        h.assertTrue(pool.getMaxSize(manager) == 0,"Cache empty pool height before insertion");
        var piece = StructurePoolElement.single("minecraft:village/plains/houses/plains_small_house_1").apply(StructureTemplatePool.Projection.RIGID);
        TemplatePoolInsertion.append(pool,piece,15);
        h.assertTrue(pool.size() == 17 && pool.getTemplates().size() == 2,"Serialized and weighted pool views both grow");
        h.assertTrue(pool.getShuffledTemplates(level.getRandom()).stream().filter(p -> p == piece).count() == 15,"Insertion preserves weight exactly");
        h.assertTrue(pool.getMaxSize(manager) > 0,"Height cache is recomputed after insertion");
        var ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var restored = StructureTemplatePool.DIRECT_CODEC.parse(ops,StructureTemplatePool.DIRECT_CODEC.encodeStart(ops,pool).getOrThrow()).getOrThrow();
        h.assertTrue(restored.size() == 17 && restored.getFallback() == fallback,"Pool codec round trip retains weight and fallback");
        boolean rejected = false;
        try { TemplatePoolInsertion.append(pool,piece,0); } catch (IllegalArgumentException expected) { rejected=true; }
        h.assertTrue(rejected && pool.size() == 17,"Invalid weight is rejected without partial mutation");

        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var source = new InventoryPlain(1,"Items",64);
        var apples = new ItemStack(Items.APPLE,9);
        apples.set(DataComponents.CUSTOM_NAME,Component.literal("Matched components"));
        source.setItem(0,apples.copy());
        player.getInventory().setItem(0,apples.copyWithCount(63));
        player.setItemInHand(InteractionHand.OFF_HAND,apples.copyWithCount(62));
        player.getInventory().setItem(1,new ItemStack(Items.APPLE,10));
        int[] commits = {0};
        source.addCommitListener(slots -> commits[0]++);
        try(var transaction=Transaction.openRoot()) {
            h.assertTrue(BackpackTransfers.topOff(player,source.getTransferHandler(),0,9,transaction)==3,"Only existing matching main/offhand stacks are topped up");
        }
        h.assertTrue(source.getItem(0).getCount()==9 && player.getInventory().getItem(0).getCount()==63
            && player.getOffhandItem().getCount()==62 && commits[0]==0,"Root rollback restores both inventories without persistence callbacks");
        var rejecting = new InventoryTransferView(source.getTransferHandler(),1,i->i,(i,r)->true,(i,r)->false);
        try(var transaction=Transaction.openRoot()) {
            h.assertTrue(BackpackTransfers.topOff(player,rejecting,0,9,transaction)==0,"Rejected extraction cancels tentative player insertion");
            transaction.commit();
        }
        h.assertTrue(source.getItem(0).getCount()==9 && player.getInventory().getItem(0).getCount()==63 && player.getOffhandItem().getCount()==62,
            "Late source failure leaves neither duplication nor loss");
        try(var transaction=Transaction.openRoot()) {
            h.assertTrue(BackpackTransfers.topOff(player,source.getTransferHandler(),0,1,transaction)==1,"Resupply moves one item per pass");
            transaction.commit();
        }
        h.assertTrue(commits[0]==1 && source.getItem(0).getCount()==8 && player.getInventory().getItem(0).getCount()==64,
            "Commit persists once and respects max stack size");
        h.assertTrue(player.getInventory().getItem(1).getCount()==10 && player.getInventory().getItem(2).isEmpty(),"Different components and empty slots remain untouched");
        var tag = new net.minecraft.nbt.CompoundTag();
        source.write(tag,level.registryAccess());
        var reloaded = new InventoryPlain(1,"Items",64);
        reloaded.read(tag,level.registryAccess());
        h.assertTrue(ItemStack.matches(source.getItem(0),reloaded.getItem(0)),"Committed source persists count and components");
        h.assertTrue(StandardStackFilters.FUEL.test(new ItemStack(Items.COAL)) && !StandardStackFilters.FUEL.test(new ItemStack(Items.DIAMOND)),"Fuel filter reads the loaded fuel data map");

        // A non-damageable item exercises the production mode component policy.
        var modeStack = new ItemStack(Items.APPLE);
        h.assertTrue(BackpackMode.read(modeStack,MODE.get())==BackpackMode.NEUTRAL,"Missing mode defaults to neutral");
        modeStack.set(DataComponents.DAMAGE,3);
        h.assertTrue(BackpackMode.read(modeStack,MODE.get())==BackpackMode.RESUPPLY,"Legacy damage is read without durability clamping");
        for (var mode : BackpackMode.VALUES) {
            mode.write(modeStack,MODE.get());
            h.assertTrue(!modeStack.has(DataComponents.DAMAGE) && BackpackMode.read(modeStack,MODE.get())==mode,"Mode no longer uses damage");
            var saved = ItemStack.CODEC.encodeStart(ops,modeStack).getOrThrow();
            var loaded = ItemStack.CODEC.parse(ops,saved).getOrThrow();
            h.assertTrue(BackpackMode.read(loaded,MODE.get())==mode,"Saved item retains each backpack mode");
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer,modeStack);
                h.assertTrue(BackpackMode.read(ItemStack.STREAM_CODEC.decode(buffer),MODE.get())==mode,"Network item retains each backpack mode");
            } finally { buffer.release(); }
        }
        h.assertTrue(BackpackMode.RECEIVE.next(false)==BackpackMode.NEUTRAL && BackpackMode.RECEIVE.next(true)==BackpackMode.RESUPPLY
            && BackpackMode.RESUPPLY.next(true)==BackpackMode.NEUTRAL,"Mode cycle respects the resupply configuration");

        var smoker = SMOKER.get();
        var smokerStack = new ItemStack(smoker);
        player.setItemInHand(InteractionHand.OFF_HAND,smokerStack);
        h.assertTrue(smoker.use(level,player,InteractionHand.OFF_HAND).consumesAction() && player.isUsingItem()
            && player.getUsedItemHand()==InteractionHand.OFF_HAND,"Smoker retains offhand use and native duration");
        h.assertTrue(smoker.getUseDuration(smokerStack,player)==32,"Smoker use lasts 32 ticks");
        smoker.clientIdleTick(player); // Must not touch client particle classes on a dedicated server.
        smoker.onUseTick(level,player,smokerStack,30);
        player.stopUsingItem();
        h.succeed();
    }
}
