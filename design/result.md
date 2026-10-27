# Results: the old recipe framework as generic modules

Draft of 2026-10-26, challenged by three reviewers (rules, fidelity to the old hosts, 26.1 feasibility; 18 findings
folded in), approved by the author on 2026-10-26; all four steps built; the last, the recipe values, the round trip and the sync, on 2026-10-27. Replaces the `outcome` shared system of `module-plan.md`,
which was JolCraft's own recipe executor, with generic pieces any system can use.

## What the old base was

`old/world/recipe/base`, 2,000 lines: an output is a loot-context user that validates itself and carries hook
ids; the four kinds are an item output (a vanilla `LootPool` rolled against a `LootContext`), an entity output
(type and count provider), a sound output (holder, source, volume and pitch providers) and an effect output (a
`MobEffectInstance`); kinds dispatch through one custom registry; hooks are behaviours by id, checked before
anything is consumed and run on each generated thing; inputs are loot conditions over an input-item key, with a
fate (consume or damage an amount, keep) applied by 400 lines of inventory code. Seven recipe kinds, the dwarf
actions, the hand-interaction event, three block entities and JEI host it. Its faults: every host re-dispatches on
the concrete class, 17 private copies of the context sets drift apart, the hook table and the registry are
statics, the base imports feature code, and most hosts only want the rolled value while the kinds act on the world.

## The generic pieces

