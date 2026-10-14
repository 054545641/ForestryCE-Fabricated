package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.block.NaturalistChestBlockType;
import forestry.core.platform.block.*;
import forestry.core.content.burnbarrel.BlockBurnBarrel;
import forestry.core.content.decorative.BlockBigCandle;
import forestry.core.content.decorative.BlockJumboCandle;
import forestry.core.content.decorative.BlockPlywoodBlock;
import forestry.core.content.decorative.BlockSheet;
import forestry.core.content.decorative.BlockTypeBigCandle;
import forestry.core.content.decorative.BlockTypeJumboCandle;
import forestry.core.content.decorative.BlockTypeMetalPlating;
import forestry.core.content.decorative.CandleRefractory;
import forestry.core.content.decorative.CorkBlock;
import forestry.core.content.soil.*;
import forestry.core.content.lighting.BlockWaterloggableTorch;
import forestry.core.content.lighting.BlockWaterloggableWallTorch;
import forestry.core.content.resources.*;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ItemBlockTesr;
import forestry.core.platform.registration.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

@FeatureProvider
public class CoreBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final FeatureBlockGroup<BlockCore, BlockTypeCoreTesr> BASE = REGISTRY.blockGroup((properties, type) -> new BlockCore(type, properties), List.of(BlockTypeCoreTesr.values()))
		.blockProperties((properties, type) -> type.getBlockProperties())
		.item(ItemBlockTesr::new).create();

	// Deviation from 1.20.1: the barrel built its own properties inside its constructor. Here the registry owns them.
	// Blocks.FURNACE is safe to ofFullCopy, it sets no dropsLike, so the copy cannot steal a vanilla loot table id
	public static final FeatureBlock<BlockBurnBarrel, ItemBlockForestry<?>> BURN_BARREL = REGISTRY.block(BlockBurnBarrel::new, () -> Properties.ofFullCopy(Blocks.FURNACE)
		.mapColor(MapColor.COLOR_GRAY)
		.sound(SoundType.METAL)
		.lightLevel(state -> state.getValue(BlockBurnBarrel.LIT) ? 15 : 0), ItemBlockForestry::new, "burn_barrel");

	public static final FeatureBlock<BlockBogEarth, ItemBlockForestry<?>> BOG_EARTH = REGISTRY.block(BlockBogEarth::new, ItemBlockForestry::new, "bog_earth");
	public static final FeatureBlock<BlockPeat, ItemBlockForestry<?>> PEAT = REGISTRY.block(BlockPeat::new, null, "peat");
	public static final FeatureBlock<BlockHumus, ItemBlockForestry<?>> HUMUS = REGISTRY.block(BlockHumus::new, ItemBlockForestry::new, "humus");
	public static final FeatureBlockGroup<BlockResourceStorage, EnumResourceType> RESOURCE_STORAGE = REGISTRY.blockGroup(BlockResourceStorage::new, List.of(EnumResourceType.values())).item(ItemBlockForestry::new).identifier("block", FeatureGroup.IdentifierType.SUFFIX).create();
	public static final FeatureBlock<Block, BlockItem> APATITE_ORE = REGISTRY.block((properties) -> new DropExperienceBlock(UniformInt.of(0, 4), properties), () -> Properties.ofFullCopy(Blocks.COAL_ORE), ItemBlockForestry::new, "apatite_ore");
	public static final FeatureBlock<Block, BlockItem> DEEPSLATE_APATITE_ORE = REGISTRY.block((properties) -> new DropExperienceBlock(UniformInt.of(0, 4), properties), () -> Properties.ofFullCopy(APATITE_ORE.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE), ItemBlockForestry::new, "deepslate_apatite_ore");
	public static final FeatureBlock<Block, BlockItem> TIN_ORE = REGISTRY.block(Block::new, () -> Properties.ofFullCopy(Blocks.COPPER_ORE), ItemBlockForestry::new, "tin_ore");
	public static final FeatureBlock<Block, BlockItem> DEEPSLATE_TIN_ORE = REGISTRY.block(Block::new, () -> Properties.ofFullCopy(TIN_ORE.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE), ItemBlockForestry::new, "deepslate_tin_ore");
	public static final FeatureBlock<Block, BlockItem> RAW_TIN_BLOCK = REGISTRY.block(Block::new, () -> Properties.ofFullCopy(Blocks.RAW_COPPER_BLOCK), ItemBlockForestry::new, "raw_tin_block");

	/* Turf */
	// Blocks.GRASS_BLOCK is safe to ofFullCopy, it sets no dropsLike, so the copy cannot steal a vanilla loot
	// table id. It does carry randomTicks(), which 1.20.1 copied too. Spreading lives in GrassBlock rather than
	// in the properties, and neither block below is one, so neither spreads and the tick reaches nothing
	public static final FeatureBlock<Block, BlockItem> TURF_BLOCK = REGISTRY.block(Block::new, () -> Properties.ofFullCopy(Blocks.GRASS_BLOCK), ItemBlockForestry::new, "turf_block");
	public static final FeatureBlock<CarpetBlock, BlockItem> TURF = REGISTRY.block(CarpetBlock::new, () -> Properties.ofFullCopy(Blocks.GRASS_BLOCK), ItemBlockForestry::new, "turf");

	/* Plywood and cork */
	// Deviation from 1.20.1: the sheet and the cork built their properties inside their constructors, and all
	// three block items carried new ItemProperties().burnTime(n). The registry owns the properties here, and
	// the burn times moved to the neoforge:furnace_fuels data map in ForestryDataMapProvider
	// todo sheet blocks can only be wood
	public static final FeatureBlock<BlockSheet, BlockItem> PLYWOOD_SHEET = REGISTRY.block(BlockSheet::new, () -> Properties.ofFullCopy(Blocks.OAK_PLANKS), ItemBlockForestry::new, "plywood");
	public static final FeatureBlock<BlockPlywoodBlock, BlockItem> PLYWOOD_BLOCK = REGISTRY.block(BlockPlywoodBlock::new, () -> Properties.ofFullCopy(Blocks.OAK_PLANKS).ignitedByLava(), ItemBlockForestry::new, "plywood_block");
	public static final FeatureBlock<CorkBlock, BlockItem> CORK = REGISTRY.block(CorkBlock::new, () -> Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.CHERRY_WOOD).strength(2.0f, 3.0f).ignitedByLava(), ItemBlockForestry::new, "cork");

	/* Lighting */
	// Deviation from 1.20.1: block properties now come from the registry's Supplier<Properties> overload
	// (Properties.copy was renamed ofFullCopy), and block items take a BiFunction<B, Item.Properties, I>.
	public static final FeatureBlock<ChainBlock, BlockItem> TIN_CHAIN = REGISTRY.block(ChainBlock::new, () -> Properties.ofFullCopy(Blocks.IRON_CHAIN), ItemBlockForestry::new, "tin_chain");
	public static final FeatureBlock<LanternBlock, BlockItem> PHOSPHOR_LANTERN = REGISTRY.block(LanternBlock::new, () -> Properties.ofFullCopy(Blocks.SOUL_LANTERN).lightLevel(state -> 13), ItemBlockForestry::new, "phosphor_lantern");
	// The torch's item is registered separately in CoreItems as a StandingAndWallBlockItem covering both blocks
	public static final FeatureBlock<BlockWaterloggableTorch, ItemBlockForestry<?>> PHOSPHOR_TORCH = REGISTRY.block(BlockWaterloggableTorch::new, () -> Properties.ofFullCopy(Blocks.SOUL_TORCH).lightLevel(state -> 13), null, "phosphor_torch");
	// Deviation from 1.20.1: copies SOUL_TORCH rather than SOUL_WALL_TORCH. 1.20.1's Properties.copy left the loot
	// table alone, but 1.21.1's ofFullCopy also copies `drops`, and vanilla's soul wall torch sets dropsLike(SOUL_TORCH).
	// Copying it would give this block the minecraft:blocks/soul_torch loot table, so it would drop a vanilla soul
	// torch in game and make the loot datagen write forestry's table into the minecraft namespace. Vanilla's soul wall
	// torch properties are otherwise identical to soul torch's, so copying the standing torch reproduces 1.20.1 exactly.
	public static final FeatureBlock<BlockWaterloggableWallTorch, ItemBlockForestry<?>> PHOSPHOR_WALL_TORCH = REGISTRY.block(BlockWaterloggableWallTorch::new, () -> Properties.ofFullCopy(Blocks.SOUL_TORCH).lightLevel(state -> 13), null, "phosphor_wall_torch");

	public static final FeatureBlockGroup<BlockTesr<NaturalistChestBlockType>, NaturalistChestBlockType> NATURALIST_CHEST = REGISTRY.blockGroup((properties, type) -> new BlockTesr<>(type, properties.sound(SoundType.WOOD)), List.of(NaturalistChestBlockType.values())).item(ItemBlockTesr::new).create();

	/* Decorative stone and brick block sets */
	// Deviation from 1.20.1: that tree registered all 68 of these by hand and left a "//TODO: Helper method?"
	// where the section starts. stoneFamily, chiseled and stoneSet below stand in for it

	// Ash bricks
	public static final StoneFamily ASH_BRICKS = stoneFamily("ash_bricks", MapColor.COLOR_LIGHT_GRAY, SoundType.MUD_BRICKS);
	public static final FeatureBlock<Block, BlockItem> CHISELED_ASH_BRICKS = chiseled("ash_bricks", MapColor.COLOR_LIGHT_GRAY, SoundType.MUD_BRICKS);

	// Wax bricks
	public static final StoneFamily WAX_BRICKS = stoneFamily("wax_bricks", MapColor.COLOR_YELLOW, SoundType.MUD);
	public static final FeatureBlock<Block, BlockItem> CHISELED_WAX_BRICKS = chiseled("wax_bricks", MapColor.COLOR_YELLOW, SoundType.MUD);

	// Refractory wax bricks
	public static final StoneFamily REFRACTORY_WAX_BRICKS = stoneFamily("refractory_wax_bricks", MapColor.COLOR_RED, SoundType.MUD);
	// Deviation from 1.20.1: this one block carried MapColor.COLOR_YELLOW there while its four siblings
	// carried COLOR_RED. Read as a copy-paste slip and given COLOR_RED
	public static final FeatureBlock<Block, BlockItem> CHISELED_REFRACTORY_WAX_BRICKS = chiseled("refractory_wax_bricks", MapColor.COLOR_RED, SoundType.MUD);

	// Waxstone, refractory waxstone and honeystone, seventeen blocks each
	public static final StoneSet WAXSTONE = stoneSet("waxstone", MapColor.SAND, SoundType.CANDLE);
	public static final StoneSet REFRACTORY_WAXSTONE = stoneSet("refractory_waxstone", MapColor.SAND, SoundType.CANDLE);
	public static final StoneSet HONEYSTONE = stoneSet("honeystone", MapColor.COLOR_ORANGE, SoundType.CANDLE);

	// Misc
	public static final FeatureBlock<Block, BlockItem> ASHEN_WAX_BLOCK = REGISTRY.block(Block::new, mudBrickProperties(Blocks.MUD_BRICKS, MapColor.SAND, SoundType.MUD_BRICKS), ItemBlockForestry::new, "ashen_wax_block");
	public static final FeatureBlock<Block, BlockItem> CRISPY_HONEY_BLOCK = REGISTRY.block(Block::new, mudBrickProperties(Blocks.MUD_BRICKS, MapColor.COLOR_BROWN, SoundType.MUD_BRICKS), ItemBlockForestry::new, "crispy_honey_block");

	/* Metal plating */
	// Deviation from 1.20.1: BlockMetalPlating was a Block subclass whose only job was to build the
	// properties in its constructor. The registry owns them here, so the subclass is gone and the group
	// registers plain Blocks
	public static final FeatureBlockGroup<Block, BlockTypeMetalPlating> METAL_PLATING = REGISTRY.<Block, BlockTypeMetalPlating>blockGroup((properties, type) -> new Block(properties), List.of(BlockTypeMetalPlating.values()))
		.blockProperties((properties, type) -> Properties.ofFullCopy(Blocks.COPPER_BLOCK.waxed().unaffected()).mapColor(type.getMapColor()))
		.item(ItemBlockForestry::new)
		.identifier("metal_plating")
		.create();

	/* Candles */
	// Vanilla candles are built straight from Properties.of and never call dropsLike, so ofFullCopy carries
	// a null `drops` and cannot steal a vanilla loot table id
	public static final FeatureBlockGroup<BlockJumboCandle, BlockTypeJumboCandle> JUMBO_CANDLES = REGISTRY.blockGroup(BlockJumboCandle::new, List.of(BlockTypeJumboCandle.values()))
		.blockProperties((properties, type) -> Properties.ofFullCopy(Blocks.CANDLE)
			.mapColor(type.getMapColor())
			.lightLevel(state -> candleLight(state.getValue(BlockJumboCandle.LIT), type == BlockTypeJumboCandle.REFRACTORY)))
		.item(ItemBlockForestry::new)
		.identifier("jumbo_candle")
		.create();

	// Deviation from 1.20.1: the big candles carried no map color there, so every one of them mapped as the
	// vanilla candle's sand. They keep that here, since BlockTypeBigCandle names no colors
	public static final FeatureBlockGroup<BlockBigCandle, BlockTypeBigCandle> BIG_CANDLES = REGISTRY.blockGroup(BlockBigCandle::new, List.of(BlockTypeBigCandle.values()))
		.blockProperties((properties, type) -> Properties.ofFullCopy(Blocks.CANDLE)
			.lightLevel(state -> candleLight(state.getValue(BlockBigCandle.LIT), type == BlockTypeBigCandle.REFRACTORY)))
		.item(ItemBlockForestry::new)
		.identifier("big_candle")
		.create();

	public static final FeatureBlock<CandleBlock, BlockItem> RAINBOW_CANDLE = REGISTRY.block(CandleBlock::new, () -> Properties.ofFullCopy(Blocks.DYED_CANDLE.magenta()), ItemBlockForestry::new, "rainbow_candle");
	public static final FeatureBlock<CandleRefractory, BlockItem> REFRACTORY_CANDLE = REGISTRY.block(CandleRefractory::new, () -> Properties.ofFullCopy(Blocks.DYED_CANDLE.red())
		.lightLevel(state -> state.getValue(CandleBlock.LIT) ? state.getValue(CandleBlock.CANDLES) * 2 : 0), ItemBlockForestry::new, "refractory_candle");
	// todo the cake variants are still missing

	/**
	 * Used to walk every decorative stone set at once. Datagen and the creative tab read this rather than
	 * naming the three sets one at a time.
	 */
	public static final List<StoneSet> STONE_SETS = List.of(WAXSTONE, REFRACTORY_WAXSTONE, HONEYSTONE);

	/**
	 * Used to walk every decorative shape family at once, the three brick families and the twelve a stone
	 * set contributes. Fifteen families, sixty blocks.
	 */
	public static final List<StoneFamily> DECORATIVE_FAMILIES = Stream.concat(
		Stream.of(ASH_BRICKS, WAX_BRICKS, REFRACTORY_WAX_BRICKS),
		STONE_SETS.stream().flatMap(set -> set.families().stream())
	).toList();

	/**
	 * Used to walk every chiseled decorative block at once.
	 */
	public static final List<FeatureBlock<Block, BlockItem>> DECORATIVE_CHISELED = List.of(
		CHISELED_ASH_BRICKS, CHISELED_WAX_BRICKS, CHISELED_REFRACTORY_WAX_BRICKS,
		WAXSTONE.chiseled(), REFRACTORY_WAXSTONE.chiseled(), HONEYSTONE.chiseled()
	);

	/**
	 * A base block and the three shapes cut from it. Every block in a family shares one texture, one map
	 * color and one sound.
	 *
	 * @param base   The full block the family is named after
	 * @param stairs The stairs cut from the base block
	 * @param slab   The slab cut from the base block
	 * @param wall   The wall cut from the base block
	 */
	public record StoneFamily(FeatureBlock<Block, BlockItem> base, FeatureBlock<StairBlock, BlockItem> stairs, FeatureBlock<SlabBlock, BlockItem> slab, FeatureBlock<WallBlock, BlockItem> wall) {
		/**
		 * @return Every feature the family registered, in creative tab order
		 */
		public List<FeatureBlock<? extends Block, BlockItem>> features() {
			return List.of(this.base, this.stairs, this.slab, this.wall);
		}

		/**
		 * @return Every block the family registered, in creative tab order
		 */
		public Block[] blocks() {
			return new Block[]{this.base.block(), this.stairs.block(), this.slab.block(), this.wall.block()};
		}
	}

	/**
	 * A stone block and its three other surface finishes, one shape family each, plus the chiseled block the
	 * four finishes share.
	 *
	 * @param stone    The plain stone family
	 * @param cobbled  The cobbled family
	 * @param bricks   The brick family
	 * @param polished The polished family
	 * @param chiseled The chiseled block, which has no shapes of its own
	 */
	public record StoneSet(StoneFamily stone, StoneFamily cobbled, StoneFamily bricks, StoneFamily polished, FeatureBlock<Block, BlockItem> chiseled) {
		/**
		 * @return Every family the set registered, in creative tab order
		 */
		public List<StoneFamily> families() {
			return List.of(this.stone, this.cobbled, this.bricks, this.polished);
		}
	}

	/**
	 * Registers a base block and the stairs, slab and wall cut from it.
	 *
	 * @param id    The registry id of the base block, which the three shape ids derive from
	 * @param color The map color every block in the family carries
	 * @param sound The sound type every block in the family carries
	 * @return The four registered blocks
	 */
	private static StoneFamily stoneFamily(String id, MapColor color, SoundType sound) {
		// shapes cut from an "<x>_bricks" block are named "<x>_brick_<shape>", singular
		String shape = id.endsWith("bricks") ? id.substring(0, id.length() - 1) : id;

		return new StoneFamily(
			REGISTRY.block(Block::new, mudBrickProperties(Blocks.MUD_BRICKS, color, sound), ItemBlockForestry::new, id),
			// Deviation from 1.20.1: StairBlock took a Supplier<BlockState> there and takes the BlockState
			// itself here, so the base state is read eagerly rather than on first use
			REGISTRY.block(properties -> new StairBlock(Blocks.MUD_BRICKS.defaultBlockState(), properties), mudBrickProperties(Blocks.MUD_BRICK_STAIRS, color, sound), ItemBlockForestry::new, shape + "_stairs"),
			REGISTRY.block(SlabBlock::new, mudBrickProperties(Blocks.MUD_BRICK_SLAB, color, sound), ItemBlockForestry::new, shape + "_slab"),
			REGISTRY.block(WallBlock::new, mudBrickProperties(Blocks.MUD_BRICK_WALL, color, sound), ItemBlockForestry::new, shape + "_wall")
		);
	}

	/**
	 * Registers the chiseled block of a brick family or a stone set.
	 *
	 * @param id    The registry id of the family's base block, which the chiseled id derives from
	 * @param color The map color the block carries
	 * @param sound The sound type the block carries
	 * @return The registered block
	 */
	private static FeatureBlock<Block, BlockItem> chiseled(String id, MapColor color, SoundType sound) {
		return REGISTRY.block(Block::new, mudBrickProperties(Blocks.MUD_BRICKS, color, sound), ItemBlockForestry::new, "chiseled_" + id);
	}

	/**
	 * Registers a stone block, its cobbled, brick and polished finishes with the stairs, slab and wall of
	 * each, and the chiseled block the four finishes share.
	 *
	 * @param id    The registry id of the plain stone block, which the other sixteen ids derive from
	 * @param color The map color every block in the set carries
	 * @param sound The sound type every block in the set carries
	 * @return The seventeen registered blocks
	 */
	private static StoneSet stoneSet(String id, MapColor color, SoundType sound) {
		return new StoneSet(
			stoneFamily(id, color, sound),
			stoneFamily("cobbled_" + id, color, sound),
			stoneFamily(id + "_bricks", color, sound),
			stoneFamily("polished_" + id, color, sound),
			chiseled(id, color, sound)
		);
	}

	/**
	 * Used to build the light level of one jumbo or big candle. A refractory candle burns cooler than the
	 * rest, matching the soul fire it burns with.
	 *
	 * @param lit        Whether the candle is lit
	 * @param refractory Whether the candle is the refractory one
	 * @return The light level the candle emits
	 */
	private static int candleLight(boolean lit, boolean refractory) {
		if (!lit) {
			return 0;
		}
		return refractory ? 10 : 15;
	}

	/**
	 * Used to build the properties of one decorative block. Every one of them copies a vanilla mud brick
	 * block and overrides only the map color and the sound.
	 *
	 * @param copyFrom The vanilla mud brick block of the matching shape
	 * @param color    The map color the block carries
	 * @param sound    The sound type the block carries
	 * @return The properties supplier the registry calls once at registration
	 */
	private static Supplier<Properties> mudBrickProperties(Block copyFrom, MapColor color, SoundType sound) {
		// The four vanilla mud brick blocks never call dropsLike, so ofFullCopy carries a null `drops` and
		// cannot steal a vanilla loot table id the way it would for a block built with one
		return () -> Properties.ofFullCopy(copyFrom).mapColor(color).sound(sound);
	}
}
