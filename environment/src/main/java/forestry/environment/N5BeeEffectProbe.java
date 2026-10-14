package forestry.environment;

import forestry.apiculture.bees.genetics.effects.BeeEffectEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import java.util.function.Consumer;

/** Runs the production effects' mutation helpers in a native server world. */
public final class N5BeeEffectProbe {
    public static void entities(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var source = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.BONE, 2));
        helper.assertTrue(level.addFreshEntity(source), "Source drop joins level");
        Consumer<FinalizeSpawnEvent> cancelFinalize = event -> {
            if (event.getEntity().level() == level && event.getEntity().getType() == EntityTypes.SKELETON) {
                event.setSpawnCancelled(true);
            }
        };
        Consumer<EntityJoinLevelEvent> cancelJoin = event -> {
            if (event.getLevel() == level && (event.getEntity().getType() == EntityTypes.SKELETON
                || event.getEntity().getType() == EntityTypes.MOOSHROOM)) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(cancelFinalize);
        try {
            helper.assertFalse(BeeEffectEntities.resurrect(source, Items.BONE, EntityTypes.SKELETON), "Cancelled finalization fails resurrection");
            helper.assertTrue(source.isAlive() && source.getItem().getCount() == 2, "Cancelled finalization preserves source");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancelFinalize);
        }
        var cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
        cow.snapTo(pos.getX(), pos.getY(), pos.getZ());
        cow.setAge(-600);
        cow.setHealth(7);
        cow.setCustomName(Component.literal("Effect persistence"));
        cow.setPersistenceRequired();
        level.addFreshEntity(cow);
        NeoForge.EVENT_BUS.addListener(cancelJoin);
        try {
            helper.assertFalse(BeeEffectEntities.resurrect(source, Items.BONE, EntityTypes.SKELETON), "Cancelled join fails resurrection");
            helper.assertTrue(source.isAlive() && source.getItem().getCount() == 2, "Cancelled join preserves drop");
            helper.assertFalse(BeeEffectEntities.convertCow(cow), "Cancelled conversion join fails");
            helper.assertTrue(cow.isAlive() && level.getEntity(cow.getUUID()) == cow, "Failed conversion retains original cow in level");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancelJoin);
        }
        helper.assertFalse(BeeEffectEntities.resurrect(source, Items.BONE, EntityTypes.ITEM), "Non-mob datapack entry is safely rejected");
        source.getItem().set(DataComponents.CUSTOM_NAME, Component.literal("Keep named bone"));
        helper.assertFalse(BeeEffectEntities.resurrect(source, Items.BONE, EntityTypes.SKELETON), "Component mismatch is preserved");
        source.getItem().remove(DataComponents.CUSTOM_NAME);
        helper.assertTrue(BeeEffectEntities.resurrect(source, Items.BONE, EntityTypes.SKELETON), "Successful resurrection joins a mob");
        helper.assertTrue(source.isAlive() && source.getItem().getCount() == 1, "Success consumes exactly one item");
        var problems = new ProblemReporter.Collector();
        var savedDrop = TagValueOutput.createWithContext(problems, level.registryAccess());
        source.saveWithoutId(savedDrop);
        var restoredDrop = new ItemEntity(EntityTypes.ITEM, level);
        restoredDrop.load(TagValueInput.create(problems, level.registryAccess(), savedDrop.buildResult()));
        helper.assertTrue(restoredDrop.getItem().is(Items.BONE) && restoredDrop.getItem().getCount() == 1, "Remaining count survives ValueIO round trip");
        helper.assertTrue(BeeEffectEntities.resurrect(source, Items.BONE, EntityTypes.SKELETON) && source.isRemoved(), "Final item removes empty drop");
        helper.assertFalse(BeeEffectEntities.resurrect(source, Items.BONE, EntityTypes.SKELETON), "Removed drop cannot spawn again");
        helper.assertTrue(BeeEffectEntities.convertCow(cow) && cow.isRemoved(), "Successful conversion replaces original");
        var area = new AABB(pos).inflate(3);
        var converted = level.getEntitiesOfClass(MushroomCow.class, area);
        helper.assertTrue(converted.size() == 1, "Exactly one replacement joins level");
        var savedCow = TagValueOutput.createWithContext(problems, level.registryAccess());
        converted.getFirst().saveWithoutId(savedCow);
        var restoredCow = EntityTypes.MOOSHROOM.create(level, EntitySpawnReason.LOAD);
        restoredCow.load(TagValueInput.create(problems, level.registryAccess(), savedCow.buildResult()));
        helper.assertTrue(problems.isEmpty() && restoredCow.getAge() == -600 && restoredCow.getHealth() == 7
            && restoredCow.isPersistenceRequired() && Component.literal("Effect persistence").equals(restoredCow.getCustomName()),
            "Conversion retains age, health, name and persistence through ValueIO");
        level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area).forEach(Entity::discard);
        helper.succeed();
    }
}
