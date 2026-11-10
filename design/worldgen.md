# Worldgen: the structure processor and placement filter kinds

Draft of 2026-11-09 from a scout of the old tree's worldgen folder (eight structure processors, a rule test, two
placement filters, the ore helper, three features, the biome modifiers) and its structure datagen, against vanilla
26.1.2's template system and placement code with NeoForge 26.1.2.122's patches; four readers, each checked by a
refuter. The author chose the scope the same day: every generic kind vanilla cannot replace. Challenged the same
day by four reviewers (rules, fidelity to the old kinds, 26.1.2 feasibility, proof) and a judge; 10 findings of 19
confirmed and folded in. The author decided the open choices on 2026-11-10, see the end. Built on 2026-11-10, see "Built 2026-11-10" at the end.

## What the old tree did

`JolCraftProcessors` registered eight structure processor types, each paired in a `ProcessorDef` record with a
processor-list key of the same name, and logged their count; `JolCraftRuleTests` registered one rule test and
`JolCraftPlacementModifierTypes` two placement filters. Sorted by what 26.1.2 offers:

- **Vanilla JSON now, no kind.** `random_replace_with_loot` is one `minecraft:rule` processor with two ordered
  rules, each `random_block_match` gravel to `suspicious_gravel` with an `append_loot` block entity modifier; both
  rules sit in one processor so they share its random and the old odds hold (two processors would make the second
  7% instead of 7.92%). `random_not_air` is a rule with `always_true` input and location predicates and a
  `linear_pos` position predicate (`min_chance` = `max_chance` = the chance, `min_dist` 0, `max_dist` 1), after three
  guard rules that map `air`, `cave_air` and `void_air` to themselves through `block_match`; the only draw is the
  first float of the position's random in both forms, and the one difference left is `<=` against `<`. The per-block
  half of `structure_void` is `block_ignore` with `[minecraft:structure_void]`. These are recorded here so that the
  fortress's processor lists write them and no one ports the old classes.
- **Feature code.** `bookshelf_tome` and `random_fermenting_container` hard-code the mod's items, blocks and
  effects; they come with the tome and brew systems, declared in those systems' own content. The geode copy and the
  two huge mushrooms are features of `flora`; the biome modifiers belong to the systems that own what they place.
- **Dropped.** `JolCraftOreReplacement` copies vanilla's private ore placement helpers, and its only callers are
  the mod's ores.
- **Generic, no vanilla equivalent, ported here.** `add_loot_table`, `min_y` and `max_y`, `random_cobweb`,
  `random_cave_in` and `lantern`, described below. Every old user is fortress, misc-pool or plant content:
  `add_loot_table` on the strongbox and barrel lists, `random_cobweb` at 0.01 on most fortress lists and 0.10 on the
  collapsed one, `random_cave_in` at 0.10 on the cave-in list, `lantern` on the misc lantern pool, `max_y` 64 on
  bloodroot and `min_y` 100 and 115 on skybell; the old `min_y` 0 never filtered anything.

## What 26.1.2 gives

- `StructureProcessor`: `processBlock(level, targetPosition, referencePos, original, processed, settings)` per
  block, deprecated by NeoForge in favour of `process(..., template)`, which calls it, while `CappedProcessor` calls
  its delegate's `processBlock` directly, so vanilla's own per-block processors still override `processBlock`;
  `finalizeProcessing(level, position, referencePos, original, processed, settings)` once over the whole list;
  `getType()` protected abstract. `StructureProcessorType<P>` is `MapCodec<P> codec()`, registered under
  `Registries.STRUCTURE_PROCESSOR` and dispatched by `StructureProcessorType.SINGLE_CODEC`.
  `PlacementFilter.shouldPlace(context, random, origin)`, with `PlacementModifierType<P>` under
  `Registries.PLACEMENT_MODIFIER_TYPE`, dispatched by `PlacementModifier.CODEC`. A registry finds a value's name by
  identity, so each kind has exactly one type instance.
- Order, in `StructureTemplate.processBlockInfos`: each block goes through every processor's `process` in list
  order until one drops it, then every `finalizeProcessing` runs in list order over the whole list. A finalize result
  is invisible to every per-block pass, and a `structure_void` emitted in a finalize is placed as a real block unless
  a later finalize removes it. A block missing from the final list is not placed, so the world's own block stays; the
  template's entities are added after the blocks unless the settings ignore entities. The list is the template's
  whole palette in every placement call, one call per chunk the piece touches; the chunk's box is applied after
  processing.
