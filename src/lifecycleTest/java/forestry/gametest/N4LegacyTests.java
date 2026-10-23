package forestry.gametest;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.registries.DeferredRegister;
/** Existing gameplay regressions migrated to the 26.2 function registry. */
public final class N4LegacyTests {
 public static void register(DeferredRegister<Consumer<GameTestHelper>> tests) {
		tests.register("n4_alveary_reloads_from_saved_chunk", () -> MultiblockChunkTest::alvearyReloadsFromSavedChunk);
		tests.register("n4_farm_reloads_from_saved_chunk", () -> MultiblockChunkTest::farmReloadsFromSavedChunk);
		tests.register("n4_alveary_automation_change_survives_chunk_unload", () -> MultiblockChunkTest::alvearyAutomationChangeSurvivesChunkUnload);
		tests.register("n4_alveary_forms_when_shell_chunk_loads_later", () -> MultiblockChunkTest::alvearyFormsWhenShellChunkLoadsLater);
		tests.register("n4_alveary_across_chunks_forms_when_other_chunk_loads", () -> MultiblockChunkTest::alvearyAcrossChunksFormsWhenOtherChunkLoads);
		tests.register("n4_alveary_across_chunks_forms_when_holder_chunk_loads_last", () -> MultiblockChunkTest::alvearyAcrossChunksFormsWhenHolderChunkLoadsLast);
		tests.register("n4_apiary_automation_change_marks_chunk_unsaved", () -> BeeHousingPersistenceTest::apiaryAutomationChangeMarksChunkUnsaved);
		tests.register("n4_apiary_queen_death_marks_chunk_unsaved", () -> BeeHousingPersistenceTest::apiaryQueenDeathMarksChunkUnsaved);
		tests.register("n4_bee_house_queen_death_marks_chunk_unsaved", () -> BeeHousingPersistenceTest::beeHouseQueenDeathMarksChunkUnsaved);
		tests.register("n4_apiary_breeding_marks_chunk_unsaved", () -> BeeHousingPersistenceTest::apiaryBreedingMarksChunkUnsaved);
		tests.register("n4_sapling_with_fresh_genome_is_recognized_for_growth", () -> SaplingGrowthTest::saplingWithFreshGenomeIsRecognizedForGrowth);
		tests.register("n4_humus_becomes_sand_after_three_degrade_advances", () -> HumusDegradationTest::humusBecomesSandAfterThreeDegradeAdvances);
		tests.register("n4_humus_does_not_degrade_without_roots", () -> HumusDegradationTest::humusDoesNotDegradeWithoutRoots);
		tests.register("n4_humus_does_not_degrade_from_the_block_directly_above", () -> HumusDegradationTest::humusDoesNotDegradeFromTheBlockDirectlyAbove);
		tests.register("n4_every_degrade_value_reaches_sand", () -> HumusDegradationTest::everyDegradeValueReachesSand);
		tests.register("n4_humus_opts_into_random_ticks", () -> HumusDegradationTest::humusOptsIntoRandomTicks);
		tests.register("n4_bog_earth_becomes_peat_after_three_maturity_advances", () -> BogEarthMaturationTest::bogEarthBecomesPeatAfterThreeMaturityAdvances);
		tests.register("n4_bog_earth_does_not_mature_without_water", () -> BogEarthMaturationTest::bogEarthDoesNotMatureWithoutWater);
		tests.register("n4_bog_earth_does_not_mature_with_water_out_of_range", () -> BogEarthMaturationTest::bogEarthDoesNotMatureWithWaterOutOfRange);
		tests.register("n4_every_maturity_value_reaches_peat", () -> BogEarthMaturationTest::everyMaturityValueReachesPeat);
		tests.register("n4_bog_earth_opts_into_random_ticks", () -> BogEarthMaturationTest::bogEarthOptsIntoRandomTicks);
		tests.register("n4_shading_a_panel_stops_it_counting_as_active", () -> SolarPanelExposureTest::shadingAPanelStopsItCountingAsActive);
		tests.register("n4_breaking_a_panel_releases_everything_past_the_split", () -> SolarArrayConnectivityTest::breakingAPanelReleasesEverythingPastTheSplit);
		tests.register("n4_genetic_filter_supplies_a_server_ticker", () -> GeneticFilterTickerTest::geneticFilterSuppliesAServerTicker);
		tests.register("n4_genetic_filter_forwards_items_to_adjacent_inventory", () -> GeneticFilterTickerTest::geneticFilterForwardsItemsToAdjacentInventory);
		tests.register("n4_default_product_matches_plain_product_json", () -> CentrifugeProductDispatchTest::defaultProductMatchesPlainProductJson);
		tests.register("n4_dynamic_product_round_trips_with_type", () -> CentrifugeProductDispatchTest::dynamicProductRoundTripsWithType);
		tests.register("n4_unknown_product_type_fails_instead_of_falling_back", () -> CentrifugeProductDispatchTest::unknownProductTypeFailsInsteadOfFallingBack);
		tests.register("n4_unregistered_product_type_fails_to_encode", () -> CentrifugeProductDispatchTest::unregisteredProductTypeFailsToEncode);
		tests.register("n4_dispatch_list_stream_round_trip", () -> CentrifugeProductDispatchTest::dispatchListStreamRoundTrip);
		tests.register("n4_centrifuge_recipes_resolve_products", () -> CentrifugeProductDispatchTest::centrifugeRecipesResolveProducts);
		tests.register("n4_a_cost_spread_over_slots_is_paid_from_all_of_them", () -> SmelterResourceRemovalTest::aCostSpreadOverSlotsIsPaidFromAllOfThem);
		tests.register("n4_the_last_input_slot_is_usable", () -> SmelterResourceRemovalTest::theLastInputSlotIsUsable);
		tests.register("n4_an_unpayable_cost_changes_nothing", () -> SmelterResourceRemovalTest::anUnpayableCostChangesNothing);
		tests.register("n4_two_ingredients_do_not_double_spend_one_slot", () -> SmelterResourceRemovalTest::twoIngredientsDoNotDoubleSpendOneSlot);
		tests.register("n4_the_socket_reaches_the_description_packet", () -> EngineSocketTest::theSocketReachesTheDescriptionPacket);
		tests.register("n4_socketing_marks_the_chunk_dirty", () -> EngineSocketTest::socketingMarksTheChunkDirty);
		tests.register("n4_emptying_the_socket_marks_the_chunk_dirty", () -> EngineSocketTest::emptyingTheSocketMarksTheChunkDirty);
		tests.register("n4_the_socket_survives_save_and_load", () -> EngineSocketTest::theSocketSurvivesSaveAndLoad);
		tests.register("n4_default_leaves_item_name_is_per_species", () -> DefaultLeavesItemTest::defaultLeavesItemNameIsPerSpecies);
		tests.register("n4_alveary_round_trip_conserves_inventory", () -> MultiblockGameTests::alvearyRoundTripConservesInventory);
		tests.register("n4_alveary_break_conserves_inventory", () -> MultiblockGameTests::alvearyBreakConservesInventory);
		tests.register("n4_alveary_partial_break_conserves_inventory", () -> MultiblockGameTests::alvearyPartialBreakConservesInventory);
		tests.register("n4_alveary_bridge_merge_conserves_inventory", () -> MultiblockGameTests::alvearyBridgeMergeConservesInventory);
		tests.register("n4_farm_round_trip_conserves_inventory", () -> MultiblockGameTests::farmRoundTripConservesInventory);
		tests.register("n4_farm_break_conserves_inventory", () -> MultiblockGameTests::farmBreakConservesInventory);
		tests.register("n4_farm_bridge_merge_conserves_inventory", () -> MultiblockGameTests::farmBridgeMergeConservesInventory);
 }
}
