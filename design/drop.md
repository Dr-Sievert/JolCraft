# Drop: a loot modifier that replaces what a table rolled

Draft of 2026-11-11, from a read of the old tree's two loot modifiers and their four uses, against vanilla 26.1.2 with
NeoForge 26.1.2.122's patches. The generic support for clearing a drop before adding to it, which NeoForge's own
`add_table` cannot do: the `drop` system, which declares one loot modifier kind through the loot primitive, in one
build step. Challenged 2026-11-11 by two lenses (the rules, and 26.1.2 feasibility with fidelity to the old tree): eleven
of twelve findings confirmed and folded in, see "Challenged". The one open choice is decided, see "Decided 2026-11-11".

## What the old tree did

`world/loot/custom/AddItemModifier` and `AddLootTableModifier`, NeoForge loot modifiers registered in
`world/loot/JolCraftLootModifiers` and written by `datagen/loot/glm/JolCraftGlobalLootModifierProvider` (`addItem`,
`replaceWithItem`, `addLootTable`, `replaceWithLootTable`) from four sub-providers. Their fields:
- NeoForge's `conditions`.
- `item` (one of it), or `loot_table`, the table rolled into the drop.
- `chance`, clamped to 0..1 and rolled on the context's random.
- `replace`, optional, false: when true the drop was cleared before the addition.

The four uses:
- Barley seeds added to short and tall grass (5%), unless broken with shears.
- The lexicon added to the stronghold library (50%) and the abandoned mineshaft (20%) chests.
- The mod's fishing table replacing 15% of what fishing gives.
- The lexicon replacing 8% of the rare trail-ruins finds.

Its faults:
- The table form rolled its table through `getRandomItems(context, consumer)`, which NeoForge patches to apply the
  modifiers, under a queried table id that was already the target's. So the fishing replace could fire again inside its
  own roll, at every level its 15% held, and every other modifier on fishing ran inside it as well as outside.
- `chance` repeated a condition vanilla has.

## What 26.1.2 gives

- NeoForge's `LootModifier` base reads `conditions`, all of which must hold or the drop passes untouched, and
  `priority`, optional with `IGlobalLootModifier.DEFAULT_PRIORITY`, 1000, through `codecStart`, which fixes that default.
- `LootModifierManager` reads every file under `data/<namespace>/loot_modifiers/`, subfolders included; the old list
  file is gone. It decodes through NeoForge's conditional ops over the reload's registry lookup, and a file that fails is
  logged and skipped. It applies the modifiers highest priority first, sorting by the negated priority. Equal priorities
  keep the order of a hash map of the file ids, which no data pack controls.
- `CommonHooks.modifyLoot` runs them on every roll through `LootTable.getRandomItems` or `fill`, after the table rolled
  itself. It sets the context's queried table id once per roll, which `neoforge:loot_table_id` tests. It catches an
  `Exception` from a modifier, logs it naming the modifier and the table, and goes on with the list as that modifier
  left it. A nested table (a `loot_table` entry) is rolled raw, so the modifiers see only the table the game rolls. The
  fishing hook rolls `minecraft:gameplay/fishing`, whose fish, junk and treasure are nested.
- A brushed block rolls its archaeology table and keeps the first stack, warning when there are more. The game's
  archaeology tables always roll one stack, so an addition to one never shows up: only a replace changes what a brush
  finds, and the replacement must roll one stack itself.
- `neoforge:add_table` (`AddTableLootModifier`) reads `conditions`, `priority` and `table`, a loot table key. When it
  applies it looks the table up through the context's resolver and adds nothing for a missing one. It rolls the table
  raw (`getRandomItemsRaw`, deprecated so that callers apply modifiers) through `LootTable.createStackSplitter`, which
  splits oversized stacks and drops disabled items. So the subtable's own modifiers do not run, and the modifiers after
  it can still change what it added. NeoForge ships no item form and nothing that clears.
- Vanilla's `random_chance` condition (a number provider) does what the old `chance` did, on the same random.
- The loot primitive's `LootKey.modifier(id, codec)`: a kind declared while its module is built, registered once by the
  root into NeoForge's serializer registry; the kind's class answers `codec()` with the key's. The dev mod's `add_star`
  is the only kind so far.
- Datagen: `LootContributor.modifiers(LootModifiers)`. NeoForge's provider writes each modifier to
  `data/<mod>/loot_modifiers/<name>.json` through the conditional codec, and ours refuses a name used twice.

## The system

