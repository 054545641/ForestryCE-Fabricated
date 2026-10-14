package forestry.core.data.models;

import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.VanillaWoodType;
import forestry.arboriculture.wood.ForestryButtonBlock;
import forestry.arboriculture.wood.ForestryDoorBlock;
import forestry.arboriculture.wood.ForestryFenceBlock;
import forestry.arboriculture.wood.ForestryFenceGateBlock;
import forestry.arboriculture.wood.ForestryHangingSignBlock;
import forestry.arboriculture.wood.ForestryLogBlock;
import forestry.arboriculture.wood.ForestryPressurePlateBlock;
import forestry.arboriculture.wood.ForestrySlabBlock;
import forestry.arboriculture.wood.ForestryStairsBlock;
import forestry.arboriculture.wood.ForestryStandingSignBlock;
import forestry.arboriculture.wood.ForestryTrapdoorBlock;
import forestry.arboriculture.wood.ForestryWallHangingSignBlock;
import forestry.arboriculture.wood.ForestryWallSignBlock;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.core.platform.registration.FeatureBlockGroup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import forestry.core.data.models.ModelAsset;
import org.jetbrains.annotations.NotNull;

public class ForestryWoodModelProvider extends ForestryBlockStateProvider {
	public ForestryWoodModelProvider(PackOutput output) {
		super(output);
	}

