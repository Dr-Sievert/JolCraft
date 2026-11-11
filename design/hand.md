# Hand: two-hand interaction recipes

Draft of 2026-11-11, from a read of the old tree's hand-interaction recipe, its event handler and its three recipe
sets against vanilla 26.1.2 with NeoForge 26.1.2.122's patches. The generic support the author asked for: a recipe
kind features fill with data, used through NeoForge's own interaction event, with no mixin. The `hand` system with a
client half, the `outcome` system holding the mod's yield keys and delivering what results roll (`outcome.md`), one
addition to the `inventory` composite, four build steps. Decided 2026-11-11, see "Decided". Challenged 2026-11-11 by two lenses (the rules,
and 26.1.2 feasibility with fidelity to the old tree): ten of eleven findings confirmed and folded in, see
"Challenged". Built on 2026-11-11 in all four steps, see "Built 2026-11-11 (step 1)", `outcome.md`, "Built 2026-11-11
(step 3)" and "Built 2026-11-11 (step 4)"; the author did the hand check of the client half the same night, and it
holds.

## What the old tree did

`world/recipe/custom/hand/HandInteractionRecipe` (602 lines) and `event/game/world/recipe/JolCraftHandInteractionEvents`
(529). The recipe: `ingredient_a` and `ingredient_b`, each an old item input with an action (consume or damage by an
amount or a range, else `catalyst`, the default, which keeps it); `results`, a list of old outputs, at least one;
`success_sound` and `fail_sound`, both required; `require_sneaking`, default false. Matching tried A in the main hand
and B in the offhand, then the reverse.

