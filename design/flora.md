# Flora: the plants, their ground and what grows them

Draft of 2026-11-11, the first feature migrated. From a read of every old plant class, the two plant mixins, the
bonemeal and planting handlers, the plant worldgen, the plant datagen and the 100 plant textures, against vanilla
26.1.2 with NeoForge 26.1.2.122's patches. It is not a port: the old code is the reference for what the plants do, and
this design says how the new tree does it, with the faults listed below left behind. Four build steps after a
groundwork step. Challenged 2026-11-11 by two lenses (the rules, and 26.1.2 feasibility with fidelity to the old
tree): nineteen findings, all confirmed, one in part, and folded in, see "Challenged". The author decided the
two open choices the same day, see "Decided 2026-11-11". All of it is built, the groundwork and the four steps, see
the five "Built 2026-11-11" notes at the end. The author looked at it in the dev client on 2026-11-12, see the last note.

## What the old tree did

27 blocks and 23 items (eleven of them block items) in `world/block/custom/plant/**`, registered through three
factory classes, with their rules spread over block classes, two mixins and two event handlers:

- **Ground.** Verdant soil (crafted from mud and verdanite dust) and verdant farmland (tilled from it, always moist,
  never trampled). On them a plant ignores its light and depth rules and grows 1.5 times as fast.
- **Crops.** Barley, a wheat-like crop. Four hops (asgarnian, duskhold, krandonian, yanillian): two blocks high, grown
  in the dark on farmland. Deepslate bulbs: on deepslate at Y 0 or below, in the dark, harvested with an iron pickaxe.
  Festerling: rotten flesh planted on the top of an upright log grows into the festerling mushroom.
- **Wild plants.** Bloodroot hangs from cave ceilings and spreads by bonemeal under dirt. Skybell is a mountain
  flower, cyanella a flower of the warped forest. Duskcap and festerling are mushrooms with huge forms of their own.
- **Rules outside the blocks.** `CropBlockMixin` and `MushroomBlockMixin` hard-coded the verdant rules into vanilla's
  crops and mushrooms. `JolCraftBonemealEvents` let bonemeal on grass sprout a skybell and on warped nylium a
  cyanella. `JolCraftPlantingEventsHelper` planted the rotten flesh.
- **World.** Seven placed features through seven biome modifiers, two feature types for the huge mushrooms.

Its faults, each fixed by this design:

1. **Hops were eight blocks for four plants.** The top half was a subclass of the bottom half built with a
   `() -> null` supplier, the bottom asked `this instanceof HopsCropTopBlock`, each half copied its age to the other,
   two tags existed only so the halves could find each other, and breaking one half deleted the other without drops.
2. **The verdant rule was written five ways**: the tag `verdant` in one mixin, the soil block in the other and in two
   crops, the farmland block in a third, and a `canSustainPlant` override on the soil that held every plant but a
   crop, nether wart, stems and pitcher pods among them. Pitcher crops missed the light rule, since only one call site
   was hooked.
3. **Planting consumed the click on the server alone.** The client was never told, so it went on to the item's own use,
   which for rotten flesh is eating. The handler also re-implemented vanilla's placement gates by hand and told no
   protection mod that a block was placed.
4. **The bonemeal rule ran on invalid targets.** A grass block with a block above it is no bonemeal target, so vanilla
   consumed nothing, but the rule still rolled: free flowers.
5. **The two dark crops disagreed.** A lit bulb broke on its next random tick; a lit hop only stopped growing.
6. **Dead and wrong data.** The tag `cyanella_plantable` (verdant soil, warped nylium) was filled and never read:
   cyanella stood on verdant soil only through the soil's blanket override. The bulbs' tooltip names tuff, their tag
   does not hold it. No item tag was copied from its block tag, so the flowers were in no item flower tag.
7. **The huge mushrooms** were two classes with the trunk, the tip and a 35-line face routine copied between them, a
   configured radius neither read, and two different rules for which faces show the cap.
8. **Rules as code.** The bonemeal rules were a static map of lambdas, the planting rule named `Items.ROTTEN_FLESH`,
   and the numbers (1.5, 0.6, 3 in 50, 0.2, 0.4, Y 0, light 8) sat as literals in nine files.
9. **Datagen** copied vanilla's crop and mushroom-block generators (395 lines), wrote a moisture dispatch that pointed
   both ways at one model, and set every plant's render layer in client code.

## What 26.1.2 gives

Read in the patched sources; the old code predates all of it.

- **Plant bases.** `VegetationBlock` is the common base (`canSurvive` asks the ground's `canSustainPlant`, then
  `mayPlaceOn`); `CropBlock`, `MushroomBlock`, `FlowerBlock`, `DoublePlantBlock` and `BushBlock` extend it, and
  `BushBlock` is now a concrete bush. `FarmBlock` is `FarmlandBlock`. `onRemove` is gone
  (`affectNeighborsAfterRemoval`), `updateShape` takes a `ScheduledTickAccess` and a random, `entityInside` an effect
  applier, `TriState` is vanilla's. Shapes are `Block.column` and `getShapeForEachState`.
- **Ground is tags.** `CropBlock.mayPlaceOn` is `#supports_crops`, the growth bonus reads `#grows_crops`,
  `VegetationBlock.mayPlaceOn` is `#supports_vegetation` (`#substrate_overworld` and farmland), farmland stays under
  `#maintains_farmland`, a mushroom ignores light on `#overrides_mushroom_light_requirement`, and a huge mushroom
  stands where its config's `can_place_on` predicate holds. `isFertile` is any `FarmlandBlock` with moisture.
  `#substrate_overworld` also feeds the podzol, azalea-root, sculk and carver replaceables, so a block in it is
  replaced by them.
- **`PitcherCropBlock`** is vanilla's two-block crop: one `DoublePlantBlock` with `AGE` and `HALF`, the upper half
  placed when the age crosses a threshold, `updateShape` guarded while single, loot from the lower half.
- **`CropBlock.randomTick`** grows only at raw brightness 9 or more, through `getGrowthSpeed` (protected static, read
  by stems and the pitcher crop too) and NeoForge's `CropGrowEvent.Pre` and `Post`. `canSurvive` needs
  `hasSufficientLight` (public static, raw brightness 8 or more), which the pitcher crop also calls. No event carries
  the light or the speed. `MushroomBlock` spreads on `random.nextInt(25) == 0`, succeeds at bonemeal on
  `nextFloat() < 0.4`, and takes its huge feature as a `ResourceKey`; its bonemeal check needs that feature to be an
  `AbstractHugeMushroomFeature` over `HugeMushroomFeatureConfiguration`, and reads `4 + foliage_radius` as the height.