	@Override
	protected void registerStatesAndModels() {
		// Vanilla fireproof wood types
		for (VanillaWoodType woodType : VanillaWoodType.VALUES) {
			// planks
			Block planks = ArboricultureBlocks.PLANKS_VANILLA_FIREPROOF.get(woodType).block();
			String woodTypeName = woodType.getSerializedName();
			String planksName = woodTypeName + "_planks";
			ModelAsset planksModel = existingMcBlock(planksName);
			simpleBlock(planks, planksModel);
			generic3d(planks, mcLoc(planksName));
			// log
			ForestryLogBlock log = ArboricultureBlocks.LOGS_VANILLA_FIREPROOF.get(woodType).block();
			String logName = woodTypeName + "_log";
			ModelAsset logModel = existingMcBlock(logName);
			axisBlock(log, logModel, logModel);
			generic3d(log, mcLoc(logName));
			// wood
			ForestryLogBlock wood = ArboricultureBlocks.WOOD_VANILLA_FIREPROOF.get(woodType).block();
			String woodName = woodTypeName + "_wood";
			ModelAsset woodModel = existingMcBlock(woodName);
			axisBlock(wood, woodModel, woodModel);
			generic3d(wood, mcLoc(woodName));
			// stripped log
			ForestryLogBlock strippedLog = ArboricultureBlocks.STRIPPED_LOGS_VANILLA_FIREPROOF.get(woodType).block();
			String strippedLogName = "stripped_" + woodTypeName + "_log";
			ModelAsset strippedLogModel = existingMcBlock(strippedLogName);
			axisBlock(strippedLog, strippedLogModel, strippedLogModel);
			generic3d(strippedLog, mcLoc(strippedLogName));
			// stripped wood
			ForestryLogBlock strippedWood = ArboricultureBlocks.STRIPPED_WOOD_VANILLA_FIREPROOF.get(woodType).block();
			String strippedWoodName = "stripped_" + woodTypeName + "_wood";
			ModelAsset strippedWoodModel = existingMcBlock(strippedWoodName);
			axisBlock(strippedWood, strippedWoodModel, strippedWoodModel);
			generic3d(strippedWood, mcLoc(strippedWoodName));
			// slab
			SlabBlock slab = ArboricultureBlocks.SLABS_VANILLA_FIREPROOF.get(woodType).block();
			String slabName = woodTypeName + "_slab";
			ModelAsset bottomSlabModel = existingMcBlock(slabName);
			ModelAsset topSlabModel = existingMcBlock(slabName + "_top");
			slabBlock(slab, bottomSlabModel, topSlabModel, planksModel);
			generic3d(slab, mcLoc(slabName));
			// stairs
			StairBlock stairs = ArboricultureBlocks.STAIRS_VANILLA_FIREPROOF.get(woodType).block();
			String stairsName = woodTypeName + "_stairs";
			ModelAsset stairsModel = existingMcBlock(stairsName);
			ModelAsset innerStairsModel = existingMcBlock(stairsName + "_inner");
			ModelAsset outerStairsModel = existingMcBlock(stairsName + "_outer");
			stairsBlock(stairs, stairsModel, innerStairsModel, outerStairsModel);
			generic3d(stairs, mcLoc(stairsName));
			// fence
			ForestryFenceBlock fence = ArboricultureBlocks.FENCES_VANILLA_FIREPROOF.get(woodType).block();
			String fenceName = woodTypeName + "_fence";
			ModelAsset fencePostModel = existingMcBlock(fenceName + "_post");
			ModelAsset fenceSideModel = existingMcBlock(fenceName + "_side");
			ModelAsset fenceInventoryModel = existingMcBlock(fenceName + "_inventory");
			fourWayBlock(fence, fencePostModel, fenceSideModel);
			itemModels().withExistingParent(path(fence), fenceInventoryModel.getLocation());
			// fence gate
			ForestryFenceGateBlock fenceGate = ArboricultureBlocks.FENCE_GATES_VANILLA_FIREPROOF.get(woodType).block();
			String fenceGateName = woodTypeName + "_fence_gate";
			ModelAsset gateModel = existingMcBlock(fenceGateName);
			ModelAsset gateOpenModel = existingMcBlock(fenceGateName + "_open");
			ModelAsset gateWallModel = existingMcBlock(fenceGateName + "_wall");
			ModelAsset gateWallOpenModel = existingMcBlock(fenceGateName + "_wall_open");
			fenceGateBlock(fenceGate, gateModel, gateOpenModel, gateWallModel, gateWallOpenModel);
			generic3d(fenceGate, mcLoc(fenceGateName));
		}

		// Forestry wood types
		for (ForestryWoodType woodType : ForestryWoodType.VALUES) {
			// Planks
			Block planks = ArboricultureBlocks.PLANKS.get(woodType).block();
			Block fireproofPlanks = ArboricultureBlocks.PLANKS_FIREPROOF.get(woodType).block();
			ModelAsset planksModel = cubeAll(planks);

			simpleBlock(planks);
			simpleBlock(fireproofPlanks, planksModel);
			generic3d(planks);
			generic3d(fireproofPlanks, planks);

			// Logs, Wood, Stripped Logs, Stripped Wood
			ForestryLogBlock log = ArboricultureBlocks.LOGS.get(woodType).block();
			Identifier logTexture = blockTexture(log);
			Identifier strippedLogTexture = withPrefix("stripped_", logTexture);

			logLike(woodType, ArboricultureBlocks.LOGS, ArboricultureBlocks.LOGS_FIREPROOF, logTexture, withSuffix(logTexture, "_top"));
			logLike(woodType, ArboricultureBlocks.STRIPPED_LOGS, ArboricultureBlocks.STRIPPED_LOGS_FIREPROOF, strippedLogTexture, withPrefix("stripped_", withSuffix(logTexture, "_top")));
			logLike(woodType, ArboricultureBlocks.STRIPPED_WOOD, ArboricultureBlocks.STRIPPED_WOOD_FIREPROOF, strippedLogTexture, strippedLogTexture);
			logLike(woodType, ArboricultureBlocks.WOOD, ArboricultureBlocks.WOOD_FIREPROOF, logTexture, logTexture);

			// Slab
			ForestrySlabBlock slab = ArboricultureBlocks.SLABS.get(woodType).block();
			ForestrySlabBlock fireproofSlab = ArboricultureBlocks.SLABS_FIREPROOF.get(woodType).block();
			Identifier planksLoc = blockTexture(planks);
			ModelAsset bottomSlabModel = models().slab(path(slab), planksLoc, planksLoc, planksLoc);
			ModelAsset topSlabModel = models().slabTop(path(slab) + "_top", planksLoc, planksLoc, planksLoc);
			slabBlock(slab, bottomSlabModel, topSlabModel, planksModel);
			slabBlock(fireproofSlab, bottomSlabModel, topSlabModel, planksModel);
			generic3d(slab);
			generic3d(fireproofSlab, slab);

			// Stairs
			ForestryStairsBlock stairs = ArboricultureBlocks.STAIRS.get(woodType).block();
			ForestryStairsBlock fireproofStairs = ArboricultureBlocks.STAIRS_FIREPROOF.get(woodType).block();
			ModelAsset stairsModel = models().stairs(path(stairs), planksLoc, planksLoc, planksLoc);
			ModelAsset innerStairsModel = models().stairsInner(path(stairs) + "_inner", planksLoc, planksLoc, planksLoc);
			ModelAsset outerStairsModel = models().stairsOuter(path(stairs) + "_outer", planksLoc, planksLoc, planksLoc);
			stairsBlock(stairs, stairsModel, innerStairsModel, outerStairsModel);
			stairsBlock(fireproofStairs, stairsModel, innerStairsModel, outerStairsModel);
			generic3d(stairs);
			generic3d(fireproofStairs, stairs);

			// Fence
			ForestryFenceBlock fence = ArboricultureBlocks.FENCES.get(woodType).block();
			ForestryFenceBlock fireproofFence = ArboricultureBlocks.FENCES_FIREPROOF.get(woodType).block();
			ModelAsset fencePostModel = models().fencePost(path(fence) + "_post", planksLoc);
			ModelAsset fenceSideModel = models().fenceSide(path(fence) + "_side", planksLoc);
			ModelAsset fenceInventoryModel = models().fenceInventory(path(fence) + "_inventory", planksLoc);
			fourWayBlock(fence, fencePostModel, fenceSideModel);
			fourWayBlock(fireproofFence, fencePostModel, fenceSideModel);
			itemModels().withExistingParent(path(fence), fenceInventoryModel.getLocation());
			itemModels().withExistingParent(path(fireproofFence), fenceInventoryModel.getLocation());

			// Fence Gate
			ForestryFenceGateBlock fenceGate = ArboricultureBlocks.FENCE_GATES.get(woodType).block();
			ForestryFenceGateBlock fireproofFenceGate = ArboricultureBlocks.FENCE_GATES_FIREPROOF.get(woodType).block();
			ModelAsset gateModel = models().fenceGate(path(fenceGate), planksLoc);
			ModelAsset gateOpenModel = models().fenceGateOpen(path(fenceGate) + "_open", planksLoc);
			ModelAsset gateWallModel = models().fenceGateWall(path(fenceGate) + "_wall", planksLoc);
			ModelAsset gateWallOpenModel = models().fenceGateWallOpen(path(fenceGate) + "_wall_open", planksLoc);
			fenceGateBlock(fenceGate, gateModel, gateOpenModel, gateWallModel, gateWallOpenModel);
			fenceGateBlock(fireproofFenceGate, gateModel, gateOpenModel, gateWallModel, gateWallOpenModel);
			generic3d(fenceGate);
			generic3d(fireproofFenceGate, fenceGate);

			// Door
			ForestryDoorBlock door = ArboricultureBlocks.DOORS.get(woodType).block();
			doorBlock(door, withSuffix(blockTexture(door), "_bottom"), withSuffix(blockTexture(door), "_top"));
			generic2d(door);

			// Trapdoor
			ForestryTrapdoorBlock trapdoor = ArboricultureBlocks.TRAPDOORS.get(woodType).block();
			trapdoorBlockWithRenderType(trapdoor, blockTexture(trapdoor), true, "cutout");
			itemModels().trapdoorBottom(path(trapdoor), blockTexture(trapdoor));

			// Sign
			ForestryStandingSignBlock sign = ArboricultureBlocks.SIGN.get(woodType).block();
			ForestryWallSignBlock wallSign = ArboricultureBlocks.WALL_SIGN.get(woodType).block();
			legacySign(woodType, sign, wallSign, false);
			generic2d(sign);

			// Hanging Sign
			ForestryHangingSignBlock hangingSign = ArboricultureBlocks.HANGING_SIGN.get(woodType).block();
			ForestryWallHangingSignBlock hangingWallSign = ArboricultureBlocks.WALL_HANGING_SIGN.get(woodType).block();
			legacySign(woodType, hangingSign, hangingWallSign, true);
			generic2d(hangingSign);

			// Button
			ForestryButtonBlock button = ArboricultureBlocks.BUTTON.get(woodType).block();
			buttonBlock(button, planksLoc);
			ModelAsset buttonInventoryModel = itemModels().buttonInventory(path(button) + "_inventory", planksLoc);
			itemModels().withExistingParent(path(button), buttonInventoryModel.getLocation());

			// Pressure plate
			ForestryPressurePlateBlock pressurePlate = ArboricultureBlocks.PRESSURE_PLATE.get(woodType).block();
			pressurePlateBlock(pressurePlate, planksLoc);
			generic3d(pressurePlate);
		}
	}

