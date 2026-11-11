# Rework: crafting that carries a base item's components

Draft of 2026-11-11, from a read of the old tree's component-keeping shapeless recipe and the five recipe sets that
used it, against vanilla 26.1.2 with NeoForge 26.1.2.122's patches. The generic support for a crafting-table recipe
kind that features fill with data, with no mixin: the `rework` system, whose content class registers the kind as the
worldgen kinds are registered, and the datagen seam for it, in one build step. Challenged 2026-11-11 by two lenses
(the rules, and 26.1.2 feasibility with fidelity to the old tree): all nine findings confirmed and folded in, see
"Challenged". Built on 2026-11-11, see "Built 2026-11-11".

## What the old tree did

`world/recipe/custom/vanilla/ComponentPreservingShapelessRecipe` (596 lines), a `CraftingRecipe` with a serializer of
its own, written by `ComponentPreservingShapelessRecipeBuilder` and `DyeColorRecipeBuilder`. Its fields:
- `base`, an ingredient: the stack the result is made from. `ingredients`, the others, 0 to 8. `result`, a strict
  stack. `group` and `category`.
- `keep` (copy only these components), `remove` (copy all but these) and `remove_all` (copy nothing but `keep`).
- `base_requirements` (components the base must have) and `set` (a patch applied last, with no removals).
- `suppress_remainder` (ingredients whose crafting remainder is not given back, tested against every stack).

It matched when the grid held exactly one stack more than `ingredients`, exactly one of them matching `base`, and
NeoForge's `RecipeMatcher` assigned the rest to the ingredients.

Five sets used every field:
- Dyeing, 16 recipes for each of the two compass items: the compass and one dye make the same compass, every
  component copied, `set` giving the dye's exact colour.
- Undyeing, one per compass item: the compass, which must have a colour (`base_requirements`), and a water bucket make
  the same compass with the colour removed. The bucket comes back as its remainder.
- Taking the dial out: the compass alone makes the empty compass, keeping only the colour.
- Resetting a dial: a configured dial (NeoForge's component ingredient) alone makes the plain dial, its structure
  group and colour removed.
- Yeast cultures, five steps: a water bottle, or a culture of one brewing speed, and one ingredient make a culture of
  the next speed. Nothing is copied, the result's stack carries the speed, and the base's remainder (a culture's glass
  bottle) is held back, the one use `suppress_remainder` had.

Its faults:
- `keep` and `remove` copied from the base's whole component map, its item's own defaults included (model, name,
  stack size). Onto another item, that would have made the result look and stack like the base. Only the fact that
  every `remove` use kept the same item hid it.
- A craft that changed nothing still matched: a red compass and a red dye made the same red compass and ate the dye.
  Undyeing needed `base_requirements` to avoid the same thing.
- `set` was needed because the copied components went on over the result's own, so the result's stack could not
  carry a component that won.
- An ingredient equal to the base was refused at load, and one that only overlapped it made the grid never match,
  since exactly one stack had to match the base.

## What 26.1.2 gives

- `CraftingRecipe` answers `RecipeType.CRAFTING` itself, so a kind under it is only a serializer. `RecipeSerializer`
  is a record of a map codec and a stream codec; the stream codec is deprecated and used when a mod asks NeoForge to
  send recipes. It is registered in `RECIPE_SERIALIZER`, like a structure processor's type in its registry.
- The recipe primitive's `RecipeKey` is for the mod's own types: it registers a type, a serializer and a recipe-book
  category (`buildType()` is always `RecipeType.simple(id)`), and `Recipe.Custom` is special and not placeable.
- `NormalCraftingRecipe` is the base of vanilla's data-driven crafting kinds (shapeless, transmute, dye). It holds
  `Recipe.CommonInfo` (`show_notification`, default true) and `CraftingRecipe.CraftingBookInfo` (`category`, default
  misc; `group`, default empty) with their codecs, and caches its `placementInfo`.
- `ShapelessRecipe.matches` checks the count of non-empty stacks. For ingredients that are not simple it then calls
  NeoForge's `RecipeMatcher.findMatches(stacks, ingredients)`, which answers one assignment of each stack to an
  ingredient, counting the non-empty stacks only, or none.
- `ItemStackTemplate.apply(patch)` makes the item with the given patch, applies the template's own components over it,
  and validates: a refused stack comes back empty, with a warning. `TransmuteRecipe.createWithOriginalComponents` is
  that call with the input's whole patch. `ItemStack.validateStrict` refuses little: a damageable item that stacks
  above one, a count above the stack size, and container or bundle contents above their sizes.
- `DataComponentPatch.forget(predicate)` drops entries by type, and keeps a removal (`!type`) whose type passes; a
  carried removal strips the result item's own default of that type. `DataComponentType.PERSISTENT_CODEC` names a
  type and refuses a transient one.
- Vanilla's own kinds come close and stop short:
  - `crafting_transmute` takes an input and 1 to 8 of one material, and carries the input's whole patch, removals
    included. It refuses a craft whose single result equals its input.
  - `crafting_dye` (`DyeRecipe`, which replaced the special armour dye) dyes a target, mixing the colour as leather's
    is mixed, and carries the rest.
  - Neither matches a lone item, keeps or drops chosen components, or holds a remainder back.
- `CraftingRecipe.getRemainingItems` defaults to each stack's crafting remainder. `ResultSlot.onTake` asks the level
  again for the grid's crafting recipe, without the hint the menu used, and puts each remainder back in the slot it
  came from, in the input's order. The crafter gives `assemble` and `getRemainingItems` the same trimmed input and
  dispenses the remainders.
- A recipe that is not special is in the recipe book:
  - The book shows its `display()`, and taking a result awards it to the player's book.
  - Under the limited-crafting game rule, a player who has not unlocked it gets no result.
  - `RecipeManager` warns of one with an empty ingredient (an empty tag), since it cannot be placed, and leaves it
    out of its property sets. The recipe stays, and an empty ingredient matches nothing.
  - A special recipe skips all of this.
- Datagen: vanilla's builders write the unlock advancement through `RecipeUnlockAdvancementBuilder`, under
  `recipes/<category>/`, and refuse a recipe with no criteria ("No way of obtaining recipe"). Our `Recipes.Provider.add`
  writes the mod's own kinds with no advancement, and checks only a `Recipe.Custom`, through `key.check`, which reads
  the key's codec.
- NeoForge's ingredient types, among them `neoforge:components` (an item with given component values, strict or
  partial), compound, difference and intersection.