- **Seeds** are plain `BlockItem`s with `useItemDescriptionPrefix()`; `ItemNameBlockItem` is gone. A block's `asItem()`
  is the `BlockItem` registered for it, so a crop's pick-block is its seed with no supplier.
- **Flowers.** `FlowerBlock` must be built with stew effects, which only the brown mooshroom, the creative menu's stew
  list and vanilla's own datagen read from the block. Bees, pots and endermen go by tags and the pot's own map. The
  stew itself is one data recipe per flower (`suspicious_stew_from_allium`), its effect a
  component of the result. `FlowerPotBlock`'s block-taking constructor is deprecated for the supplier one plus
  `addPlant` on the empty pot.
- **Bonemeal.** `BonemealEvent` fires on both sides before vanilla looks at the block and carries
  `isValidBonemealTarget()`. Grass walks up to 128 spots and, at one free spot in eight, places a random configured
  feature of the biome that is in the feature tag `can_spawn_from_bone_meal`; nylium places fixed features. Neither
  takes a height. A placed feature placed by key runs its placement modifiers, and a `minecraft:biome` filter among
  them throws outside worldgen.
- **Use on a block.** `UseItemOnBlockEvent` fires on both sides in three phases; `ITEM_AFTER_BLOCK` is where an item's
  own `useOn` runs, after the block had its turn and with vanilla's sneak rule applied. `cancelWithResult(SUCCESS)`
  consumes the click on the side it is called on.
- **Worldgen.** `RandomPatchConfiguration`, `Feature.RANDOM_PATCH` and `Feature.FLOWER` are gone: a patch is a
  `simple_block` feature placed with `count`, `random_offset` and a `block_predicate_filter`. `WeightedStateProvider`
  takes a `WeightedList`. `HugeMushroomFeatureConfiguration` gained `can_place_on`. During feature placement the
  chunk is not lit and raw brightness reads 15.
- **Data maps.** NeoForge ships `compostables` on items. A data map type may be synced and may merge list values of
  several packs (`AdvancedDataMapType` with `listMerger`).
- **Client.** A block's render layer comes from its sprite's pixels (`ChunkSectionLayer.byTransparency`);
  `ItemBlockRenderTypes` is gone. A plant with transparent pixels is cut out with no code.
- **Datagen.** `BlockModelGenerators` is public: `createCropBlock`, `createCrossBlock` (also by age),
  `createPlantWithDefaultItem`, `createTrivialCube`, `createRotatedPillarWithHorizontalVariant`. Its
  `createMushroomBlock` names vanilla's inside model, and `createFarmland` vanilla's farmland.

## The system

`module/system/flora`. `Flora`, `@ModSystem(Dictionary.FLORA)`, takes `tab`, and `log` from the step that first has
something to log, the planting. Its constructor declares its
items, in the old order, to the mod's one tab, `CatalogKeys.TAB` (see "Groundwork"). It
holds the three rules that are not a block's own, as listeners on the game bus, on both sides where the event fires on
both.

### Ground