	private void logLike(ForestryWoodType woodType, FeatureBlockGroup<ForestryLogBlock, ForestryWoodType> logs, FeatureBlockGroup<ForestryLogBlock, ForestryWoodType> fireproofLogs, Identifier sideTexture, Identifier topTexture) {
		ForestryLogBlock wood = logs.get(woodType).block();
		ForestryLogBlock fireproofWood = fireproofLogs.get(woodType).block();
		ModelAsset woodModel = models().cubeColumn(path(wood), sideTexture, topTexture);
		axisBlock(wood, woodModel, woodModel);
		axisBlock(fireproofWood, woodModel, woodModel);
		generic3d(wood);
		generic3d(fireproofWood, wood);
	}

	public static String legacyTextureName(ForestryWoodType wood) {
		return switch (wood) {
			case CAMELTHORN -> "acacia_desert";
			case SOUR_CHERRY -> "hill_cherry";
			case LEMON -> "citrus";
			case GIANT_SEQUOIA -> "giganteum";
			case ZEBRANO -> "zebrawood";
			default -> wood.getSerializedName();
		};
	}

	private void legacySign(ForestryWoodType wood, Block sign, Block wallSign, boolean hanging) {
		String name=wood.getSerializedName();
		String texture=legacyTextureName(wood);
		String family=hanging?"hanging_sign":"sign";
		Identifier sprite=modLoc("entity/signs/"+(hanging?"hanging/":"")+texture);
		Identifier particle=modLoc("block/"+(hanging?"stripped_"+name+"_log":name+"_planks"));
		var variants=new com.google.gson.JsonObject();
		for(boolean attached : hanging?new boolean[]{false,true}:new boolean[]{false}) {
			for(int rotation=0;rotation<16;rotation++) {
				String model=name+"_"+family+(attached?"_attached":"")+"_rot_"+(rotation%4);
				if(rotation<4) models().withExistingParent(model,modLoc("block/template_legacy_"+(attached?"attached_":"")+family+"_rot_"+rotation)).texture("all",sprite).texture("particle",particle);
				variants.add((hanging?"attached="+attached+",":"")+"rotation="+rotation,new ModelVariant(new ModelAsset(modLoc("block/"+model)),0,rotation/4*90,false).json());
			}
		}
		var state=new com.google.gson.JsonObject(); state.add("variants",variants); blockStateJson(sign,state);
		String wall=name+"_wall_"+family;
		models().withExistingParent(wall,modLoc("block/template_legacy_wall_"+family)).texture("all",sprite).texture("particle",particle);
		var wallVariants=new com.google.gson.JsonObject();
		for(var facing:net.minecraft.core.Direction.Plane.HORIZONTAL) wallVariants.add("facing="+facing.getSerializedName(),new ModelVariant(new ModelAsset(modLoc("block/"+wall)),0,(int)facing.toYRot(),false).json());
		var wallState=new com.google.gson.JsonObject(); wallState.add("variants",wallVariants); blockStateJson(wallSign,wallState);
		models().withExistingParent(name+"_"+family,modLoc("block/"+name+"_"+family+"_rot_0"));
	}

	@Override
	public @NotNull String getName() {
		return "Wood Block States: Forestry";
	}
}