## The system

| Piece | What it is |
|---|---|
| **`rework`.** | `module/system/rework`. `Rework`, `@ModSystem(Dictionary.REWORK)`, takes nothing and holds no behaviour, as `Worldgen` does: the game decodes the recipe and calls it from the crafting table and the crafter. |
| **`ReworkContent`.** | `@Content`. `SERIALIZERS`, a `DeferredRegister` of `RECIPE_SERIALIZER` under the mod's namespace, and `REWORK`, the holder of the kind's serializer under `rework`: a `RecipeSerializer` over `ReworkRecipe.CODEC` and `ByteBufCodecs.fromCodecWithRegistries(ReworkRecipe.CODEC.codec())`. `register(modBus)`, which the root calls once. |
| **`ReworkRecipe`.** | An `@Actor`: a final class extending `NormalCraftingRecipe`, since a record cannot extend the base. Its one static final is `CODEC`, which the actor rule allows. `getSerializer()` answers `ReworkContent.REWORK.get()`, as a worldgen kind answers its type. It names no module and no handle. |
| **Data.** | Vanilla's `show_notification`, `category` and `group`, through vanilla's codecs. `base`: an ingredient, the stack the result is made from. `ingredients`: the others, optional, at most the grid's size less one, read lazily as vanilla's shapeless codec reads it. `result`: an `ItemStackTemplate`, with item, count and components. `keep`: an optional list of persistent component types, the only ones carried over; an empty list carries nothing. `remove`: an optional list; everything but these is carried over. It is refused together with `keep`. `base_remainder`: default true; false holds back the base's crafting remainder. |
| **Matching.** | The grid holds exactly one stack more than `ingredients`. `RecipeMatcher` assigns the stacks to the base and the ingredients together. The assignment is the same for the same grid, so matching, assembling and the remainders agree on the base. Its indices count the non-empty stacks and are mapped back to the input's slots. When an ingredient also matches the base, the grid's order decides which stack is the base. The result is then assembled. An empty result is no match. A single result of the base's own item with the base's own components is no match either, vanilla transmute's rule: a craft that changes nothing takes nothing. That rule replaces the old `base_requirements`, and stops a dye being eaten for the colour a compass already has. |
| **Assembling.** | The base's patch carries what that stack changed against its item: the values it set and the defaults it removed. `keep` or `remove` filters it by type, removals included. A carried removal strips the result item's own default of that type, as vanilla's transmute does. The result is made as `ItemStackTemplate.apply` makes it, so the template's own components win over the carried ones, which replaces the old `set`. It is checked with `ItemStack.validateStrict`, without the template's warning, since the carry comes from a player's stack and `matches` assembles on every change of the grid. A refused result is no result. Since the game refuses little, a recipe whose result is another kind of item names what it keeps. |
| **Remainders.** | Each stack's crafting remainder, in its own slot, as the game's default gives them. The base's slot is left empty when `base_remainder` is false. The result slot asks the level again without the menu's hint, so two recipes matching one grid are a data error, as they are for vanilla's own kinds. |
| **Recipe book.** | Not special. `placementInfo` is built from the base and the ingredients, and `display()` is a `ShapelessCraftingRecipeDisplay` of them, the result's template and the crafting table. So the book shows the recipe once it is unlocked, crafting awards it, and the limited-crafting rule holds it back, as with any shapeless recipe. |

