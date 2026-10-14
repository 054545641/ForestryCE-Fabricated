package forestry.apiculture;

import forestry.api.ForestryConstants;
import forestry.apiculture.features.ApicultureEffects;
import forestry.apiculture.apiarist.villagers.ApicultureVillagers;
import forestry.apiculture.worldgen.VillagerJigsaw;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import forestry.apiculture.apiarist.ApiaristAI;

/**
 * Apiculture's game-bus handlers. Split out of {@code forestry.core.platform.EventHandlerCore} so the base
 * artifact does not wire bee content.
 */
@EventBusSubscriber(modid = ForestryConstants.MOD_ID)
public class EventHandlerApiculture {
	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Villager villager) {
			if (villager.getVillagerData().profession().is(ApicultureVillagers.PROF_BEEKEEPER.getKey())) {
				villager.goalSelector.addGoal(6, new ApiaristAI(villager, 0.6));
			}
		}
	}

	@SubscribeEvent
	public static void doHakunaDamageReduction(LivingIncomingDamageEvent event) {
		if (event.getEntity().hasEffect(ApicultureEffects.HAKUNA_MATATA)) {
			event.setCanceled(true);
			if (event.getAmount() > 5) {
				event.getEntity().removeEffect(ApicultureEffects.HAKUNA_MATATA);
				event.getEntity().addEffect(new MobEffectInstance(ApicultureEffects.MATATA, (int) (300 * event.getAmount())));
				event.getEntity().playSound(SoundEvents.WITHER_BREAK_BLOCK);
				if (event.getSource().getEntity() instanceof LivingEntity attacker) {
					//no to no worries when attacking
					if (attacker.hasEffect(ApicultureEffects.HAKUNA_MATATA)) {
						attacker.removeEffect(ApicultureEffects.HAKUNA_MATATA);
						attacker.addEffect(new MobEffectInstance(ApicultureEffects.MATATA, (int) (300 * event.getAmount())));
						attacker.playSound(SoundEvents.WITHER_BREAK_BLOCK);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void serverAboutToStart(ServerAboutToStartEvent event) {
		Registry<StructureTemplatePool> pools = event.getServer().registryAccess().lookup(Registries.TEMPLATE_POOL).orElseThrow();
		Registry<StructureProcessorList> processors = event.getServer().registryAccess().lookup(Registries.PROCESSOR_LIST).orElseThrow();

		VillagerJigsaw.init(pools, processors);
	}
}