The handler, on `PlayerInteractEvent.RightClickItem`: the main hand only, a server player, both hands filled, neither
item on cooldown; the recipes of the kind matching the two stacks, in the map's order, a sneaking recipe skipped
unless the player sneaks; the first that matched with A in the main hand, else with A in the offhand. The context
was `THIS_ENTITY` (the player) and its own `input_item` (A's stack). When a hook's precondition refused, the fail
sound played, both items got the cooldown and the click went through to vanilla's own use, logged at info.
Otherwise it cancelled the click with `SUCCESS` and rolled the outputs, acting on each by its class: stacks into the
inventory or dropped, sounds at the player, effects on the player, entities spawned at the player. It then played
the success sound when anything came out, else the fail sound, applied A's and B's actions, put a 10-tick cooldown
on both items, swung A's hand to the player as well, and logged at info.

Three sets used it. The compass is put together from an empty compass and a dial, both consumed, into a compass
carrying the compass hook. The spanner salvages: the spanner is damaged by a range per material, the salvage item is
consumed, a weighted pool comes out (mostly empty), with a sound per material and the book sound as the fail. The dev
key exercised every output kind.

Its faults:
- The actions ran after the outputs, which is the pay-after-give flaw `inventory.md` names, and the two hands were
  paid one at a time.
- Nothing stopped the client's own prediction. It ran the main-hand item's use (food started to be eaten, armour was
  equipped client-side). When that passed, it went on to the offhand: its block placement, then its use. Only the
  cooldown stopped the server from running that second use.
- A refusal let vanilla's use run.

## What 26.1.2 gives

- `Minecraft.startUseItem` tries the main hand, then the offhand. For each it gives an entity or a block in reach the
  click first, then calls `useItem`, and any `Success` ends the loop.
- `MultiPlayerGameMode.useItem` checks the cooldown, then posts `RightClickItem` on the client through
  `CommonHooks.onItemRightClick`. When the event is cancelled its result stands; otherwise the client predicts the
  item's use. The packet is sent either way.
- `ServerPlayerGameMode.useItem` answers `PASS` for an item on cooldown before it posts the event. So a cooldown set
  during the main hand's packet stops the offhand's packet of the same click.
- `ServerGamePacketListenerImpl.handleUseItem` swings only for a result whose swing source is the server.
  `InteractionResult.CONSUME` is a success with no swing.
- `ItemCooldowns`: `isOnCooldown(stack)`, `getCooldownGroup(stack)` (the `use_cooldown` group, or the item's id),
  `addCooldown(stack | group, ticks)`.
- `LivingEntity.swing(hand, true)` sends the animation to the swinging player too. `Player.isSecondaryUseActive()` is
  `isShiftKeyDown()`.
- `LootContextParams.TOOL` is a `ContextKey<ItemInstance>`, which `ItemStack` implements. It is what `match_tool` and
  the enchantment-level functions read. `THIS_ENTITY` and `ORIGIN` are as before.
- NeoForge's `RecipesReceivedEvent` (client, game bus) carries a `RecipeMap` of the kinds a server asked to send. The
  recipe primitive's `sync` asks for every declared kind. NeoForge's javadoc says to drop what was kept on
  `LoggingOut`. A world load fires its `LoggingOut` from `disconnectWithProgressScreen`, the first line of
  `doWorldLoad`, before the integrated server starts, so before any recipe arrives.
- What the stack below already gives, in our own primitives and composites:
  - `RecipeKey` and `Recipe.Custom`. `ItemInput` with its `Fate`, whose `matches(stack)` is pure on both sides.
  - `Result.apply` with hooks asked first, answering a `Delivery`. `YieldKinds.listCodec()` and `single(kind)`.
    `Shape` with its validators.
  - `Inventory.match` on a `Holder.Hand`, then `pay` and `give` with the creative rule and crafting remainders.

## The system

| Piece | What it is |
|---|---|
| **`hand`.** | `module/system/hand`. `Hand`, `@ModSystem(Dictionary.HAND)`, takes `recipe`, `result`, `inventory`, `outcome` and `log`. Its constructor declares `HandKeys.RECIPE` to `Recipe`; `onUse` handles `PlayerInteractEvent.RightClickItem` on the game bus, registered by the root. |
| **`HandKeys`.** | `@Keys`: `RECIPE = RecipeKey.of(jolcraft:hand, HandRecipe.CODEC, HandRecipe.STREAM_CODEC)`. It holds no `YieldKinds`. One class holding both the record's key and the kinds its codec embeds would initialise in a cycle; the kinds are `OutcomeKeys.YIELDS` (see "Decided"). |
| **`HandRecipe`.** | A record implementing `Recipe.Custom<HandInput>`, with no stereotype. It names two handles, its key through `key()` and the mod's `YieldKinds` in its codec, and the architecture test refuses a `@Value` that depends on a handle; a system folder is unrestricted. No static of it reads `HandKeys`, so initialisation runs one way, `HandKeys` to `HandRecipe` to `OutcomeKeys`, and `key()` reads `HandKeys.RECIPE` only when called. `tool` and `target`: an `ItemInput` each, fate included, with the recipe primitive's codec. `results`: the mod's yields through `listCodec()`, at least one, validated against `HandContext.SHAPE`, and each one of the four kinds of the composite: another module's kind is refused at load, since `outcome` delivers those four alone. `success` and `failure`: optional `SoundYield`s through `single(SoundYield.CODEC)`, validated against the same shape. `sneaking`: boolean, default false. `cooldown`: ticks, default 10, at least 1. `orientation(main, off)` answers the tool's hand: the main hand when `tool` matches the main-hand stack and `target` the offhand's, else the offhand for the reverse, else none. `matches(input, level)` is that orientation plus the sneak rule, pure, the same on both sides. `STREAM_CODEC` is `ByteBufCodecs.fromCodecWithRegistries(CODEC.codec())`, as `YieldKinds.streamCodec()` is, since `ItemInput` and the game's `ItemPredicate` have no stream codec of their own. So a client reads the recipe through its data codec, with the hook check and the predicate's tags resolved against its own registries. |
| **`HandInput`.** | A `@Value` record implementing `RecipeInput`: the main-hand stack, the offhand stack, and whether the player sneaks. `isEmpty()` is true when either hand is empty, so the game asks no recipe then. |
| **Context.** | `HandContext`, a `@Value` closed set, since a value constant keeps its `@Value` set. `SHAPE`, keyed `jolcraft:hand`, requires `THIS_ENTITY` (the player), `ORIGIN` (the player's position), `TOOL` (the matched tool stack) and `TARGET` (a `ContextKey<ItemInstance>` under `jolcraft:target`, the matched target stack). Both stacks are the match's copies, so a hook reads them and changes nothing in a hand. |
| **Choosing.** | Among the recipes of the kind on the level that match, one that requires sneaking wins over one that does not, so a pair can have a sneaking variant. Otherwise the first in the map's order wins, so two recipes over one pair, both sneaking or both not, are a data error. |
| **Firing.** | Server side, from the main hand's event, at `EventPriority.LOW`. Like every default listener it never sees a cancelled event, so a system or another mod that forbids the use at a higher priority wins. The handler returns at once unless the level is a server's, both hands are filled, neither stack is on cooldown and a recipe matches. Then, in order: (1) both stacks' cooldown groups are noted, since a consumed stack no longer names its item; (2) `inventory.match` on `Holder.Hand` of the tool's hand with `[tool]` and of the other hand with `[target]`; (3) `result.apply(results, level, HandContext.SHAPE, params)`. (4) A refused delivery plays the failure sound and pays nothing. Otherwise (5) `inventory.pay` takes both matches in one transaction; a shortfall (only a hook changing a hand could cause one) is logged at warn, plays the failure sound and changes nothing. (6) `outcome.deliver(delivery, player)` gives, spawns, adds and plays it (`outcome.md`). (7) The success sound plays when the delivery holds anything, else the failure sound, so an empty roll is paid, as in the old tree. A sound of the recipe's own is rolled through `result.apply` against the same shape and delivered the same way, on every path that plays one. (8) The tool's hand swings with `swing(hand, true)`. On every path after a match, a refusal and a shortfall included, both cooldown groups get `cooldown` ticks, as the old tree did so that a refusing hook cannot be asked every four ticks while the button is held, and the event is cancelled with `CONSUME`; only a firing swings. Each firing and refusal is logged at debug. |
| **Client half.** | `hand/client/HandClient`, a `@Helper` the root calls after the dist check with the game bus. On `RecipesReceivedEvent` it keeps the kind's recipes from the map in an `@Internal` holder captured by its listeners, not a static; it drops them on `LoggingOut`. On a client level's `RightClickItem` from the main hand, at `EventPriority.LOW` as on the server, with both hands filled and neither on cooldown, a kept recipe matching by the same `matches` cancels the event with `CONSUME`. So the client predicts no use, swings nothing itself (the server's swing reaches it) and does not go on to the offhand. The client asks no hook: a refusal on the server still consumes the click, so both sides agree. |

## Lifecycle and rules

Built: the root builds `Hand` after `Recipe`, `Result`, `Inventory` and `Outcome`, and registers `onUse` on the game bus. Inside
the dist check it calls `HandClient.register(gameBus)`. The kind is declared before the root registers `Recipe`.
Load: a hand recipe is read by the kind's codec. An empty `results`, a kind the system cannot act on, a yield or sound
reading a key outside `HandContext.SHAPE`, a hook no module declared, a bounded predicate count or a `cooldown` below 1 is a
`DataResult` error, so the recipe is dropped with the game's own message. Sync: the recipe primitive already sends
the kind to every client.

- No actor, no mixin, no access transformer line, no content, nothing in `src/main/resources`.
- Rule 4: `Hand` takes `recipe`, `result`, `inventory`, `outcome` and `log`. `HandKeys` depends on its handle, the record and
  values, never on a module.
  `HandClient` is the only class that imports client types, in `hand/client`.
- `RightClickItem` alone, as in the old tree. An entity or a block in reach answers the click first (vanilla's order):
  a main-hand block item looked at a block is placed, a chest opens. The interaction fires when the click would
  otherwise use the item.
- A creative player matches as anyone does and pays nothing, through `Fate.apply`.
- Pay before give, both hands in one transaction, a hand's crafting remainder given back to that hand first.
- A server recipe the client has not received (before the first sync, or with the client half missing) leaves the
  client predicting as before. The cooldown still stops the server's own second use, and the client shows its
  prediction until the game corrects it, as in the old tree.
- Cost: an early return for a click that is not a filled main hand with a filled offhand. Otherwise one pass over the
  kind's recipes, a handful.

## Proof

Unit tests under the main mod, registers filled:
- The codec round trips a recipe.
- It refuses an empty `results`, a `cooldown` of 0, a bounded predicate count, a yield of a kind outside the four (a
  test record implementing `Yield`), and a sound reading a key outside the shape.

The test JVM cannot build an `ItemStack`, so `orientation`, `matches` and `HandInput.isEmpty` are proven by scenes.

Scenes on the game-test server under `gametest/module/system/hand`, isolated, with a survival `TestPlayer` driven
through `player.gameMode.useItem`, the server's own path with its cooldown check. The fixtures are recipes of the kind
in the scene pack, on vanilla items, under `scene/data/jolcraft_gametest/recipe/hand/`:
- The main ones are shears (damaged by a range) on wool (consumed), a sneaking variant of that pair, flint (kept) on
  gravel (consumed) with an empty pool, a water bucket (consumed) for the remainder, one result of each of the four
  kinds, and an apple (kept) on a stick (consumed), a tool with a use of its own that stays in its hand.
- One more, bread on paper, names a refusing hook. The dev mod registers that hook into the mod's hook registry at
  `RegisterEvent`, under its own namespace.

The checks:
- Either orientation fires, and the swing names the tool's hand.
- The tool is worn and the target consumed by the fates, and the string arrives.
- The success sound reaches the player, and both noted cooldown groups are on cooldown (asked directly, since a
  consumed stack's group is air).
- With the apple kept in the offhand, the offhand's use right after answers `PASS` and the player is not eating.
- An empty roll pays and plays the failure sound.
- The sneaking variant wins only while sneaking.
- A refusal pays nothing, plays the failure sound, sets the cooldowns, does not swing, and runs no use (the bread in
  the main hand is not being eaten).
- A recipe with a tag predicate and a hook-naming yield survives its stream codec over the level's registries.
- Each of the four kinds is delivered.
- The bucket's remainder lands in its hand.
- A creative player pays nothing.
- An empty hand, an item on cooldown or a pair no recipe holds leaves the click to vanilla.

The two-hand pay has its own scenes in step 1. The client half has no headless proof. It is checked by hand in
`runClient` after `/datapack enable "mod/jolcraft_gametest:scene"`: with a fixture whose main-hand item has a use of
its own, the click shows no predicted use and no client-side equip, and the swing comes from the server.

## Where things go, and the order

Four steps, each ending with the gates in `CLAUDE.md`.

1. **Two hands paid together** (built 2026-11-11). `Inventory.pay(List<Match>)` pays several matches in one root transaction:
   - each match keeps its own holder and handler: the no-handler answer, then the coverage check;
   - every consume and damage of every match is extracted, and a shortfall anywhere commits nothing and answers false;
     two matches over one holder are paid as one, so a claim that falls short because the other took it answers false;
   - then each damage is worn and the remainders go back to each match's holder;
   - then the commit, and each holder's overflow drops.

   `pay(Match)` stays as `pay(List.of(match))`, so `take` and the pay scenes are unchanged. The method's javadoc,
   `inventory.md`'s pay row and the `CLAUDE.md` line say several matches. Scenes: two hand matches paid together; the
   target's hand emptied between the match and the pay answers false and leaves the tool's hand unchanged.
2. **Outcome** (built 2026-11-11, see `outcome.md`, "Built 2026-11-11"). As `outcome.md` plans it: `Outcome` and `OutcomeKeys.YIELDS`, into which the mod's `YieldKinds` moves
   out of the root's constructor (see "Decided"), built from `Dictionary.JOLCRAFT`, not `JolCraft.MOD_ID`, since nothing
   below the root reads the root (rule 5), and naming the same registries as now; `deliver` with its scenes.
3. **Hand** (built 2026-11-11, see "Built 2026-11-11 (step 3)"). `Hand`, `HandKeys`, `HandRecipe`, `HandContext` and `HandInput`. The Dictionary words: hand, tool, target,
   results, success, failure, sneaking. Then root wiring, the unit tests, the fixtures, the dev mod's refusing hook and
   the scenes.
4. **The client half** (built 2026-11-11, see "Built 2026-11-11 (step 4)"). `HandClient`, root wiring after the dist
   check, the hand check.

No datagen change. A feature's recipe contributor writes hand recipes through `Recipes.Provider.add`, which runs
`key.check`, the first real recipe through that walk (W6). The docs were written with each step.

## Not in this design

- Using an item on a block or an entity: those clicks belong to the block or entity first. A recipe kind of its own
  can follow with its first consumer.
- The JEI category: the `integration` system's, which waits.
- The old recipes (compass, spanner, dev key) and the compass hook: feature data and feature code.
- An event announcing a firing, for a statistic or an advancement: with its first consumer.

## Challenged 2026-11-11

Ten of the eleven findings were confirmed. Changed:
- `HandRecipe` carries no stereotype, since the architecture test refuses a `@Value` that names a handle, and the
  context's constants are a `@Value` set of their own.
- `pay(List<Match>)` is spelled out per match, and `pay(Match)` stays, so its callers are untouched.
- The yield keys are built from the Dictionary, not the root's constant.
- The second home for them is a separate keys class, since one class holding the recipe's key as well is the cycle,
  and my pick is named `outcome`, leaving `Yield` to the composite's contract.
- The docs list gains `CLAUDE.md`'s count of systems.
- The cooldowns are set on every path after a match, refusals included, as in the old tree.
- The stream codec is stated, with a scene that a recipe survives it.
- The offhand check uses a kept tool with a use of its own and asks the cooldowns directly, since a consumed stack's
  group is air and proves nothing.
- Both listeners sit at `EventPriority.LOW`, so a canceller at a higher priority wins.

Refuted: that the client half is the removed `control`'s kind of work. It has no mixin, serves every recipe of the
kind and is the fix of the old desync; its proof stays the hand check.

## Settled without the author

These depart from the old tree:
- A refusal consumes the click, so client and server agree.
- The fates are paid before the delivery, and both hands in one transaction.
- The client predicts the interaction.
- A kind the system cannot act on is refused at load rather than skipped.
- The cooldown is per recipe, the old 10 by default, and the groups are noted before paying.
- `ItemInput`'s fate defaults to consuming one, the recipe primitive's own default, where the old default kept the
  item.
- The sounds are optional.
- Logging is at debug, since a firing is no news to a server owner.
- The names are `tool` and `target`, the context's `TOOL` being the tool's stack.

## Decided 2026-11-11

The mod's one `YieldKinds` lives in a system of its own, `module/system/outcome`, as `OutcomeKeys.YIELDS`, and the
root declares it to `Result` from there. It is named `outcome`, not `yield`, so the composite's `Yield` contract keeps
its name. It is not inside `hand`, since the later hosts would import it from there, and never in `HandKeys`, which
would be the cycle. It also delivers what a result rolls, for every host, decided the same day and planned in
`outcome.md`; `Hand` takes it for that.

## Built 2026-11-11 (step 1)

Built: `Inventory.pay(List<Match>)` over a private `Paying` record per match, its handler resolved once, holding the
tools to wear, the remainders and the overflow its extracts leave; `pay(Match)` is `pay(List.of(match))`, so `take`
and its callers are untouched. Across the matches every extract comes first, then every wear, then every remainder, so
a later holder's shortfall rolls back before anything is worn, and a remainder never takes a tool's slot before the
tool is back. Two scenes in `PayScenes`: shears worn in the main hand and a water bucket's empty bucket in the
offhand, paid in one call; an offhand emptied since its match, then two claims of two wool on a hand of three, each
answering false with the main hand's shears unworn, the second after the shears were extracted and rolled back.
Verified: 205 unit tests, `runData`, 115 of 115 scenes and `build` green.

A keep extracts nothing, so it never falls short: two matches over one holder that keep the same single item both
pay. The hand system never meets it, since its two holders are different hands.

## Built 2026-11-11 (step 3)

Built: `Hand`, `HandKeys`, `HandRecipe`, `HandContext` and `HandInput` in `module/system/hand`, the seven Dictionary
words, the root building `Hand` after `Outcome` and adding its listener to the game bus. `HandRecipeTest` (five tests)
runs the codec over the main mod's registries, which the test JVM fills: a round trip through `key.check`, the
defaults, and the refusals of no result, a cooldown of 0, a bounded count, a kind outside the four and a sound reading
the attacker, with the same sound reading the player accepted beside it. The fixtures are seven recipes in
`scene/data/jolcraft_gametest/recipe/hand/`: shears (damaged by one to three) on white wool for two string, its
sneaking variant for two white carpet, flint kept on gravel with an empty pool, a water bucket on dirt for mud, a
blaze rod kept on gunpowder with blaze powder, an armor stand, haste and a bell, an apple kept on a stick for a
sapling, and bread, named through the common tag `c:foods/bread`, on paper with a yield naming
`jolcraft_gametest:refusing`. `TestHandHooks` registers that hook into `jolcraft:yield_hook` at `RegisterEvent`. Ten
scenes in `HandScenes` cover every check of "Proof". Verified: 211 unit tests, `runData`, 129 of 129 scenes and
`build` green.

Where the build goes past the plan:
- The constructor refuses no result, a kind outside the four and a cooldown below 1 in code too, as `ItemInput`
  refuses a bounded count, so a recipe a feature builds for datagen fails where it is made.
- `HandRecipe.COOLDOWN` names the default of 10.
- The handler also returns, with nothing done, when the chosen recipe's orientation or either hand's match comes back
  empty. Neither can happen, since both read the same `ItemInput.matches`; the return keeps a mismatch from consuming
  a click it cannot fire.

## Built 2026-11-11 (step 4)

Built: `hand/client/HandClient`, the `@Helper` bootstrap, and `ReceivedRecipes`, the `@Internal` holder its
listeners capture: `RecipesReceivedEvent` keeps the map's recipes of the kind, which is none when the server sent
none, `LoggingOut` drops them, and a client `RightClickItem` from the main hand at `EventPriority.LOW`, with both hands
filled and neither on cooldown, is cancelled with `CONSUME` when a kept recipe matches by `HandRecipe.matches`. The
root calls it inside the dist check. The data run, on the client dist, registers it; the game-test server, on the
dedicated one, never loads it. Verified: 211 unit tests, `runData`, 129 of 129 scenes and `build` green, the counts of
step 3, since no test reaches the client.

The hand check, done by the author on 2026-11-11 and passing: in `runClient`, in a survival world with cheats, after
`/datapack enable "mod/jolcraft_gametest:scene"` and a little hunger, an apple in the main hand and a stick in the
offhand, clicked at the sky: no eating starts, the arm swings once, a stick is taken, a sapling arrives and the apple
stays. Bread in the main hand and paper in the offhand: no eating, no swing, the page-turn sound, nothing taken.