- Seeding: `settings.getRandom(pos)` is `RandomSource.create(Mth.getSeed(pos))` unless a random was set, and the
  jigsaw path sets none; vanilla's rule processor seeds the same way, so two chance kinds stacked on the same cells
  read the same first float. `CappedProcessor` seeds its whole-piece decision from the level seed forked at
  `position`.
- `placeInWorld` overwrites `LootTableSeed` with its own random for every `RandomizableContainer` block entity
  before loading the tag; vanilla's `append_loot` writes `LootTable` through `LootTable.KEY_CODEC` and a seed.
- `CappedProcessor` pairs the original and the processed list by index and edits the processed one in place: a
  finalize before it that answers an immutable list makes it throw, and one that changes the list's size makes it
  log an error and apply nothing.
- While structures and features are placed, the region holds chunks out to a distance of 8, but only the generating
  chunk and its direct neighbours have terrain (the `FEATURES` step needs `CARVERS` at radius 1 and
  `STRUCTURE_STARTS` at radius 8): a block further out reads as air, and one outside the region crashes
  `getBlockState`.
- Structure placement runs on the world-generation threads, not the server thread.
- None of these registries is synced: processor lists, template pools and placed features stay on the server, so
  the kinds never reach a client.

## The system

| Piece | What it is |
|---|---|
| **System.** | `module/system/worldgen`, the first system: the world-generation plumbing no feature owns, which in the finished tree is these kinds, the jigsaw structure base with its terrain veto and the datagen helpers for pools and processor lists. `Worldgen`, its `@ModSystem(Dictionary.WORLDGEN)` class, takes nothing; the root builds it. The kinds need no behaviour of the system, since the game calls them, so the class holds none until the jigsaw base. Beside it one `@Content` and the kinds. |
| **Content.** | `WorldgenContent`, `@Content`: `PROCESSORS`, a `DeferredRegister` of `Registries.STRUCTURE_PROCESSOR`, with the holders `ADD_LOOT_TABLE`, `RANDOM_COBWEB`, `RANDOM_CAVE_IN` and `HANGING`, each registered as `() -> Kind.CODEC`; `PLACEMENTS`, of `Registries.PLACEMENT_MODIFIER_TYPE`, with `HEIGHT_FILTER`; `register(IEventBus)`, called once by the root on the mod bus. Ids under the mod's namespace, from Dictionary words. |
| **`add_loot_table`.** | `AddLootTableProcessor`, per block: a block whose state `is(block)` keeps its state, so facing, waterlogging and chest type survive, and gets a copy of its tag with the loot table written as vanilla's `append_loot` writes it: `LootTable` through `LootTable.KEY_CODEC`, and a `LootTableSeed` from `settings.getRandom(pos)`, which `placeInWorld` replaces for a container. Anything else passes. Fields `block` (`BuiltInRegistries.BLOCK.byNameCodec()`, so an unknown block fails when the data loads) and `loot_table`. |
| **`height_filter`.** | `HeightFilter`, a `PlacementFilter`: keeps a position whose absolute Y lies within `min_inclusive` and `max_inclusive`, both optional with the defaults of vanilla's `surface_relative_threshold_filter`. Replaces `min_y` and `max_y`. |
| **`random_cobweb`.** | `RandomCobwebProcessor`, finalize: every air block of the piece with at least two of its six neighbours in the piece blocking motion becomes a cobweb, keeping its tag, at `chance`, drawn from `settings.getRandom(pos)` for air blocks only. The world around the piece is not consulted. Field `chance`, 0 to 1. |
| **`random_cave_in`.** | `RandomCaveInProcessor`, finalize, once per piece at `chance`, seeded from the level seed forked at the piece's `position` as vanilla's capped processor seeds. It runs the old algorithm on a working map of the piece by position, where a block absent from the piece counts as not blocking motion and as open. A top is a block with nothing above it in the map, one of the four blocks below it not blocking motion, and an open side (a horizontal neighbour that is air or absent) at it or within three below. One top is drawn, then a half-width from 1 to 3; the top's column is carved, then, each at 0.65, every other column of the square of that half-width around it (the centre skipped) whose top qualifies against the map as carved so far. Carving walks down from the column's first block that blocks motion: every block without a block entity becomes a structure void in the map, a block entity is kept and the walk goes on past it, and at the first air or absent block the last voided block becomes air instead; the walk stops at the level's minimum Y. The answer is the given list in its order with the voided blocks dropped and the air in place, so the world's own block stays where a block was dropped and no remover follows. Field `chance`, 0 to 1. |
| **`hanging`.** | `HangingProcessor`, finalize, no fields: it reads the world block above the first block of maximum Y in list order; when that block blocks motion the piece is placed, otherwise the answer is empty and none of the piece's blocks is placed (its entities still are, so a hanging piece holds none). It decides once per placement call, so it serves a piece that lies within one chunk, as a hanging lantern does: such a piece is placed in one call, and the block above it lies in the generating chunk. Renamed from `lantern`, which named its one use. |
| **Kinds.** | Each a public final class extending the game's type, an `@Actor` since the game decodes and calls it: its `CODEC` a public static final `MapCodec`, its fields private and immutable, a constructor for datagen and scenes, `getType()` or `type()` answering the content's holder. A per-block kind overrides `processBlock`, as vanilla's own do, so it works under a capped processor too. No caches, since placement runs on the world-generation threads. A finalize answers a mutable list in the order it was given, less what it drops, since a capped processor after it pairs the lists by index. No nullness annotations of their own: the methods they override carry the game's. |

