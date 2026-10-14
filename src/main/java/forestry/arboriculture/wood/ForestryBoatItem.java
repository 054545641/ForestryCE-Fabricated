package forestry.arboriculture.wood;

import forestry.arboriculture.entities.ForestryBoat;
import forestry.arboriculture.entities.ForestryChestBoat;
import forestry.core.platform.item.ItemForestry;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

// BoatItem
public class ForestryBoatItem extends ItemForestry {
	private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);

	private final ForestryWoodType type;
	private final boolean hasChest;

	public ForestryBoatItem(ForestryWoodType type, boolean hasChest, Properties properties) {
		super(properties.stacksTo(1));

		this.type = type;
		this.hasChest = hasChest;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

		if (hit.getType() == HitResult.Type.MISS) {
			return InteractionResult.PASS;
		} else {
			Vec3 view = player.getViewVector(1f);
			List<Entity> list = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5)).inflate(1), ENTITY_PREDICATE);

			if (!list.isEmpty()) {
				Vec3 vec31 = player.getEyePosition();

				for (Entity entity : list) {
					AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());

					if (aabb.contains(vec31)) {
						return InteractionResult.PASS;
					}
				}
			}

			if (hit.getType() == HitResult.Type.BLOCK) {
				ForestryBoat boat = getBoat(level, hit);
				boat.setWoodType(this.type);
				boat.setYRot(player.getYRot());
				if (!level.noCollision(boat, boat.getBoundingBox())) {
					return InteractionResult.FAIL;
				} else {
					if (!level.isClientSide()) {
						if (!level.addFreshEntity(boat)) return InteractionResult.FAIL;
						level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
						if (!player.getAbilities().instabuild) {
							stack.shrink(1);
						}
					}

					player.awardStat(Stats.ITEM_USED.get(this));
					return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
				}
			} else {
				return InteractionResult.PASS;
			}
		}
	}

	private ForestryBoat getBoat(Level level, HitResult hit) {
		Vec3 location = hit.getLocation();
		return this.hasChest ? new ForestryChestBoat(level, location.x, location.y, location.z) : new ForestryBoat(level, location.x, location.y, location.z);
	}
}