| Piece | What it is |
|---|---|
| **`FloraTags.VERDANT`** | `jolcraft:verdant`, a block tag: verdant soil and verdant farmland. The one statement of what verdant ground is; the listeners and the mod's own crops read it, and a data pack may add to it. |
| **`VerdantSoilBlock`** | An `@Actor` over `Block`. A hoe tills it to verdant farmland when the block above is air, through `getToolModifiedState`, after asking the stack whether it can till as NeoForge's own does. It sparkles (one happy-villager particle in a hundred animate ticks, shared with the farmland). No `canSustainPlant`: the tags below say what stands on it. |
| **`VerdantFarmlandBlock`** | An `@Actor` over `FarmlandBlock`. Moisture 7 from placement, restored by a random tick if anything lowered it, so it is always fertile. Under a solid block it turns to verdant soil, on its tick and on placement. `fallOn` only hurts the faller: nothing tramples it. |
| **`GrowthEvent`** | An `@Event` with four nested kinds, each the question vanilla asks about a plant with the number vanilla would use, which a listener may change: `Light` (the raw brightness a crop reads), `Speed` (a crop's growth speed), `Spread` (a mushroom's chance to spread on a random tick, 1 in 25) and `Bonemeal` (a mushroom's chance to grow huge, 0.4). Each carries the level it was asked in and the plant's position. |
| **The two mixins** | `CropBlockMixin` posts `Light` where `randomTick` and `hasSufficientLight` read the raw brightness and `Speed` where `getGrowthSpeed` returns. `MushroomBlockMixin` posts `Spread` at the spread roll and `Bonemeal` at the 0.4. They name `GrowthEvent` and nothing else of ours, through MixinExtras' expression modifiers, so other mods' mixins on the same lines still apply. Those injectors take no `@Desc` method target, so the host method is named by its bare name, unique in its class, and the call inside by `@At(desc = @Desc(...))`; no descriptor string is written. A chance no listener moved leaves the game's own roll untouched. |
| **The verdant rule** | `Flora` listens to the four: when the block under the plant is in `VERDANT`, the light is at least 9, the speed is multiplied by 1.5, the spread chance is 0.06 and the bonemeal chance 0.6. The numbers are named constants of `Flora`. It reads only the level the event hands it, so it is safe on a worldgen thread and right on the client, where placement is predicted. |

Hooking `hasSufficientLight` itself, not its one caller, covers the pitcher crop; hooking `getGrowthSpeed` covers stems,
the pitcher crop and the mod's own crops, which call it. A stem's own light check and the sweet berry bush's are not
hooked, as before: on verdant farmland a stem grows faster and still wants light.

What stands on verdant ground is tags, written by flora's tag contributor:
- Verdant soil joins `supports_vegetation`, `supports_bamboo`, `azalea_grows_on`,
  `overrides_mushroom_light_requirement`, `huge_brown_mushroom_can_place_on` and `huge_red_mushroom_can_place_on`.
  Not `substrate_overworld`: a spruce, an azalea or sculk would turn the soil into something else.
- Verdant farmland joins every tag vanilla's farmland is named in: `supports_crops`, `grows_crops`,
  `supports_vegetation`, `supports_big_dripleaf` and `support_override_cactus_flower`.

So the soil holds flowers, saplings, grass, bamboo, azaleas and mushrooms, and no crop: barley, a stem or a pitcher pod
wants the farmland.

### Crops

| Piece | What it is |
|---|---|
| **`BarleyBlock`** | `barley_crop`. An `@Actor` over `CropBlock`, which already has the eight ages and the shapes; it only answers `getBaseSeedId` with itself. On normal farmland it follows wheat's light rules, on verdant farmland the verdant rule. |
| **`HopsBlock`** | Four blocks, `asgarnian_hops_crop` and its three siblings, of one `@Actor` over `DoublePlantBlock` with `BonemealableBlock`, modelled on the pitcher crop. `AGE` 0 to 9 and `HALF`; the upper half exists from age 5, carries the same age and shows stage `age - 5`. The lower half stands on `#supports_crops` where the ground is verdant or the block is dark; the upper half stands on its lower half. `updateShape` is guarded while the plant is single, as the pitcher's. Only the lower half ticks, until its last age. It grows when the ground is verdant or the block is dark, and only while the block above is air or its own upper half, at `getGrowthSpeed` and through NeoForge's crop-grow hooks, so other mods' growth rules reach it. Bonemeal adds 2 to 5 ages, as for a crop, and places the upper half when the age it lands on is double. Breaking either half takes both, with one set of drops. A ravager tramples it. |
| **`BulbsBlock`** | `deepslate_bulbs_crop`. An `@Actor` over `CropBlock` with ages 0 to 9. It stands on `#jolcraft:supports_deepslate_bulbs`; on verdant ground it needs nothing more, elsewhere it needs Y 0 or below and darkness. Its random tick is its own, since vanilla's grows only in light: under the same condition it grows at `getGrowthSpeed`, through the crop-grow hooks. No bonemeal, no trampling, an iron pickaxe to harvest. |
| **`Darkness`** | A `@Helper`: `at(level, pos)`, raw brightness 8 or less. In a `WorldGenRegion`, where nothing is lit yet, being under the surface heightmap stands in, as the old block did; this is the only place that knows it. |

Both dark crops follow vanilla's own timing: survival is checked on placement and on a neighbour's change, as vanilla
checks a wheat's light, and a random tick only grows.

### Wild plants and mushrooms

| Piece | What it is |
|---|---|
| **`BloodrootBlock`** | An `@Actor` over `Block`, waterloggable, as `HangingRootsBlock`: it survives under any sturdy face. Bonemeal spreads it (1 in 5) to a free neighbour whose ceiling is in `#jolcraft:bloodroot_spreads_under`, which holds `#supports_vegetation`. |
| **`BloomBlock`** | Skybell and cyanella. An `@Actor` over `VegetationBlock` with a flower's shape and offset, built with the tag it stands on: `#supports_vegetation` for skybell, `#jolcraft:supports_cyanella` (warped nylium, verdant soil) for cyanella. Not a `FlowerBlock`: see "Not in this design". |
| **`DuskcapBlock`** | An `@Actor` over `MushroomBlock` with its taller shape and its huge feature's key. Vanilla's mushroom rules otherwise. |
| **`FesterlingBlock`** | An `@Actor` over `MushroomBlock`. It survives on `#jolcraft:supports_festerling` (verdant soil and `#overrides_mushroom_light_requirement`) or on the top of an upright log, in any light. `grounded(state)` is that one rule, which the crop and the planting share. |
| **`FesterlingCropBlock`** | `festerling_crop`. An `@Actor` over `VegetationBlock` with `BonemealableBlock`, four ages, on festerling ground. Its speed is its own: faster on verdant soil, with verdant soil around it and with upright logs beside it, the old numbers as named constants. At its last age a random tick turns it into the festerling with one burst of particles. Bonemeal succeeds 2 in 5 and adds 1 or 2 ages. It has no item; its pick-block is the festerling. |
| **Pots and huge blocks** | Four `FlowerPotBlock`s through the supplier constructor, added to the empty pot at common setup. Four `HugeMushroomBlock`s. |
| **`LayeredMushroomFeature`** | The base of the two huge mushrooms, over `AbstractHugeMushroomFeature` and vanilla's config, so a mushroom's bonemeal check works untouched. A shape is a record: the stem's height and the cap's rings, each a height, a radius and whether it has corners, with a tip on top. The base places the stem, the rings and the tip, and gives a face the cap texture when no block of the mushroom is beside it, one rule for both. It answers vanilla's clearance question from the shape, so a mushroom grows only where its whole shape is free, and it places through vanilla's `placeMushroomBlock`, into air and `#replaceable_by_mushrooms`. `HugeDuskcapFeature` and `HugeFesterlingFeature` hold their shape and nothing else. `foliage_radius` is written as the whole height less four (4 for the duskcap, 0 for the festerling), which is what the mushroom block's bonemeal check reads it for; the features do not read it. |

### Planting

`FloraData.PLANTS`, the data map `jolcraft:plants` on items, synced: the block an item plants. The mod's one entry is
rotten flesh to the festerling crop.

`Flora` listens to `UseItemOnBlockEvent` at `ITEM_AFTER_BLOCK`. For a stack whose item has an entry it asks what
`BlockItem.place` asks, the same on both sides: the block is enabled; the place context's position (the clicked block
when replaceable, else the neighbour) can take it; the block has a placement state that survives there, unobstructed;
and the player may use the item there, which is the adventure-mode check. When any of it fails the event is left
alone and the game goes on to the item's own use, as it does for a carrot that cannot be planted. When all of it
holds the click is cancelled with `SUCCESS` on both sides, so the hand swings and the client goes on to no eating, and
the server places: NeoForge's entity-place event, so a protection mod can refuse; the block; the block's own place
sound, for everyone, the placer included, since the client predicted none; the game event; the placed-block
criterion; the item's use statistic; one item consumed, none in creative. The client predicts no block: it shows with
the server's update. Where the plant may stand is the block's own `canSurvive`, so the handler knows no log and no
soil.

### Bonemeal

`FloraData.BONEMEAL`, the data map `jolcraft:bonemeal` on blocks, server only, lists merged across packs: what else
bonemeal grows on a block. A row is a placed feature's key and an optional biome tag.

`Flora` listens to `BonemealEvent` on the server, for a valid target only. For each row of the block whose biome tag
holds (or that has none) it places the feature above the block, as grass and nylium place theirs. The chance, the
height and the spread are the placed feature's own placement modifiers, among them the worldgen system's
`height_filter`. The event is left alone, with one exception found in the build: where a sprout took the room the
block's own growth needs, the bonemeal is consumed for the sprout and the event answered as a success (see "Built
2026-11-11 (step 4)"). The rows:

| Block | Feature | Biome tag |
|---|---|---|
| `minecraft:grass_block` | `jolcraft:skybell_bonemeal`: one in four, at Y 101 or above (a grass block at 100 or above, as before), two tries within three blocks where a skybell survives | `#jolcraft:sprouts_skybell` (mountains, hills, forests, taigas, the old `dwarven` tag's biomes) |
| `minecraft:warped_nylium` | `jolcraft:cyanella_bonemeal`: one in four, two tries within three blocks where a cyanella survives | none |

Neither feature carries a `minecraft:biome` filter. Vanilla's own tag `can_spawn_from_bone_meal` was weighed for the
skybell and left: it takes no height, shares its chance with every other flower of the biome, and gives several
flowers a use.

### Content, tags, data and keys

- **`FloraContent`**, `@Content`: one `DeferredRegister.Blocks` (23 blocks), one `DeferredRegister.Items` (23 items),
  one register of the two feature types. `register(modBus)` also registers the two data map types and adds the pots.
  Every block keeps the properties the old factories gave it: strength, sound, map colour, push reaction, offset,
  `replaceable`, lava ignition, the tool requirement, random ticks, and the farmland's view-blocking and suffocating.
- **`FloraTags`**, `@Value`. Blocks: `VERDANT`, `SUPPORTS_DEEPSLATE_BULBS`, `SUPPORTS_CYANELLA`,
  `SUPPORTS_FESTERLING`, `BLOODROOT_SPREADS_UNDER`. Items: `HOPS`, `HOPS_SEEDS`. Biomes: `GROWS_SKYBELL` (mountains
  and hills), `SPROUTS_SKYBELL`. The names follow vanilla's `supports_` and its verb tags.
- **`FloraData`**, `@Value`: the two data map types and their records, `Plants` and `Sprout`.
- **`FloraFeatures`**, `@Value`: the two configured-feature keys the mushroom blocks name. Every other worldgen key is
  the datagen contributor's own.
- **`FloraHints`**, `@Value`: two `LanguageKey`s, the growing hints of the hops seeds and the bulbs. Each is a line of
  the item's default `minecraft:lore` component, with its colour and no italics set on the line itself, since lore
  lends its own purple italics to whatever a line leaves unset: vanilla's component for static tooltip text, drawn
  everywhere a tooltip is, with no client code.

Blocks, by id: `verdant_soil`, `verdant_farmland`, `barley_crop`, `barley_block` (vanilla's `HayBlock`), the four
`<variety>_hops_crop`, `deepslate_bulbs_crop`, `festerling_crop`, `festerling`, `festerling_block`, `festerling_stem`,
`duskcap`, `duskcap_block`, `duskcap_stem`, `skybell`, `cyanella`, `bloodroot`, and the four `potted_` ones.

Items: `barley_seeds`, `barley`, the four `<variety>_seeds`, the four `<variety>_hops`, `deepslate_bulbs` (the bulbs'
seed), and the block items of `bloodroot`, `skybell`, `cyanella`, `duskcap`, `festerling`, the four mushroom blocks,
`verdant_soil`, `verdant_farmland` and `barley_block`.

No client package: no screen, no tint, no renderer, and the render layers are the textures' own.

## Assets

The first hand-made assets of the tree, copied from `old/src/main/resources/assets/jolcraft` unchanged in name: 88
block textures, 12 item textures and the ten Blockbench models of the bulbs' ages under
`models/block/deepslate_bulbs/`. The hops keep their `_crop_bottom_stage` and `_crop_top_stage` file names; the model
contributor maps the one block's halves onto them.

## Datagen

Contributors under `datagen/module/system/flora`, one per kind (`FloraModels`, `FloraLoot`, `FloraTagging`,
`FloraText` and so on), each `@Contributes(Dictionary.FLORA)`:

- **Models**, with vanilla's generators: barley through `createCropBlock`; the festerling crop through the aged
  `createCrossBlock`; the plants with their pots through `createPlantWithDefaultItem`; bloodroot a cross with a flat
  item; soil a cube; the barley block the hay bale's pillar. Three small generators of the contributor's own: the hops
  (a dispatch over age and half onto the crop template), the farmland (the farmland template with the soil's textures,
  one model, no dispatch) and the mushroom blocks (vanilla's twelve-part multipart with the mod's inside textures). The
  bulbs' block state is generated and names the ten hand-made models. Every item gets its model too: flat ones from
  the twelve item textures (barley held like a tool, as before), the plants' from their block textures, the mushroom
  blocks' an inventory cube, the rest their block's model.
- **Loot.** Block tables as the old ones, with vanilla's explosion decay on every crop: barley as wheat; hops a seed,
  and when mature a second seed 1 in 5 and a hops, in the tall flowers' shape (a pool per half that checks the other
  half is there, and one for the single plant), since a double plant rolls the table of the half that was broken and
  the pitcher's own table drops nothing from the upper; bulbs (one, one more with the ore fortune
  bonus and one more 1 in 5 when mature); the festerling crop (a festerling at its last age, the rotten flesh never
  back); mushroom blocks as vanilla's; pots their contents; farmland the soil. The barley seeds of short and tall
  grass are one NeoForge `add_table` modifier over both tables (1 in 20, not with shears) and a one-entry table.
- **Tags**: the ones above; `crops`, `maintains_farmland`, `small_flowers`, `flower_pots`, `replaceable_by_mushrooms`,
  the `mineable` tags, `needs_iron_tool` and `sword_efficient` as before; `c:seeds`, `c:crops`, `c:mushrooms`; the
  item `small_flowers` copied from the block tag; the two biome tags.