## Lifecycle and rules

Built: the root builds `Worldgen` and calls `WorldgenContent.register(modBus)` once; nothing listens. Used: the
game decodes a processor list, template pool or placed feature that names a kind and calls it while it places a
structure piece or a feature, on the world-generation threads. A kind reads its fields, its piece and, for
`hanging`, the world above the piece; it posts nothing and reaches no module. The order of a list is the data's: a
list that puts `random_cobweb` after `random_cave_in` sees the air the cave-in leaves, which is the old cave-in
list's look, and a capped processor goes before `random_cave_in` and `hanging`, never after, since they change the
list's size and capped would then apply nothing.

The datagen seam is fixed with this step. A `RegistryContributor` is handed the `RegistrySetBuilder` itself, and
two contributors that add the same registry crash `runData`: `BuildState.create` puts every add's registry into an
`ImmutableMap.Builder`, which refuses a duplicate key, and `buildPatch` would keep only one add's entries per key
anyway. Template pools, processor lists and biome modifiers are each written by several systems later, so the
contributor is handed the tree's own collector, `provider/DatapackEntries` with `add(key, bootstrap)`, and `gather`
adds each registry once with a bootstrap that runs every contributor's in discovery order. The javadoc says one add
per registry per data run. No contributor exists yet, so the generated data does not change, and the merge is not
exercised until the first two contributors of one registry exist.

## Proof

Unit tests, with the main mod loaded, whose registers are filled before the tests run: each kind round trips
through `StructureProcessorType.SINGLE_CODEC` or `PlacementModifier.CODEC` under its id, a `chance` outside 0 to 1
and an unknown block are refused. Scenes on the game-test server, under `gametest/module/system/worldgen`, each
building its template in code: it places vanilla blocks in the arena, fills a template from them keeping the air
(structure voids are the only blocks ignored), clears the area and places the template through
`new StructurePlaceSettings().addProcessor(kind)`, then asserts blocks. `add_loot_table`: a waterlogged chest facing
east keeps its facing and its water and carries vanilla's `chests/simple_dungeon` table, a block of another kind
passes untouched. `random_cobweb` at 1: an air block between two blocks of the piece becomes a cobweb, one with a
single such neighbour stays air, a solid world block beside the piece does not count; at 0 nothing changes; a
vanilla `capped` processor after it still applies its delegate. `random_cave_in` at 1 on a hollow box with a ceiling
and an open side, the piece's cells filled first with a vanilla block the template does not hold: at least one
column carved, the filler showing where blocks were dropped, the lowest carved block air, a block entity in the
column kept; at 0 the piece unchanged; the same level seed and position carve the same columns twice; the answer
keeps the given order, checked through `processBlockInfos` over a hand-built list. `hanging`, over the same filler:
under a solid world block the piece is placed whole, under air none of its blocks is placed and the filler stays.
`height_filter`: a real `PlacementContext` over the scene's level keeps positions at the minimum and the maximum and
drops one below and one above, and an absent bound filters nothing on its side. No asset files and no dev content:
the templates are built from vanilla blocks in code, the loot table is vanilla's. The datagen merge has no proof in
this step, as said above.

## Where things go, and the order

