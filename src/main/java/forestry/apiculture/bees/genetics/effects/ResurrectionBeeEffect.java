package forestry.apiculture.bees.genetics.effects;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;

/**
 * The {@code forestry:resurrect} primitive: finds mob drops on the floor and resurrects them into their creature
 * counterparts, consuming one item per resurrection. Even works on the Dragon Egg!
 * <p>
 * The item&rarr;mob table is a datapack parameter, so the two built-ins that share this behaviour differ only by their
 * list: {@code REANIMATION} (bones/flesh &rarr; skeletons/zombies/blazes) and {@code RESURRECTION} (gunpowder, ender
 * pearls, the dragon egg &rarr; creepers, endermen, the Ender Dragon). Both are datapack-defined by
 * {@code BeeEffectProvider} from {@link #getReanimationList()} / {@link #getResurrectionList()}.
 */
public class ResurrectionBeeEffect extends ThrottledBeeEffect {
	public static final MapCodec<ResurrectionBeeEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ThrottleSettings.codec(40, true, true).forGetter(ThrottledBeeEffect::settings),
		Resurrectable.CODEC.listOf().fieldOf("entries").forGetter(effect -> effect.resurrectables)
	).apply(instance, ResurrectionBeeEffect::new));

	private final List<Resurrectable> resurrectables;

	public ResurrectionBeeEffect(boolean dominant, int throttle, List<Resurrectable> resurrectables) {
		this(new ThrottleSettings(dominant, throttle, true, true), resurrectables);
	}

	public ResurrectionBeeEffect(ThrottleSettings settings, List<Resurrectable> resurrectables) {
		super(settings);
		// Copied into a mutable list: doEffectThrottled shuffles it in place, and a codec-decoded list is immutable.
		this.resurrectables = new ArrayList<>(resurrectables);
	}

	@Override
	public MapCodec<? extends IBeeEffect> codec() {
		return MAP_CODEC;
	}

	@Override
	public IEffectData doEffectThrottled(IGenome genome, IEffectData storedData, IBeeHousing housing) {
		List<ItemEntity> entities = ThrottledBeeEffect.getEntitiesInRange(genome, housing, ItemEntity.class);
		if (entities.isEmpty()) {
			return storedData;
		}

		net.minecraft.util.Util.shuffle(this.resurrectables, housing.getLevel().getRandom());

		for (ItemEntity entity : entities) {
			if (resurrectEntity(entity)) {
				break;
			}
		}

		return storedData;
	}

	private boolean resurrectEntity(ItemEntity entity) {
		for (Resurrectable entry : this.resurrectables) {
			if (BeeEffectEntities.resurrect(entity, entry.item(), entry.entity())) {
				return true;
			}
		}
		return false;
	}

	public static List<Resurrectable> getReanimationList() {
		List<Resurrectable> list = new ArrayList<>();
		list.add(new Resurrectable(Items.BONE, EntityTypes.SKELETON));
		list.add(new Resurrectable(Items.ARROW, EntityTypes.SKELETON));
		list.add(new Resurrectable(Items.ROTTEN_FLESH, EntityTypes.ZOMBIE));
		list.add(new Resurrectable(Items.BLAZE_ROD, EntityTypes.BLAZE));
		return list;
	}

	public static List<Resurrectable> getResurrectionList() {
		List<Resurrectable> list = new ArrayList<>();
		list.add(new Resurrectable(Items.GUNPOWDER, EntityTypes.CREEPER));
		list.add(new Resurrectable(Items.ENDER_PEARL, EntityTypes.ENDERMAN));
		list.add(new Resurrectable(Items.STRING, EntityTypes.SPIDER));
		list.add(new Resurrectable(Items.SPIDER_EYE, EntityTypes.SPIDER));
		list.add(new Resurrectable(Items.STRING, EntityTypes.CAVE_SPIDER));
		list.add(new Resurrectable(Items.SPIDER_EYE, EntityTypes.CAVE_SPIDER));
		list.add(new Resurrectable(Items.GHAST_TEAR, EntityTypes.GHAST));
		list.add(new Resurrectable(Blocks.DRAGON_EGG.asItem(), EntityTypes.ENDER_DRAGON));
		return list;
	}

	/**
	 * One item&rarr;mob mapping. The dropped item must match {@link #item} exactly (item and components), matching the
	 * historical {@code new ItemStack(item)} comparison.
	 */
	public record Resurrectable(Item item, EntityType<?> entity) {
		public static final Codec<Resurrectable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Resurrectable::item),
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(Resurrectable::entity)
		).apply(inst, Resurrectable::new));

	}
}