- **Data maps**: the two of the mod, and `compostables` as before plus the four omissions (the barley block as the
  hay bale, the mushroom blocks and stems as vanilla's, bloodroot as hanging roots).
- **Recipes**: bread from three barley, cyan dye from cyanella, light blue dye from skybell, nine barley and the
  barley block both ways.
- **Registries**: eight configured features (a single block each of bloodroot, skybell, cyanella, duskcap and the
  bulbs at a random age, the cyanella's nether-forest patch, and the two huge mushrooms under `FloraFeatures`' keys),
  nine placed features (bloodroot and skybell twice each, cyanella, duskcap and the bulbs for the world, and the two
  of bonemeal) and seven biome modifiers, one per world feature. The old patches become `count`, `random_offset` and
  a filter with the old numbers; the cyanella keeps the nether's own vegetation feature and `count_on_every_layer`,
  as the nether's plants are placed; the bloodroot keeps its upward scan and gets `height_filter` at 64 in place of
  the old pair; the skybell `height_filter` at 100 and 115. The huge mushrooms' `can_place_on` is vanilla's
  brown-mushroom tag.
- **English**: every block and item as before, and the two hints.

## Groundwork

Generic pieces whose first consumer is flora, built first:

1. **The mod's creative tab**, in two modules, as `result` and `outcome` hold the mod's one `YieldKinds`:
   - The `tab` primitive. `Tab`, `ModTab` and the `TabKey` handle: `of(id)`, declared while a module is built and
     registered once by the root into the creative-tab registry; `tab.add(key, items...)` appends a module's entries
     in order; the icon is the first entry; the title is the mod's own text, its key starting with the namespace as
     `LanguageKey` has it for a tab, which datagen demands English for. Unit tests of its own and no scene: a tab of
     the game-test mod would show in the dev client.
   - The `catalog` system. `Catalog`, which takes `tab` and declares `CatalogKeys.TAB`, the mod's one tab,
     `jolcraft:jolcraft`; its English is its own datagen contributor's. Every system with items takes `tab` and adds
     to that key, so the order of the tab is the order the root builds the systems in.

   `module-plan.md` had tabs as the content of the system that owns them, which fits no tab that every feature fills.
2. **Model reference check.** `runData` fails when a block state names a model, or a model a parent or a texture,
   that is neither generated in this run, hand-made in `src/main/resources`, nor the game's. With it, a sound
   definition's `.ogg` (W13). Vanilla checks none of this, and a typo is a magenta cube in `runClient`.
3. **Loot tables under any context.** `LootContributor.tables(Loot.Tables)`: `add(key, context, builder)`, one seam
   for every context in place of the one per context `module-plan.md` foresaw, which is corrected there. The provider
   groups what was added into one sub-provider per context. And the check `drop.md` left to the first use: an
   `add_table` or `replace_table` modifier's table is written in this run or is the game's, for which the loot
   provider hands the modifier provider the keys it wrote.
4. **Biome tags.** `TagContributor.biomeTags(Tags<Biome>)` and its provider.

The datagen source set has no tests, so the two checks are proven as `REVIEW_TODO.md`'s W6 records the other walks
were: by hand, once, with a reference and a table that are deliberately wrong, noted in this document when built.

## Lifecycle and rules

- Built: the root builds `Flora` with the other systems, registers `FloraContent` on the mod bus and `Flora` on the
  game bus. No client bootstrap.
- Rule 4: `Flora` takes `tab`, and `log` once it logs. Rule 8: the blocks and features are actors that name the
  folder's content, tags, keys and helper, and no module; the mixins name `GrowthEvent`.
- An actor's statics are properties, codecs, shapes and records, which the architecture test allows; a shape table is
  a record, and per-state shapes are an instance field, as the pitcher crop's.
- `accesstransformer.cfg` gains one line, `CropBlock.getGrowthSpeed` public, for the hops, which are a double plant
  and not a crop. `jolcraft.mixins.json` gains the two mixins.
- Cost: an event post where a crop reads its light or speed and where a mushroom rolls, each with one listener that
  reads one block state.
- `damage`'s `Stage`, `effect`, `hand`, `rework` and `drop` are not used. `worldgen`'s `height_filter` is, in data.

## Proof

Unit tests under the main mod:
- `Sprout` and `Plants` round trip through their codecs and refuse what they must.
- The two mushroom shapes give the counts the old features built: 16, 12, 12, 12, 8 and the tip for the duskcap; 12, 8
  and the tip for the festerling.
- The hops' arithmetic: which ages are double and which stage the upper half shows.

Scenes under `gametest/module/system/flora`, against the mod's own content, which the game-test server loads. No
scene asks the system anything, so the question of how a scene reaches a system does not arise yet. The scenes build a
roofed box where they need darkness and tick a block's `randomTick` themselves.
- **Ground**: wheat on verdant farmland in the dark survives a neighbour's change and grows; on plain farmland it
  breaks. `getGrowthSpeed` on verdant farmland is 1.5 times that on moist farmland. A pitcher crop grows in the dark on
  it. A brown mushroom stands lit on verdant soil. The events answer 0.06 and 0.6 over verdant soil and the game's
  own numbers over dirt, and the mushroom mixin is proven without chance: a listener of the scene's own sets a chance
  to 1 and to 0, and the game's bonemeal check and spread follow it every time. A hoe tills the soil; a block on the
  farmland returns it to soil; a fall on it changes nothing.
- **Crops**: barley grows to its last age and drops barley and seeds. A hop grows through all ten ages in the dark,
  gains its upper half at age 5, grows in the light only on verdant farmland, does not grow under a block, and breaks
  as one with one set of drops from either half. A bulb survives on verdant soil and not on plain deepslate above
  Y 0.
- **Wild plants**: in an `isolated` scene a survival `TestPlayer` plants rotten flesh on an upright log: the crop
  stands, one flesh is gone and the player eats nothing; on a log lying on its side nothing happens. The crop turns
  into a festerling.
  Bloodroot hangs under stone, spreads by bonemeal under dirt alone and keeps its water. An empty pot takes each of
  the four plants. Each huge feature places its shape; bonemeal on the mushroom grows it.
- **Bonemeal**: through a fixture row of the scene pack on rooted dirt, a bonemeal target the mod's own rows do not
  name, with a fixture feature: the feature is placed on a valid target and not on an invalid one.
- **World**: the nine placed features and seven biome modifiers are in the level's registries.

Hand check in `runClient`, the first with art: every plant draws and is cut out, the hints show, the creative tab
lists the items in order.

## Where things go, and the order

Each step ends with the gates in `CLAUDE.md` and its part of the docs.

0. Groundwork, the four pieces above, with their own tests.
1. Ground: the Dictionary words, `FloraTags`, the two verdant blocks, `GrowthEvent`, the two mixins, `Flora` with the
   verdant rule, the access transformer line (the ground scenes read a crop's speed), their textures, models, loot,
   tags and English, the ground scenes. The first run of the model check.
2. Crops: barley and its block, the hops, the bulbs, `Darkness`, their items, hints, loot, the grass seeds, compost,
   recipes and scenes.
3. Wild plants: bloodroot, the two blooms, the two mushrooms with pots and huge blocks, the festerling crop,
   `FloraData.PLANTS` and the planting, the huge features, their scenes.
4. World: the configured and placed features, the biome modifiers and biome tags, `FloraData.BONEMEAL` and its
   listener, the last scenes.

Docs: `CLAUDE.md` mirrored into `AGENTS.md` (the first feature, the `tab` primitive and the `catalog` system, the
datagen seams, the assets); `module-plan.md` (flora built, tabs, the loot seam, a status line); the loot contributor's
own comment; `REVIEW_TODO.md` (W13); this document's built notes.

## Not in this design

Everything that reaches flora's items from another feature comes with that feature, which names flora's content:
- **The flowers' stew effects.** Marksman and alchemist focus belong to the potion feature, which brews from flora's
  plants, so flora cannot name them. That feature writes the two stew recipes, which are data. How a mooshroom learns
  them is decided there; a block data map that `BloomBlock` reads is the candidate.
- **The verdant soil recipe** (mud and verdanite dust): with the feature that owns verdanite. Until then the soil is
  creative only.
- Barley malt, the deepslate plate, the mortar and fermenting recipes, the brewing-stand mixes, the keeper's trades,
  bounties and loadout, the crates, the fortress garden and supply tables, the muffhorn's food, the JEI pages, the
  hops' brew colours.
- The two `potted_` structure pieces and their unused pool ids: the fortress's, if it wants them.
- English for tags: with the recipe viewers.

## Changed on purpose

- **Hops are four blocks, not eight**, under new ids. A world of the 1.21.1 mod would lose its planted hops; nothing
  else of flora changes id. The rewrite is taken to carry no old world.
- **A lit bulb no longer breaks on a random tick**: both dark crops break where vanilla breaks a crop, on a
  neighbour's change.
- **Verdant soil holds what its tags say**: what `supports_vegetation` holds, bamboo, azaleas and mushrooms. It no
  longer holds nether wart, nether fungi and roots, stems, pitcher pods or the small dripleaf, which the old blanket
  override let stand on it; the crops among them want the farmland.
- **Cyanella's bonemeal rule** no longer asks for the warped forest: warped nylium is the condition.
- **Festerling ground is one rule**: the crop may also be planted on what the mushroom stood on (mycelium, podzol,
  nylium). The crop's pick-block is the festerling at every age, where the old gave rotten flesh before the last.
- **Planting** plays the planted block's own place sound, not the crop sound, and counts as the item's use. Clicking
  beside a valid spot plants there, as a seed does; the old asked for the top face.
- **Bonemeal sprouts** need a valid target, and give one or two flowers on a quarter of the uses where the old gave
  exactly one.
- **The huge mushrooms** grow only where their whole shape is free and replace only what vanilla's do, where the old
  ones replaced anything that was not a full opaque block and the festerling never looked at its cap's room. The
  festerling's cap blocks beside the stem show their inside there, by the one face rule.
- **A patch's spread** is vanilla's triangle distribution, as vanilla's own patches became.
- **Compost**: the four omissions above.

## Decided 2026-11-11

Two choices were put to the author, each with a pick; the answer was "yes".
- **The mod keeps a creative tab of its own**, as the old mod had: the `tab` primitive and the `catalog` system of the
  groundwork. Every feature declares its items to the one tab in order, and the icon is the tab's first item until the
  coin exists. The alternative was vanilla's tabs through NeoForge's tab event, with no tab and no primitive.
- **The bulbs stand on deepslate and verdant soil**, as the old tag had it and the game did. The old tooltip also
  named tuff; the hint is written to match the tag. The "yes" followed the tab question, and the bulbs were asked in
  the same message: taken as covering both.

## Settled without the author

- The system is `flora`, one folder, one content class, no client package.
- The verdant rule is a tag and four named numbers, not a data map: there is one kind of verdant ground. A second kind
  with other numbers would make it a data map without touching a plant.
- The mixins stay two, posting events as the module map asks; NeoForge's crop-grow event cannot carry the light, since
  it fires only after the light check.
- Planting and bonemeal are data maps, keyed as NeoForge keys its own (`compostables` by item), and the bonemeal rule
  reuses placed features so that chance, height and spread need no fields of their own.
- The crops keep the mod's `<harvest>_crop` ids; the hops' textures keep their file names.
- The hints are the `lore` component; a hold-key tooltip helper, if the mod wants one later, takes them over.
- The huge mushrooms stay two classes over one base with vanilla's config; a config of the mod's own would break the
  mushroom block's bonemeal check for nothing two shapes need.
- The mod's tab key is a constant of a `catalog` system, as the mod's `YieldKinds` is `outcome`'s, not a key the root
  hands around.

## Challenged 2026-11-11

Two lenses, nineteen findings, all confirmed, one in part. Changed:
- Every item's model is named; without them `runData` stops at vanilla's own check.
- The counts: 23 blocks and 23 items, eight configured features with the cyanella's two.
- The mod's tab key and its English have an owner, the `catalog` system; the draft had the root hand a key to every
  system, which no other key does and which left the title with no contributor.
- The hops' table is the tall flowers' shape. The draft followed the pitcher crop, whose upper half drops nothing.
- The hops' survival rule, the bulbs' own random tick and the bonemeal jump's upper half are written down.
- What verdant soil no longer holds is listed whole (stems, pitcher pods, the small dripleaf), and the claim that
  cyanella could not stand on it is withdrawn: the old blanket override held it.
- Planting says what a failed check does (nothing, as for a carrot), who hears the sound, and counts the item's use.
- The grass's own one-in-eight roll is stated, and the skybell's height is 101 at the feature's origin, which is the
  old 100 at the block.
- The huge mushrooms' clearance and what they replace are stated as changes, and `foliage_radius` is the whole height
  less four.
- The cyanella's worldgen keeps its nether feature and its per-layer count.
- The lore line sets no italics itself. The scene with a player is `isolated`. The bonemeal fixture names rooted dirt.
- The datagen checks' proof is by hand, and the loot seam's change is carried into `module-plan.md`.
- The old properties are carried over by name, and the festerling crop's pick-block is stated.

In part: that the old huge features never checked their caps' room holds for the festerling alone; the duskcap's
radii were constants.

## Built 2026-11-11 (step 0)

The groundwork, on the author's "Yes, build it":
- **`tab`**: `Tab`, `ModTab` and `TabKey` in `module/primitive/tab`, with the Dictionary word. A tab is titled under
  `<namespace>.tab.<path>`. Entries may be added before their tab is declared; registering with entries on a tab
  nobody declared fails. The game hides a category tab with nothing to display, so the mod's tab does not show until a
  feature fills it. `ModTabTest` (ten tests) reads the display order and the icon through the generator, since no
  stack can be made in the test JVM before the game binds components.
- **`catalog`**: `Catalog` and `CatalogKeys` in `module/system/catalog`, the root's wiring and log line,
  `CatalogText` with the title "JolCraft", `Translations.add(TabKey, title)`, and `CatalogKeysTest` (two tests), which
  finds the tab registered in the test JVM.
- **Tables under any context**: `LootContributor.tables` and `Loot.Tables`. The provider holds one sub-provider per
  context the game's `LootContextParamSets` holds as a public constant, since its registry is private, and refuses a
  context that is none of them, a key outside the mod's namespace and a key added twice. `Loot` records every key it
  writes; `LootModifiers`, which runs after it, demands that an `AddTableLootModifier`'s or a `ReplaceTableModifier`'s
  table is among them or in the game's data.
- **Biome tags**: `TagContributor.biomeTags(KeyTags<Biome>)`, `contribute/KeyTags` and `provider/BiomeTags`.
- **`References`**, registered last: block states, item definitions (every `model`, and a special model's `base`),
  models (parent and textures) and the generated `sounds.json`. The data run's resource managers hold the game's own
  models, textures and loot tables, so a reference into `minecraft` is checked like any other.

Proven by hand, with a temporary contributor and temporary files that are gone again:
- A generated model with the texture `jolcraft:item/nonexistent` failed: "1 broken client references: model
  jolcraft:item/proof_a names the missing texture jolcraft:item/nonexistent (textures/item/nonexistent.png) in
  layer0". With `minecraft:item/stick` it passed, its parent `minecraft:item/generated` checked too.
- A hand-made block state and an item definition naming missing models failed, each named with its model; pointed at
  `minecraft:block/stone` and at the generated model, they passed.
- A table added under the block context was written as `loot_table/proof_table.json` with the type
  `minecraft:block`. An `add_table` modifier naming it passed, as did one naming `minecraft:chests/simple_dungeon`.
  One naming `jolcraft:missing_table` failed: "1 missing loot tables of modifiers (neither written in this run nor in
  the game's data): [proof_missing names jolcraft:missing_table]".
- A biome tag of a biome key, a game tag, an optional biome and an optional tag was written under
  `tags/worldgen/biome/` with those four values.
- Not proven: the sound-file check, which needs a sound event of the mod to write a definition for. W13 stays open
  for it.

Departures from the plan above: the tab's title key is the mod's own text, not `itemGroup`; the tab has no scene. A
review of the whole diff after the build changed two things: the modifier provider keeps its tables in one map, and
the reference check reads a special model's `base`.

Verified: 235 unit tests, `runData`, 141 of 141 scenes and `build` green. Steps 1 to 4 are not built.

## Built 2026-11-11 (step 1)

The ground, on the author's "Build step 1":
- **Main**: `FloraTags.VERDANT`; `FloraContent` with `verdant_soil`, `verdant_farmland` and their block items;
  `VerdantSoilBlock` and `VerdantFarmlandBlock`; `GrowthEvent` with its four kinds; `Flora` with the verdant rule and
  its four named numbers; the root's wiring; the Dictionary words farmland, flora, soil and verdant.
- **Mixins**: `CropBlockMixin` (three injectors) and `MushroomBlockMixin` (two), in `jolcraft.mixins.json`. All five
  apply under `defaultRequire: 1`. `Spread` and `Bonemeal` carry the game's own number as `VANILLA`, and a chance left
  at it returns the game's roll or constant untouched.
- **Access transformer**: `CropBlock.getGrowthSpeed` public, brought forward from step 2 since a ground scene reads it.
- **Assets**: `verdant_soil.png` and `verdant_farmland.png`, the tree's first hand-made files.
- **Datagen**: `FloraModels` (a cube; the farmland template with the soil as its dirt, one model), `FloraLoot`,
  `FloraTagging` (the tags as designed) and `FloraText`.
- **Tests**: `GrowthEventTest` (six) and `FloraContentTest` (three); nine scenes in `GroundScenes` and one in
  `CatalogScenes`, which reads the mod's tab as the game builds it for the level.

Departures from the plan above:
- `Flora` takes `tab` alone; `log` joins with the planting.
- MixinExtras' injectors take no `@Desc` method target. The builder stopped on it as told, and the host methods are
  named by their bare names, the calls inside by `@At(desc = @Desc(...))`.
- The mushroom scenes use a listener that forces a chance to 1 or 0 where the plan had a seeded thousand rolls.

The first real run of the reference check: "6 client files read, 9 references found, every model, parent, texture and
sound file exists".

Verified: 244 unit tests, `runData`, 151 of 151 scenes and `build` green. Not yet seen in `runClient`: the two
textures, the sparkle and the tab. Steps 2 to 4 are not built.

## Built 2026-11-11 (step 2)

The crops, on the author's "Build step 2":
- **Main**: `BarleyBlock`, `HopsBlock`, `BulbsBlock` and the `Darkness` helper; `HopsVariety`, an enum of the four
  varieties with their word and map colour, since a content class holds registers and holders only; `FloraHints`;
  the tags `SUPPORTS_DEEPSLATE_BULBS`, `HOPS` and `HOPS_SEEDS`; in `FloraContent` the barley crop and block, the four
  hops crops, the bulbs, and their twelve items, the seeds as block items of their crops with the item's own name
  and the hint as their default lore; `Flora` adds fourteen items to the tab in the old order; 19 Dictionary words.
- **Assets**: 71 block textures, 11 item textures and the ten models of the bulbs, copied unchanged.
- **Datagen**: the models (barley through the game's crop generator, the hops a dispatch over age and half, the
  bulbs' block state over the hand-made models), the loot (the hops in the tall flowers' shape; the grass seeds as one
  `add_table` modifier over a table written through `tables`, the first real use of the modifier-table check), the
  tags, `FloraDataMaps` (compost) and `FloraRecipes` (bread, the barley block both ways), the English and the hints.
  `Loot.Blocks` opens `createCropDrops` and `applyExplosionDecay`.
- **Tests**: `HopsBlockTest`, `BulbsBlockTest`, `FloraHintsTest` and more of `FloraContentTest`, twelve tests;
  sixteen scenes in `CropScenes`, `CatalogScenes` over the fourteen items, and `FloraArena` for what the flora
  scenes share.

What the build settled:
- The arena's plant row is at Y -55, so the scenes show the bulbs' deep half of the rule there, and one scene builds
  its two boxes at Y 10 above its own arena for the other half and takes them away again.
- The grass's own table asks for `minecraft:shears` by item, and the modifier asks the same, inverted.
- The hint keys are `tooltip.jolcraft.hops_seeds` and `tooltip.jolcraft.deepslate_bulbs`.

A fault found after the build: the builder's run of the scenes passed, the next crashed the server tick. Two scenes
registered a wait from inside another wait's action, which changes the game's schedule of a test while the game walks
it. `FloraArena.inOrder` now runs a scene's waits one after the other from one registration made in the scene's body,
and the game-test server passed four runs in a row after it. The same hazard in the adapter's own `runAfter` and
`runAtTick` is W24 in `REVIEW_TODO.md`.

The reference check: "116 client files read, 307 references found, every model, parent, texture and sound file
exists".

Verified: 256 unit tests, `runData`, 168 of 168 scenes four times over, and `build` green. Not yet seen in
`runClient`: any texture or model of flora, the bulbs' hand-made models among them, the hints and the tab. Steps 3
and 4 are not built.

## Built 2026-11-11 (step 3)

The wild plants, on the author's "Build step 3":
- **Main**: `BloodrootBlock` over the game's `HangingRootsBlock`; `BloomBlock` for skybell and cyanella;
  `DuskcapBlock`, `FesterlingBlock` and `FesterlingCropBlock`; four pots through the supplier constructor, added to
  the empty pot at common setup; four huge blocks; `MushroomShape`, `LayeredMushroomFeature` and the two features,
  registered as feature types; `FloraFeatures`, `FloraData.PLANTS`, three more tags; `Flora` takes `log` and plants;
  23 items in the tab; 16 Dictionary words.
- **Assets**: 15 block textures and one item texture; the folders hold the old mod's 88 and 12.
- **Datagen**: the models (the mushroom blocks' twelve parts with the mod's inside textures), loot, tags with the
  item `small_flowers` copied, compost, the two dyes, the `plants` row, the English, and `FloraRegistries` with the
  two configured features, their `foliage_radius` read off the shapes.
- **Tests**: `MushroomShapeTest`, `FesterlingCropBlockTest`, `FloraDataTest` and more of `FloraContentTest`,
  eighteen tests; 28 scenes in `WildScenes`, `MushroomScenes`, `FesterlingScenes` and `WildDataScenes`, and
  `CatalogScenes` over the 23 items.

What the build settled:
- **The use-on-block flow.** On both sides `ItemStack.useOn` posts the event at `ITEM_AFTER_BLOCK` first, and only
  then runs what a cancel therefore skips: the adventure-mode check, on the server the block snapshots with the
  entity-place event, and the item's use statistic. `Flora` asks the adventure check with the others, and on the
  server takes a snapshot, places, posts the entity-place event, and on a refusal restores the snapshot and answers
  `FAIL`. The used-on-block criterion is the game mode's own and still fires.
- **The festerling crop's model** is written by the contributor: the game's aged cross generator also writes an item
  model for the block's item, which for a crop without one is air.
- **`HangingRootsBlock` serves as the bloodroot's base**; its codec, like the mushroom's, is typed to its own class,
  so the three subclasses keep theirs as the base's type.
- A scene with a listener that cancels the entity-place event proves the refusal: `FAIL`, no crop, the flesh kept,
  no use counted, nothing eaten.

Changed after the build, on a read of the diff:
- A bloodroot spread into flowing water was waterlogged and so made a source; it is waterlogged in source water
  alone, as the game's roots are placed.
- The older flora scenes are `isolated` too. The builder found the arenas of one batch standing six blocks apart,
  closer than the scenes build: its blocks stood in an older scene's dark box. That is W8.

Not changed, and still as planned: nothing proves the client half of planting but a hand check.

The reference check: "167 client files read, 433 references found, every model, parent, texture and sound file
exists".

Verified: 274 unit tests, `runData`, 196 of 196 scenes three times over, and `build` green. Not yet seen in
`runClient`: any of flora. Step 4 is not built.

## Built 2026-11-11 (step 4)

The world, on the author's "Build step 4", which closes the feature:
- **Main**: `FloraData.BONEMEAL`, an `AdvancedDataMapType` on blocks with NeoForge's list merger, and its `Sprout`
  row, a placed feature's key and an optional biome tag; the biome tags `GROWS_SKYBELL` and `SPROUTS_SKYBELL`; the
  listener for `BonemealEvent` in `Flora`; seven Dictionary words.
- **Datagen**: in `FloraRegistries` six more configured features, nine placed features and seven biome modifiers,
  `add_` and the placed feature's id; the two biome tags, the first use of the biome tag seam; the two rows of the
  data map. `runData` wrote 8, 9 and 7 files for the registries, the two tags and `data_maps/block/bonemeal.json`.
- **Tests**: six more in `FloraDataTest`; thirteen scenes in `WorldScenes`, with fixtures in the scene pack: three
  marker features, two biome tags and rows on rooted dirt, one of them naming a feature the level lacks.

What the build settled:
- **The level's biome** is plains everywhere, a flat world; the fixtures' tags are built on it.
- **The event** is posted at the top of `BoneMealItem.applyBonemeal`, on both sides, before the game asks the block
  whether it is a target; its own answer to that is fixed when it is built.
- **The data map** is written as `values: {block: [rows]}`, and the loader reads every pack's file of the name as a
  stack, so the scene pack's file joins the mod's.

A fault in this design, found by the builder: the listener places its feature before the game asks the block again
whether it is a target, and a sprout that lands in the cell above a grass block or a nylium makes the block no target.
The game then refuses the bonemeal and consumes nothing, and the sprout grew for free: the fault this step was to
close, in about one use in fifty. `Flora` now asks the block after placing, and where it is no longer a target it
consumes the bonemeal itself and answers the event as a success. The scene with real bonemeal asserts that every one
of 150 uses is accepted and consumes exactly one.

Two faults in the scenes, found when the gates were run again: one run failed two scenes that had passed many times.
- A wait gave up after 60 ticks. The game-test server ticks as fast as it can while light settles on another thread,
  so on a slow run the box at Y 10 was still lit. A wait is now given a thousand ticks.
- Opening a dark box told the hop in it of its neighbours' changes while the light was already coming in on the other
  thread, and the hop was judged lit and gone before the scene looked. The box is opened without shape updates, and
  that scene stops the level's random ticks, since farmland that dries tells the plant above it of a change too.
Both are W25 in `REVIEW_TODO.md`. The game-test server passed eight runs in a row after them.

Verified: 280 unit tests, `runData` (37 translations; "167 client files read, 433 references found"), 209 of 209
scenes eight times over, and `build` green.

Left for a hand check in `runClient`, since nothing of flora has been seen in a client: the 100 textures and the
models, the bulbs' ten hand-made ones among them; the cut-out layers; the sparkle of the verdant blocks; the hints
on the seeds; the creative tab; the huge mushrooms; and the client half of planting, that the hand swings and a
hungry player does not start to eat.

## Hand check 2026-11-12

The author ran the dev client for about three minutes in a new world, with the list of checks this document ends its
step 4 note with, and said: "Looks good". The client's log of that run names no model, texture or block state of the
mod as missing or unreadable. Which of the checks were made is not recorded; the client half of planting is the one
only a hand check can show.