| Piece | What it is |
|---|---|
| **`drop`.** | `module/system/drop`. `Drop`, `@ModSystem(Dictionary.DROP)`, takes `loot`. Its constructor declares `DropKeys.REPLACE_TABLE`, as `Hand`'s declares its recipe kind. It holds no behaviour and no listener: the game decodes the modifier and calls it. |
| **`DropKeys`.** | `@Keys`. `REPLACE_TABLE`, `LootKey.modifier(Ids.of(Dictionary.JOLCRAFT, Dictionary.REPLACE, Dictionary.TABLE), ReplaceTableModifier.CODEC)`: the kind `jolcraft:replace_table`, beside NeoForge's `add_table`. |
| **`ReplaceTableModifier`.** | An `@Actor`: a final class extending NeoForge's `LootModifier`. Its one static is `CODEC`. Two constructors: one with the conditions and the table, at NeoForge's default priority, and one that also takes the priority, so a contributor sets the order in one argument. `codec()` answers `DropKeys.REPLACE_TABLE.codec()`, as the loot primitive's contract asks; the key set reads `CODEC` and the actor reads the key only when called, so nothing initialises in a cycle. It names no module. |
| **Data.** | `conditions`, as NeoForge's. `priority`, optional, NeoForge's default 1000, as every modifier has it. `table`, a loot table key, as `add_table` names it. `CODEC` is `codecStart` and `table`, as `add_table`'s. |
| **Applying.** | When the conditions hold, the table is looked up through the context's resolver, as `add_table` looks its own up. A missing table leaves the drop as rolled: nothing is cleared and nothing added. Otherwise the table is rolled raw into a list of its own, through the game's stack splitter, with the same context: its random, its params and the target's queried id. Only then is the drop cleared and given that list, so a roll that throws leaves the drop as it was, and NeoForge logs it naming the modifier. The modifiers after it see the replacement as the drop. Nothing checks at runtime that the table reads only what the target's context holds: its feature writes it under the target's context, the fishing table under the fishing one. |

The fishing use, as its feature's data would write it:

```json
{
  "type": "jolcraft:replace_table",
  "conditions": [
    { "condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:gameplay/fishing" },
    { "condition": "minecraft:random_chance", "chance": 0.15 }
  ],
  "table": "jolcraft:gameplay/fishing"
}
```

The archaeology use is the same with a one-entry table holding the lexicon. The two adds are NeoForge's `add_table`,
the item form through a one-entry table too, and the seeds' shears condition is vanilla's `inverted` over `match_tool`.

## Datagen

Nothing new. A feature's contributor builds a `ReplaceTableModifier` and adds it through
`LootContributor.modifiers(LootModifiers)` as it is; the provider encodes it through the kind's registered codec.

## Lifecycle and rules

Built: the root builds `Drop` with the other systems, before it registers `loot`. Load: a file of the kind decodes; one
without `conditions` or `table` is a decode error, logged and skipped by NeoForge's listener. Apply: on the server, in
the game's roll. There is no client half.

- No content class, since the loot primitive registers the kind. No mixin, no access transformer line, nothing in
  `src/main/resources`.
- Rule 4: `Drop` takes `loot`, the one primitive its folder depends on; `LootKey`, which the key set and the actor
  name, is that primitive's handle.
- Rule 8: the actor reaches no module; it names the key set's handle, as the dev mod's `add_star` names its own.
- The game-test mod declares nothing new: its fixtures are files of the main mod's kind, of NeoForge's `add_table` and
  of its own `add_star`.
- Cost: one lookup and one roll per firing, the conditions per roll as for any modifier.

## Proof

Unit tests under the main mod, `ReplaceTableModifierTest`, through `ReplaceTableModifier.CODEC` under registry-aware
JSON ops, as `HandRecipeTest` reads its recipes and NeoForge reads a file:
- A modifier with two conditions, a priority and a table round trips.
- One without a priority reads NeoForge's default, and is written back without the field; the shorter constructor
  gives the same.
- One without `table`, or without `conditions`, is refused.

Scenes on the game-test server under `gametest/module/system/drop`, rolling fixture tables through the dev mod's
`loot.roll`, which applies every loaded modifier. The fixtures, in the scene pack under `scene/data/jolcraft_gametest/`,
touch no table of the game, so no other scene sees them:
- Tables under `loot_table/drop/`: `target` and `kept`, an apple each; `replacement`, 100 sticks, more than one stack;
  `addition`, a diamond.
- Modifiers under `loot_modifiers/drop/`, each tied to its table through `neoforge:loot_table_id`, as the `add_star`
  files are: a replace of `target` by `replacement` with a priority above the default; NeoForge's `add_table` of
  `addition` to `target` with none; the dev mod's `add_star` on `replacement`; a replace of `kept` by a table the data pack lacks.

The checks:
- The kind is registered, as `LootScenes` checks its own.
- Rolling `target` gives no apple, so the table's own roll was cleared. It gives the sticks as 64 and 36, rolled through
  the splitter. It gives the diamond: the add, at the default, ran after the replace, as the stated priority orders them, and
  stayed. It gives no
  star: the replacement was rolled raw.
- Rolling `replacement` itself gives the star, so its absence above means something.
- Rolling `kept` gives its apple alone: a replace whose table is missing leaves the drop alone.