## Datagen

| Piece | What it is |
|---|---|
| **The check.** | `RecipeKey.check(registries, recipe)` gets a static form, `check(registries, codec, recipe)`, which the key's own delegates to, so the round trip has one implementation and `RecipeCheckTest` keeps its call. `Recipes.Provider.add(id, recipe)` checks every recipe through its own serializer's codec with it, through the unchecked cast its `Recipe.Custom` branch makes today. |
| **The unlock.** | A recipe that is not special needs one. `add(id, recipe, category, unlock)` writes the advancement as vanilla's builders write it, through `RecipeUnlockAdvancementBuilder`, and `add(id, recipe)` refuses such a recipe, naming the overload. |

## Lifecycle and rules

Built: the root builds `Rework`, as it builds `Worldgen`, and registers `ReworkContent` on the mod bus. Load: the kind's
codec reads a rework recipe. `keep` with `remove`, too many ingredients, a transient component type, or a result the
template's codec refuses is a `DataResult` error, so the recipe is dropped with the game's own message. Crafting
belongs to the server: the client's menu shows the result the server sends, and its recipe book the entries the
server lists, so there is no client half.

- One content class. No mixin, no access transformer line, no client half, nothing in `src/main/resources`.
- Rule 4: `Rework` takes nothing, and nothing in its folder depends on a primitive. The primitive's only change is the
  check's static form.
- The game-test mod registers no `rework` of its own: the main mod's content registers the kind once, and the
  fixtures are recipes of it.
- Cost: what a shapeless recipe with complex ingredients costs, plus one assembly per match for the no-op rule, as
  transmute does.

## Proof

