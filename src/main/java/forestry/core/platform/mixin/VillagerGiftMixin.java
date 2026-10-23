package forestry.core.platform.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GiveGiftToHero.class)
abstract class VillagerGiftMixin {
    @Inject(method = "getLootTableToThrow", at = @At("HEAD"), cancellable = true)
    private static void forestry$professionGift(Villager villager, CallbackInfoReturnable<ResourceKey<LootTable>> callback) {
        if (villager.isBaby()) return;
        villager.getVillagerData().profession().unwrapKey().ifPresent(profession -> {
            Identifier id = profession.identifier();
            if (id.getNamespace().equals("forestry") && (id.getPath().equals("arborist") || id.getPath().equals("beekeeper"))) {
                callback.setReturnValue(ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("forestry", "gameplay/hero_of_the_village/" + id.getPath() + "_gift")));
            }
        });
    }
}
