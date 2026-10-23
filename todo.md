Phase 9b is done. Four mod ids, four jars, a generated resource partition, and all five boot
configurations reaching Done with zero recipe errors and zero tag errors.

The 2026-08-02 note said the full install was green and left core-only "still logs 65 recipe and 28 tag
errors". Re-running core-only against that day's final commit showed it was worse: a hard crash. One
butterfly mutation recipe sat in core because folderOwners listed bee_mutation and tree_mutation and
not butterfly_mutation, and reading it throws IllegalStateException out of the genetic manager, which
RecipeManager does not catch.

The rule was answering the wrong question. Every rule in the cascade - folder, exact name, recipe
result, longest prefix, texture - answers what a file is *about*. Correctness turns on what a file can
*see*. A fabricator recipe whose result is a core item and whose ingredient is an arboriculture log is
about core and belongs to arboriculture. So the closure became the primary rule:

  A jar may ship a file only if every forestry id it names is present wherever that jar is.
  The cascade proposes; the closure promotes to the least derived jar that satisfies it, and
  fails the build when no jar does.

109 files moved. The build now stops on cross-jar data instead of a server dying at datapack load.

Three things the closure needed that did not exist:
- The manifest walked getFeatures(), 1,890 ids. POI types take a DeferredRegister straight off the
  module, and recipeType() was the only factory in FeatureRegistry that never called register(). It
  now walks every DeferredRegister a module owns: 5,361 ids. That subsumes the WoodAccess fix the
  spec predicted - those ids were always registered that way.
- Bare ids are not unique across registries. forestry:refractory_wax is a core item and an apiculture
  particle; forestry:escritoire is a core block and an apiculture poi. Registry-qualified keys now sit
  beside the bare ones, and an ambiguous bare key resolves to the jar that constrains least.
- Species and taxa carry no registry id. Species types come from the folder rules, a genus is claimed
  by the species naming it, and ranks above propagate upward until their children disagree. 13 taxa
  stay in core - animalia, arthropoda, insecta and the kingdoms - which is right.

Some files cannot be owned by any jar. Tags, data maps and global_loot_modifiers.json are assembled by
the game from every pack supplying one, so each jar gets a variant holding only its own entries. Ten
files split then, by post-hoc inference over what the closure had already assigned; that mechanism is
gone now, and each jar's providers write their own variant straight into that jar's root, en_us
included. global_loot_modifiers.json is the sharp one - it names the arboriculture grafter, so giving
it a single owner would have left core with no loot modifier list at all.

Cross-jar recipes have a policy now, and it is not neoforge:conditions. An absent item is an unknown
registry key and fails the recipe; an absent tag resolves empty. So the genetic filter's caterpillar
and propolis became a forestry:genetic_samples tag both jars contribute to, and the recipe ships in
core and crafts with whatever is installed.

The cross-jar loot modifier needed no redesign: ConditionLootModifier already skips absent sub-tables.
Splitting it per module would have been unsafe - the re-entrancy guard is per instance, so three
modifiers on one chest table would each fire inside the others and duplicate the loot. What was
actually broken: nether_bridge.json prefix-matched the agriculture feature forestry:nether.

Enumerated datagen diff: genetic_filter.json and a new genetic_samples.json tag. Nothing else.

One defect no boot could catch. An exclude at the top of a Copy task applies to every source it has,
and a split file's owner is 'split', not 'core' - so core's processResources excluded core's own
partitioned share, and the core jar shipped no en_us and no global_loot_modifiers.json. Every boot
still reached Done and logged nothing: a missing loot modifier list is not an error, it just stops
injecting loot, and a headless server never renders a lang key. Inspecting the built jars found it.
A boot proves what loads; only the artifact proves what shipped. checkJarPartition checked that split
ownership on every `check` at the time; the ownership machinery it read is gone now, and the guard
that replaced it is narrower, every jar must ship en_us and core's copy must differ from the
hand-written file, with no equivalent yet for global_loot_modifiers.json.

Two harness traps worth knowing before rerunning the boots. A server run never stops on its own, so
the forked JVM outlives the gradle task and keeps its port; the next configuration then fails to bind
and stops before loading any datapack, which reads as zero recipe errors and zero tag errors and no
Done. Each config now takes its own port, 25566 to 25570. Read the Done line, not the absence of
errors.

Still to do: publish the artifacts.