Unit tests under the main mod:
- `RecipeCheckTest`: the static form round trips through a codec with no key.
- `ReworkRecipeTest`, through that check under the registries, since the ingredient codec needs registry ops: the
  codec round trips a recipe that keeps types with every other field set, and one that removes types, and an empty
  `keep` stays apart from an absent one. It reads the defaults (misc, no group, the notification shown, no
  ingredients, everything carried, the base's remainder given back). It refuses `keep` with `remove`, each of which
  it accepts alone, nine ingredients, and a transient component type. The serializer's stream codec carries a recipe
  over a registry-aware buffer, and the recipe book gets a placement of the base then the ingredients, and a display
  of them, the template and the crafting table.

Scenes on the game-test server under `gametest/module/system/rework`, isolated, with a survival `TestPlayer`. A
crafting table is placed in the arena and its menu opened through the block's menu provider. The grid is filled
through the menu's slots, and the result is read from its slot and taken with a pickup click, so the game's own
menu, result slot and award run. The adapter needs no new helper. The fixtures are rework recipes in the scene pack,
on vanilla items, under `scene/data/jolcraft_gametest/recipe/rework/`. The checks:
- Everything carries: a named, dyed leather helmet and an iron ingot make an iron helmet with the name and the colour.
- `keep`: the same with a chestplate keeps only the name.
- `remove`: dyed leather boots and a water bucket make the boots without the colour, the name kept, with the empty
  bucket back in the bucket's slot.
- A craft that changes nothing: undyed boots and a water bucket give no result.
- The template wins: paper with one name and an ink sac make paper with the template's name. Paper that already
  carries that name gives no result.
- A lone base: a named clock alone makes a compass with the name.
- `keep` empty and `base_remainder` false: a named honey bottle and sugar make the template's potion without the name,
  and no glass bottle comes back. Another honey recipe gives the bottle back by default.
- A carry the game refuses gives no result: a base whose patch makes it damageable, onto a result that stacks.
- The recipe book: taking a result awards the recipe to the player's book. With the limited-crafting rule on, a
  player without the recipe gets no result until it is awarded. The rule is server-wide, so that scene sets it and
  restores it in its `onEnd`.

The datagen seam has no fixture. The first feature that writes a rework recipe runs it, as the first book recipe
through the provider (W6).

## Where things go, and the order

One step, ending with the gates in `CLAUDE.md`, in this order:
1. The check's static form in `RecipeKey`, with its `RecipeCheckTest` case, and the provider's check over every
   recipe and its unlock overload.
2. `Rework`, `ReworkContent` and `ReworkRecipe` in `module/system/rework`, the Dictionary words rework, base,
   ingredients, remove and remainder, and root wiring.
3. `ReworkRecipeTest`, the fixtures and the scenes.

Docs:
- `CLAUDE.md`, mirrored into `AGENTS.md`: the first paragraph's list and count of systems, the `module/system/`
  bullet, the recipe primitive's sentence on `key.check` and the provider, and the datagen bullet on `Recipes.Provider`.
- `module-plan.md`: a `rework` row in the shared-systems table, the sentence above it on what is built, the `recipe`
  row's note on the check, and a status line.
- This document's built note.

## Not in this design

- A shaped variant, and kinds of the mod's own under other game types (smithing, stonecutting): with their first
  consumer, each through the content class of its folder.
- Holding back the remainder of a stack other than the base, the old list form whose one use was the base: with its
  first consumer.
- The compass, dial and yeast recipes: feature data. Whether the compass dyes through vanilla's `crafting_dye`, which
  mixes colours, or through rework with each dye's exact colour in the template, is the compass feature's choice.
- Components the base must carry, the old `base_requirements`: the no-op rule covers its one use, and a base that
  must carry a value is NeoForge's component ingredient.
- The JEI category: the `integration` system's, which waits.

## Challenged 2026-11-11

All nine findings were confirmed. Changed:
- The kind is registered by a content class in its own folder, as the worldgen kinds are, since the module map has a
  folder's content register its actors. So the recipe primitive gains no declaration, no shared id space and no
  exception in `sync`. A module that only holds content and takes nothing is a system, as `Worldgen` is, so the kind
  is the `rework` system, not a composite over `recipe`.
- The round trip has one home, the static form of `key.check`, which the provider calls with a recipe's serializer
  codec.
- The docs list names every line of `CLAUDE.md` and `module-plan.md` the build changes, with the `AGENTS.md` mirror.
- What carries is stated with removals: a carried removal strips the result item's default, as in vanilla's transmute.
- The game's validation is stated as the little it is, and a refused carry is no match without a warning per grid
  change, with a scene.
- `RecipeManager` does not drop a recipe with an empty ingredient; it warns, and the ingredient matches nothing.
- The base's slot is mapped from the matcher's count of non-empty stacks back to the input's slots, and the cases
  where the assignment or the result slot's second lookup decide are named.
- The old `suppress_remainder` list is named as what `base_remainder` leaves for later, and the old overlap fault is
  stated as it was.
- The scenes say how they reach the crafting menu and the limited-crafting rule with the adapter as it is, and the
  unit test round trips under the registries.

## Built 2026-11-11

Built in the one step as planned:
- `RecipeKey.check(registries, codec, recipe)`, which the key's own `check` now delegates to.
- `Recipes.Provider`: `add(id, recipe)` and the new `add(id, recipe, category, unlock)`, which takes vanilla's
  `RecipeUnlockAdvancementBuilder`, both writing through one check over the recipe's serializer codec.
- `module/system/rework` with `Rework`, `ReworkContent` and `ReworkRecipe`, the Dictionary words base, ingredients,
  remainder, remove and rework, and the root building `Rework` and registering `ReworkContent`.
- `ReworkRecipeTest` (six tests) and the static form's case in `RecipeCheckTest`.
- Eight fixture recipes on vanilla items in the scene pack, and ten scenes in `ReworkScenes`, all through a real
  crafting table's menu.

Verified: 218 unit tests, `runData`, 139 of 139 scenes and `build` green. After the review only the unit tests and the
scenes' javadoc changed, and `compileGametestJava test` ran again green.

Reviewed by two lenses. Correctness found nothing: the slot mapping, the filters' direction, the codec's `keep`, the
static initialisation and the datagen path all hold. Proof found four gaps in the unit tests, all closed there with no
new scene:
- the `remove` round trip and the empty `keep`;
- controls for the `keep`-with-`remove` refusal;
- the stream codec;
- the recipe book's placement and display.

The code departs from the plan in nothing it does.

## Settled without the author

- The name is `rework`, distinct from vanilla's `crafting_transmute`, whose format differs.
- The serializer is built in the content class over the recipe's codec, so the recipe's one static is its codec, and
  the recipe extends `NormalCraftingRecipe`, as an `@Actor`.
- What carries over is the base's patch, removals included, as vanilla's transmute carries it. A same-item rework,
  such as undyeing or resetting a dial, keeps what the stack removed, and a recipe onto another item names what it
  keeps.
- The template's components win over the carried ones, vanilla's own order, so there is no `set`.
- A craft that changes nothing is no match, so there is no `base_requirements`.
- A refused carry is no match and logs nothing, since it comes from a player's stack, not from the data.
- `remove` stays beside the template's own `!` entries: it reads plainly, and leaves the result item's own defaults
  alone.
- `ingredients` is optional, so a lone base reads plainly.
- The kind is in the recipe book, as vanilla's data-driven crafting kinds are, and the provider refuses a book recipe
  with no unlock, as vanilla's builders do.