`module/system/worldgen/Worldgen`, `WorldgenContent`, `AddLootTableProcessor`, `HeightFilter`,
`RandomCobwebProcessor`, `RandomCaveInProcessor`, `HangingProcessor`; the Dictionary words the ids and fields need;
the root builds the system and registers the content. The unit test beside the kinds in `src/test`,
`WorldgenScenes` in `gametest/module/system/worldgen`. In datagen, `RegistryContributor` takes `DatapackEntries`,
and `JolCraftDatagen.gather` merges. Docs: the `worldgen` row of `module-plan.md` (takes nothing; the kinds now, the
jigsaw base and the datagen helpers with the fortress; the vanilla JSON above) and its Status; in `module-map.md`
and the `@Content` javadoc, a content depends on its folder's values and actors, since every content registers its
folder's actors (this one the kinds, a feature's its blocks and items); a `CLAUDE.md` line for `module/system`,
mirrored into `AGENTS.md`. One step.

Not in this step: the jigsaw structure base with the terrain veto, the structures and structure sets, which come
with the fortress and give `Worldgen` its behaviour; the template pool and processor-list helpers and a check of
the names they hold (the bootstrap lookup makes holders on demand, so a misspelt pool, processor list or template
fails only at world load), with the first structure; `bookshelf_tome` and `random_fermenting_container`, with tome
and brew.

## Decided 2026-11-10

- `Worldgen` is a system class with no behaviour until the jigsaw base. No rule bends to the tree's current state,
  so there is no declaration-only system folder.
- `min_y` and `max_y` become one `height_filter` with vanilla's field names.
- `lantern` becomes `hanging`, places none of the piece's blocks when nothing is above it, and serves one-chunk
  pieces.
- `random_cave_in` keeps the old algorithm, drops its carved blocks instead of emitting voids (so no structure-void
  kind), keeps the given order and seeds per piece position as vanilla's capped processor.
- Names are left to the design: the old ids stay and `add_loot_table`'s `input` becomes `block`, as vanilla's
  `block_match` names it.
- The datagen collector fix comes with this step; bug fixes are not choices.

Kept from the draft: not a primitive and not the `registration` primitive; the kinds are actors; `add_loot_table`
writes the table as `append_loot` does; every chance is drawn unsalted, as vanilla's own processors draw.

## Challenged 2026-11-09

Confirmed and folded in: `hanging`'s first guard, reading only where the region holds the chunk, did not make a
piece's placement calls agree, since the region holds chunks out to 8 and only the neighbours have terrain, so the
kind now states its one-chunk scope and the block it reads; a system folder of declarations alone would have been a
rule of its own, so `Worldgen` is a system class without behaviour; a capped processor after a kind that changes
the list's size applies nothing, so lists put capped first and the proof checks capped after `random_cobweb`
instead of "places without throwing"; `random_cave_in` now states the old qualification exactly, absent counting as
not blocking and as open, the square of the drawn half-width, each column tested against the map as carved so far;
the datagen merge cannot run without contributors, so the proof says so instead of calling a review a proof; the
cave-in and hanging scenes fill the piece's cells first, so a dropped block and a placed air block differ; a
content depends on its folder's actors, which the map's wording now says; `hanging` names the block it reads above;
`add_loot_table`'s field rename is owned; `hanging`'s empty answer still lets the template's entities in, which is
said. Refuted: that unit tests run before the main mod's registers are filled (FML's JUnit launcher posts every
`RegisterEvent` first), that a per-block kind should override `process` (capped calls `processBlock` directly), and
four points of taste or implementation detail.

## Built 2026-11-10

Built as drafted: `Worldgen`, `WorldgenContent` and the five kinds in `module/system/worldgen`, the root building the
system and registering the content, the Dictionary words, `DatapackEntries` with `RegistryContributor` and
`JolCraftDatagen.gather` merging, `WorldgenKindsTest` (six unit tests) and `WorldgenScenes` (eleven scenes). Small
departures: the merge of `DatapackEntries` sits in its `builder()` method, which `gather` calls, rather than in
`gather` itself; `height_filter` and the chance kinds take plain constructors (`HeightFilter(min, max)` with the integer
extremes for an absent bound) for datagen and scenes; the draft's jspecify was wrong, the tree uses JetBrains'
annotations where it annotates at all, and the kinds need none. A five-lens review and a judge confirmed one finding of
three: the cobweb scene could not catch a kind that counts the world's blocks, so the scene now gives the checked cell
two world neighbours.