| Piece | Where | Built from | Who uses it |
|---|---|---|---|
| **Context shapes.** `Shape`, a top-level `@Value` in the loot folder: a named `ContextKeySet` of required and optional keys. `loot.context(level, shape, [random,] params)` builds the `LootContext`, refusing a missing required key by the shape's name; a key beyond the shape is carried, as NeoForge allows, so the shape bounds what data may read, not what a caller may give; `shape.validator()` and `shape.check(user)` are the game's own `DataResult` check. The mod's context keys are `@Value` constants beside the module that owns them, as tags are; a host's shape carries its matched input stacks as keys of its own, so hooks and conditions read them from the context. | `loot` primitive, an addition | nothing new | every executor and every codec that validates against a context |
| **Dispatch.** `Dispatch.codec(registryKey, field, kindOf, codecOf)`: a dispatch codec over a custom registry resolved lazily by key through the game's root registry (`Codec.lazyInitialized`; datapack registries are not in that registry), so a codec held as a static constant, as `Recipe.Custom` records hold theirs, resolves the registry only when first used and holds no module. | `registration` primitive, a `@Helper` | nothing new | every registry of codec-dispatched data |
| **Yields.** In `composite/loot`: `Yield` is the `@Contract` a kind's record implements: `codec()` (its kind's), `hooks()` (ids, none by default), `validate(ValidationContext)` and `roll(LootContext)`, which answers `Yielded` values and never acts on the world. `Yielded` is the value contract a `Delivery` carries, with the four values the composite's kinds roll nested in it. `YieldKind` is the `@Handle` (`of(id, mapCodec)`) that declares a kind while a module is built; `ItemYield` (a loot pool, rolled with the game's own stack splitting), `EntityYield` (type and count), `SoundYield` (holder, source, volume, pitch) and `EffectYield` (an instance) are public `@Value` records with their own codecs. `ResultHook` is the second `@Contract`: `canApply(context)` asked before anything is rolled, `apply(context, rolled)` on each rolled value. `Result` is the `@Composite` over `loot`, `registration` and `log`: `declare(YieldKinds)` names the two registries (kinds and hooks) under the root's namespace through `registration.custom`, so the game-test mod's instance does not clash with the mod's; `declare(kind)` and `declare(hookId, hook)`, each refusing an id or a value twice; `register(modBus)` fills both registries at `RegisterEvent`, called once by the root. The `YieldKinds` handle's methods are what a host embeds, built lazily over the registries so a record keeps them as constants: `codec()` (one yield by the kind `type` names, refusing a hook id no module declared), `listCodec()`, `choice()` (a weighted list one of which is rolled), `single(kindCodec)` (a field typed as one kind, with the same hook check; a kind's bare codec checks no hook, so a host never embeds one), `streamCodec()` (`ByteBufCodecs.fromCodecWithRegistries`). `result.apply(yields, context)`, `apply(choice, context)` and `apply(yields, level, shape, params)` answer a `Delivery`: stacks, entities as type and count, effect instances, sounds, a module's own values through `of(class)`, and `refused` when a hook's `canApply` said no (an `apply` that says no drops that one value only). | composite | `loot`, `registration`, `log` | systems: the dwarf actions, the hand-interaction listener, the station systems; an actor (a block entity, a loot modifier) announces with an `@Event` and its system applies |
| **Item inputs.** `ItemInput` and `Fate`, top-level `@Value` types in the recipe folder: an `ItemPredicate` whose own count must be unbounded (a `DataResult` error otherwise, so the amount has one source), a `Fate` of consume, damage or keep with an inclusive amount range rolled on application, `matches(stack)` pure on both sides, `fate.apply(stack, level, holder)` shrinking, damaging through the game's own wear (so unbreaking applies, a worn-out stack empties and the holder hears the break) or leaving the stack, and paying nothing for a creative holder; a stack covers a consume fate by its count and a damage fate by being damageable. | `recipe` primitive, an addition | nothing new | every recipe kind with items in; a trade's cost stays its own record over an `Ingredient` and a count provider |
| **Round trip.** `key.check(registries, recipe)` on the recipe key in main, unit-tested: writes and reads a recipe back through its data codec and answers the `DataResult`; `Recipes.Provider.add` calls it, so a bad hook id, a missing key or a refused amount fails `runData`. It proves structure only: the read goes through the registries' lookups, where a tag of the mod's is an empty set at datagen, so a tag input survives the check and no codec validation may read a tag's members. | `recipe` primitive and the datagen core | | every recipe contributor |

What stays with the features: the recipe records (`Recipe.Custom` with their matching), what a system does with
a `Delivery` (a bench fills its slot, a dwarf hands over, a crate holds, a cauldron merges the effect into its
brew, a bounty stores the rolled objective) and the compass hook. The `effect` shared system later takes over how a system applies an effect instance, not the kind.

## Lifecycle

Built: a module declares its kinds and hooks in its constructor; the root calls `result.register(modBus)` once.
Load: the host's codec decodes yields through the dispatch codec and validates through `Shape.validator` and each
kind's `validate`; every check is a `DataResult` error, never a throw, since the recipe loader drops a recipe on an
error but aborts the whole reload on an exception. Use, server only: the system matches (pure), builds a context
from its shape with the matched stacks in it, asks `result.apply`; the composite asks every hook's `canApply`
first, then rolls each kind and runs its hooks; the system consumes inputs only when nothing refused, then acts on
the delivery. Sync: the kinds registry is not synced, since a client with the mod resolves kinds by id; recipes
reach clients only through NeoForge's opt-in `sendRecipes`, which the recipe primitive requests for its declared
kinds, carrying inputs and yields whole for JEI; `Recipe.Custom` shows no recipe-book display.

## Proof

Unit tests: shapes and their validator, dispatch over the game's registry and over a missing one, each kind's codec round
trip, the item kind's check against a shape, the composite's guards, the fate and input codecs, `RecipeKey.check` refusing on the
way back. Scenes in the game-test mod with its own registries: a kind of the declared registry decoded through the dispatch codec; the choice and wire forms; a field typed as one kind checked for its hooks; an item pool rolled against a context; a hook
that refuses sets `refused` and one that rewrites a stack; a recipe in the test data pack carrying yields is found
and applied; a recipe with an unknown hook is absent after load, refused by the composite's own message; item inputs matched and their
fates applied on real stacks, a creative holder paying nothing; the sync request answered with every declared kind and the
recipes reaching a player who joins.

## Where things go, and the order

`Result` is a composite over `loot`, `registration` and `log`; its kinds only roll values and the systems act on
them. Context keys and shapes are `@Value` constants beside the module that owns them, as tags are; a host puts its
matched input stacks into the context under keys of its own shape. The order is four steps, one at a time: the
`loot` addition (shapes, context builder, validator), the `registration` helper (dispatch), the `result` composite,
then the `recipe` values and the round trip.

## Sync, settled 2026-10-26

The game no longer sends recipes to clients; NeoForge sends a mod's recipe kinds only when the mod asks. The recipe
primitive asks for every declared kind at the sync event, one line per kind, and each kind's stream codec carries
its inputs and yields whole, so the JEI bridge, generic plumbing of the planned `integration` system, can show
them. The registries themselves stay unsynced, since a client with the mod resolves kinds by id.