A replace that rolled its table with the modifiers under the target's queried id would meet itself again, its
conditions still holding, and overflow the stack, which `modifyLoot` does not catch: the scene fails as a crash. One that
rolled them under a fresh context would give the star.

## Where things go, and the order

One step, ending with the gates in `CLAUDE.md`, in this order:
1. The Dictionary words drop and replace; `Drop`, `DropKeys` and `ReplaceTableModifier` in `module/system/drop`; root
   wiring.
2. `ReplaceTableModifierTest`, the fixtures and the scenes.

Docs:
- `CLAUDE.md`, mirrored into `AGENTS.md`: the first paragraph's list and count of systems, a `module/system/drop`
  entry in the `module/system/` bullet, and loot tables in the source-sets table's list of what the scene pack holds,
  with the rework recipes that list already misses.
- `module-plan.md`: a `drop` row in the shared-systems table, the sentence above it on what is built, the `loot` row's
  note that the old two modifiers are NeoForge's `add_table` and this kind, and a status line.
- This document's built note.

## Not in this design

- The data of the four old uses: the seeds from grass, the lexicon in two chests and in the trail ruins, the mod's
  fishing table. All are feature data. Their tables come with them, through loot sub-providers on the contexts they are
  rolled under, as the chest sub-provider comes with the strongbox.
- An item field or an inline table: a one-entry table is the item form, for a replace as for NeoForge's add, and can
  carry functions and conditions of its own.
- Clearing part of the drop, by an item predicate, or clearing without a roll: no use needs either.
- A datagen check that a modifier's table is written or is the game's: with the first use, which writes both.
- The old `chance`: vanilla's `random_chance`.

## Decided 2026-11-11

The author: "Just set them to default for now as the default and allow an easy way to edit it."
- The default priority is NeoForge's, 1000, as `add_table`'s, so the kind's codec is `codecStart` and `table`.
- The order is the data's. A file states `priority`, and a contributor passes it to the longer constructor. A replace
  that should run before the adds on its table, so that what they add survives it, states a higher one.
- Two modifiers at one priority run in the hash order of their file ids, as NeoForge orders any.

## Settled without the author

- The system is `drop`, the kind `replace_table` beside NeoForge's `add_table`, and its field `table` as `add_table`
  names it.
- The kind is declared through the loot primitive's `LootKey.modifier`, which exists for loot kinds, not registered by a
  content class: a content class registers what no primitive does, as the worldgen kinds and the rework serializer.
- A missing table leaves the drop alone rather than clearing it, as `add_table` adds nothing for one.
- The table is rolled raw through the splitter, as `add_table` rolls its own: a roll that applied the modifiers would
  meet the replace again under the same queried id, as the old tree's did.
- The table is rolled into a list of its own before the drop is cleared, so a roll that throws loses nothing.

## Challenged 2026-11-11

Two lenses, twelve findings, eleven confirmed. Changed:
- The codec is named: a default of the kind's own would need its own group of fields, since `codecStart` fixes
  NeoForge's; with NeoForge's default, which the author chose, it is `codecStart`.
- The table is rolled into a list of its own before the drop is cleared. NeoForge catches a modifier's exception and goes
  on with the list as the modifier left it, so clearing first would lose the drop to a roll that throws.
- The open choice said that two replaces at one priority tie under either option.
- The scenes keep the star, which a roll under a fresh context would give, and say that a roll with the modifiers
  under the target's id fails as a crash, not with a second diamond.
- The unit tests read the kind's own codec, as `HandRecipeTest` does, and the registration is checked in a scene, as
  `LootScenes` checks its own; no unit test yet relies on a declared loot kind being registered in the test JVM.
- The fixtures tie each modifier to its table through `neoforge:loot_table_id`.
- The old tree's faults are stated, among them its table form rolling with the modifiers, and the seeds' data is
  named beside the other uses.
- What a brush keeps is stated exactly: the first stack, so a replacement for an archaeology table rolls one.
- The docs list names the source-sets table's list of the scene pack's contents.
- Rule 4 is stated over the primitive, not its handle.

Refuted: that `IGlobalLootModifier.LOOT_CONDITIONS_CODEC` is out of a hand-written codec's reach. It is a field of a
public interface, so it is public.

## Built 2026-11-11

Built in the one step as planned:
- `module/system/drop` with `Drop`, `DropKeys` and `ReplaceTableModifier`, the Dictionary words drop and replace, and the
  root building `Drop` before it registers `loot`.
- `ReplaceTableModifierTest` (four tests): the kind's id, the round trip, the default priority and both constructors,
  and the refusals.
- Four fixture tables and four fixture modifiers in the scene pack, and two scenes in `DropScenes`.

Verified: 222 unit tests, `runData`, 141 of 141 scenes and `build` green. The code departs from the plan in nothing it
does.
