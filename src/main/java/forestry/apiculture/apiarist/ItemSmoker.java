package forestry.apiculture.apiarist;

import forestry.api.apiculture.hives.IHiveTile;
import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.client.ParticleRender;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ItemSmoker extends ItemForestry {
	public ItemSmoker(Properties properties) {
		super(properties.stacksTo(1));
	}

	/** Called only by the client player-tick handler; idle smoke never changes hive state. */
	public void clientIdleTick(Player player) {
		if (player.level().isClientSide() && !player.isUsingItem() && player.level().getRandom().nextInt(40) == 0) {
			addSmoke(player.level(), player, 1);
		}
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
		addSmoke(level, entity, (count % 5) + 1);
	}

	private static HumanoidArm getHandSide(Entity entity) {
		if (entity instanceof LivingEntity LivingEntity) {
			InteractionHand activeHand = LivingEntity.isUsingItem() ? LivingEntity.getUsedItemHand() : InteractionHand.MAIN_HAND;
			HumanoidArm handSide = LivingEntity.getMainArm();
			if (activeHand == InteractionHand.OFF_HAND) {
				handSide = handSide.getOpposite();
			}
			return handSide;
		}
		return HumanoidArm.RIGHT;
	}

	private static void addSmoke(Level level, Entity entity, int distance) {
		if (distance <= 0) {
			return;
		}
		Vec3 look = entity.getLookAngle();
		HumanoidArm handSide = getHandSide(entity);

		Vec3 handOffset;
		if (handSide == HumanoidArm.RIGHT) {
			handOffset = look.cross(new Vec3(0, 1, 0));
		} else {
			handOffset = look.cross(new Vec3(0, -1, 0));
		}

		Vec3 lookDistance = new Vec3(look.x * distance, look.y * distance, look.z * distance);
		Vec3 scaledOffset = handOffset.scale(1.0 / distance);
		Vec3 smokePos = lookDistance.add(entity.position()).add(scaledOffset);

		if (level.isClientSide()) {
			ParticleRender.addEntitySmokeFX(level, smokePos.x, smokePos.y + 1, smokePos.z);
		}

		BlockPos blockPos = BlockPos.containing(smokePos.x, smokePos.y + 1, smokePos.z);
		calmAt(level, blockPos);
	}

	private static void calmAt(Level level, BlockPos pos) {
		if (!level.isClientSide() && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof IHiveTile hive) {
			hive.calmBees();
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		ItemStack itemStack = player.getItemInHand(hand);
		return InteractionResult.SUCCESS;
	}

	public InteractionResult useBeforeBlock(ItemStack stack, UseOnContext context) {
		calmAt(context.getLevel(), context.getClickedPos());
		return InteractionResult.PASS;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 32;
	}
}
