# JolCraft — Module Plan

Every module the 1.0 features need, derived from the 1.21.1 tree in `old/` and the rules in `module-map.md`.
Written 2026-10-24 at the end of round two of the primitives. Features themselves are not migrated here: this
plan finishes the ground they stand on. A module is "built" once it has its interface and its `Mod` implementation (a primitive) or its one class (a
composite or system),
unit tests where a tick-free assertion exists, scenes where it touches the world, its datagen seam, a passed independent review, and a line in
`CLAUDE.md`.

## Primitives

Rounds one and two, built: `log`, `registration`, `language`, `config`, `network`, `attachment`, `data`, `sound`.

Round three, in build order. Each follows the shape the first eight settled: where a primitive has something to
declare per module, the module declares keys while it is built, the root registers once and a `@Handle` key carries
the runtime use; where it has nothing to declare, `schedule`, the runtime use is on the module itself. Nothing is
static in the mod; the game-test mod's plumbing hands scenes what its root built, as `module-map.md` rule 5 allows.

| Primitive | Job | Replaces in `old/` | Datagen seam |
|---|---|---|---|
| `schedule` | server-thread tasks after N ticks or every N ticks, cancellable, dropped when the server stops; failures reported through a contract the root adapts from `Log` | the per-entity tick counters in events and goals, the brewing sleep handler's ticking | none |
| `recipe` | a module's recipe types and serializers as keys, with `key.find(level, input)` and `key.all(level)` over the level's recipe access | `world/recipe/JolCraftRecipes`, the custom recipe types' registrations; not `world/recipe/base`, whose generic pieces were built from `result.md`: context shapes in `loot`, the dispatch helper in `registration`, the `result` composite, and `ItemInput` with its `Fate`, `key.check(registries, recipe)` and `sync(gameBus)` here (built 2026-10-27, step 4) | `RecipeContributor` (exists) |
| `loot` | loot modifier serializers, loot condition and function types as keys, `roll(level, table, params)`, and `Shape` with `context(level, shape, [random,] params)` to validate data against a loot context's keys and build the context (built 2026-10-26, step 1 of `result.md`) | `world/loot/JolCraftLootModifiers`, `AddItemModifier`, `AddLootTableModifier` | `LootContributor` (exists) plus a loot-modifier provider |
| `advancement` | custom criterion triggers as keys with `key.fire(player, matcher)`, and the advancement datagen provider with its completeness (title and description text) | `world/entity/player/advancement/*`, `datagen/advancement/*` | new `AdvancementContributor` and `Advancements` provider |
| `menu` | menu types as keys, `open(player, title, menu, extra data)`, and the client half that gives a menu its screen | `world/gui/JolCraftMenuTypes`, `JolCraftMenu`, the screen registrations | none |
| `command` | a module's Brigadier command trees declared while it is built, registered once at `RegisterCommandsEvent` (`module-map.md` says so since this plan) | nothing; roadmap item 9 is new, and the old dev key's dwarf level-up is a candidate for a dev command | none |
| `capability` | capability providers for block entities, entities and items declared while a module is built, registered once at `RegisterCapabilitiesEvent` | `world/capability/JolCraftCapabilities`, `event/mod/JolCraftModEvents` | none |
| `stat` | custom stats as keys with `award(player, key, amount)` and `value(player, key)`; the offline queue the old `PendingStatData` kept is the `ledger` composite below | `data/id/stat` | language text for stat names |

Not primitives, on purpose: items, blocks, entities, effects, particles and creative tabs are `@Content`
declarations in the system that owns them; tag keys and data-map types are constants in a `@Value` closed set of
that system (a `@Content` holds registers and holders only); the handles a record must name before the system
exists (a `RecipeKey`, a `YieldKinds`) are constants in the system's `@Keys` set, settled 2026-10-27; the system's
`@Content.register(bus)` adds the `RegisterDataMapTypesEvent` listener for its data-map types; datapack registries
go through `registration`; events are
`@Event` classes posted on NeoForge's bus; mixins are outside the tiers.

## Composites

Built from primitives only, in the folder of the primitive they extend.

| Composite | Folder | Built from | Job | Replaces in `old/` |
|---|---|---|---|---|
| `action` | `composite/schedule` | schedule, log | a multi-tick transaction an actor performs: ordered timed steps, cancel, and recovery when the actor is interrupted or unloaded mid-way | `world/entity/custom/dwarf/action/*` (the framework, not the dwarf actions) |
| `feedback` | `composite/language` | language, log; the sound arrives as a key the caller holds | telling a player something: a message to chat or the action bar, with a sound, in one call, on the server | the scattered `displayClientMessage` and sound pairs in handlers |
| `cooldown` | `composite/attachment` | attachment, log | per-entity cooldowns in ticks that survive a save, kept as the game time they end at, asked as `ready(entity, key)` and `start(entity, key, ticks)`; no timer runs, the game time decides | the dwarf busy gates and the endorsement and promotion timers; the per-tick packet limiter is another thing and stays with the system that needs it |
| `ledger` (built 2026-10-24) | `composite/stat` | stat, data, log | awarding a statistic to a player who is offline: the award waits in saved data and is granted when the player next joins | `world/data/custom/PendingStatData` |
| `result` (built 2026-10-26, step 3 of `result.md`) | `composite/loot` | loot, registration, log | data-described yields rolled against a loot context, for any system: item pools, entities, sounds and effects as kinds in a registry declared per root, hooks by id checked at load, one `apply` answering a delivery of values the system acts on; designed in `result.md`, challenged, approved 2026-10-26 | `world/recipe/base/output/*`, the hook table |
| `inventory` | `composite/capability` | capability | counting, taking and giving items against a holder's item handler, found through the game's capability lookups, with the overflow dropped at the holder; the 26.1 transfer API it stands on is `ResourceHandler<ItemResource>` (indexed slots: `size`, `getResource`, `getAmountAsInt`, `getCapacityAsInt`, `isValid`, `insert(index, resource, amount, transaction)`, `extract`), every change inside a `Transaction.openRoot()` that is committed or closed, and `ResourceHandlerUtil.insertStacking`, `extractFirst` and `move` for the common moves; the handler comes from `Capabilities.Item.BLOCK` or `ENTITY` lookups, an `ITEM` holder coming with its first consumer; designed in `inventory.md` (2026-10-27, challenged by three reviewers): holders per call, `count`, `match` and `pay` over `ItemInput`, `give` with overflow dropped at the holder, a block entity exposed through `capability`, in two steps, both built 2026-10-27 | `world/item/inventory/*`, the coin pickup helpers |
| `station` (step 1 built 2026-10-28, step 2 (client) open) | `composite/menu` | menu, log | the container stations: a kind with a layout of role-typed cells, a block entity over the game's storage base, a block, a menu with one role-driven quick-move, a slot with the result bookkeeping, and `Button` and `Changed` events for the owning system; `Tick` and `Sync` serve any block entity; designed in `station.md` | `world/gui/menu/JolCraftMenu`, `gui/slot/*`, `gui/client/screen/JolCraftScreen`, the two base interfaces |

## Shared systems

Systems that several features stand on and that are not features themselves. Built after the composites, in
this order; none is built yet, so nothing is forgotten.

| System | Takes | Job | Replaces in `old/` |
|---|---|---|---|
| `integration` | log | the optional-mod bridges: a JEI plugin and a Jade plugin that are `@Bridge` classes behind a `ModList` guard, each giving a feature system one place to add its categories and tooltips | `integration/jei`, `integration/jade` |
| `worldgen` | registration, log | the structure plumbing every structure shares: the jigsaw structure base with the terrain veto, the structure processors and rule tests, the placement modifiers, declared as content and bootstrapped in datagen | the `structure`, `processor` and `placement` folders of `world/worldgen`; a vegetation feature or biome modifier belongs to the feature system that owns the plant, as `@Content` with a `RegistryContributor` |
| `effect` | attachment, schedule, network, log | the effect plumbing: stacking and duration rules, crowd control, the client-side effect payloads | `event/game/world/entity/effect/*`, `world/entity/effect` |
| `economy` | log | the coin items, their values and the pay and give helpers trade and bounties share | the coin items in `world/item/JolCraftItems`, `world/item/custom/container/CoinPouchItem`, `JolCraftCoinPickupEventsHelper` |

## Feature systems, for later

Not built by this plan. Each takes the primitives, composites and shared systems above.

`dwarf` (entity, variants, loadouts, AI base, interaction pipeline), `profession`, `trade`, `reputation`,
`bounty`, `brew` (cauldron, corruption, essences, the sleep handler), `tome`, `strongbox`, `lapidary`, `mortar`,
`hearth`, `compass`, `structure` (the fortress and its pieces), `curse` (delirium, ataxia), `armor` (sets and
trims), `muffhorn`, `flora` (barley and hops, the wild plants and mushrooms, verdant soil and farmland, their
planting and bonemeal rules, their vegetation features and biome modifiers, and the events its two mixins post;
`muffhorn`, `brew`, `trade` and `bounty` reach its items), `crate`.

## Status, 2026-10-27

The generic layer is complete and committed on `main`: every primitive in the two tables above (sixteen) and every
composite (`action`, `feedback`, `cooldown`, `ledger`, `result`, `inventory`), each with scenes on the game-test
server, unit tests where a tick-free assertion exists (`feedback` needs a player and is proven by a scene alone;
`log` has none), a datagen seam where it has one, an independent review and a `CLAUDE.md` line. No system is
built; the shared systems come next, in their table's order. The history below says how the layer got here.

The night of 2026-10-24 built every round-three primitive (`schedule`, `recipe`, `loot`, `advancement`, `menu`,
`command`, `capability`, `stat`) and the composites `action`, `feedback` and `cooldown`. No system was built: an `economy`
system with a coin, a tab and a texture was built overnight as the worked example of a system and removed the
next morning at the author's direction, since those are feature content and no feature is migrated yet. The game-test mod declares its own keys of every kind in its root and exposes what its root
built to the scenes through static accessors, the one exception to rule 5 that `module-map.md` states.

The night's last whole-tree review left twelve open items, listed in `REVIEW_TODO.md` under W1 to W12; the
architecture test now checks that the root reaches every implementation, content class, composite and system,
and the code refuses tasks on an unattached schedule, writes nothing for a saved attachment equal to its fallback,
keeps cooldowns over a death, demands English for every `LanguageKey` constant in a `@Value` class, and follows a
test player's live server player across a respawn; the schedule and attachment changes have no check of their
own yet. A second whole-tree review the same night (33 confirmed) had a scene end inside the game's tick however
it was ended, dropped the retry members of `@Scene`, and set the plan right where it had drifted from the code. A third
review (five confirmed) opened the loot sub-providers' registries to contributors, made every data run start clean,
and left the dry-run notes below. A fourth found nothing; a fifth (22 confirmed) made the flag setting a toggle on
the config screen, had the architecture test demand every `register` call from the root, and pinned a dozen
behaviours the tests had left loose; what it left is W14 to W19. A sixth (19 confirmed) left W20 to W23.

The `ledger` composite was built and reviewed last, at the end of the budget: a `Ledger.Book` per module naming the statistics
it may award, server-wide saved data for the awards of absent players, granted at their next login; one isolated
scene joins a player under a chosen id, which `TestScene.addPlayer(name, id)` now allows. Every piece of `result.md` is built as of 2026-10-27. The `inventory` composite is built as of 2026-10-27 (`inventory.md`, both steps). The `station` composite is built as of 2026-10-28 in its first step, the server side (`station.md`); the screen and button are the second step. Not built, in the order
they should come: the shared systems `integration`, `worldgen`, `effect` and `economy`, each a day's work of its own with the old tree as
reference. Everything above is committed as of 2026-10-27; the author commits.

## Dry-run notes, 2026-10-24

The third review of the night walked the old war horn, deepslate compass, strongbox and Muffhorn through the built
modules at call level. What it settled, for whoever builds the first feature:

- A system's keys (`SoundKey`, `ConfigKey`, `AttachmentKey`, `Cooldown.Key`) are fields of the system, declared in
  its constructor; `LanguageKey` constants, which need no declaration, sit in a `@Value` closed set of the
  system, and no `@Value` holds a `@Handle`. A handle a record must name before the system exists (a `RecipeKey`
  for `Recipe.Custom.key()`, a `YieldKinds` for a yield codec) is a constant of the system's `@Keys` set, the
  stereotype settled 2026-10-27; the constructor declares a `RecipeKey` to `Recipe`, and the root declares the
  mod's one `YieldKinds` to `Result`, reading it from the set.
- Datagen seams are added with their first consumer, as the advancement and loot-modifier providers were: chest
  loot tables (the strongbox's fortress tables: a `Loot.Chests` sub-provider on `LootContextParamSets.CHEST` and a
  `chestLoot` method on `LootContributor`), tags of other registries (the compass's structure tags: a method on
  `TagContributor`), and the block loot sub-provider's remaining protected helpers (`hasSilkTouch`; fortune tables
  go through the `registries()` the sub-providers now expose).
- `Feedback` carries a `LanguageKey` line and the mod's own `SoundKey`; a styled component or a vanilla sound is
  the caller's own call, or an overload to add with its first consumer.
- An item's use cooldown (the war horn) is the game's `ItemCooldowns` on the player; `Cooldown` is for entity state
  that survives a save.
- `SoundKey.play(entity, …)` is positional; a sound that follows a moving entity (the game's entity-bound sound
  packet) is an overload to add with its first consumer.
- The menu, slot and block-entity bases the strongbox, lapidary and mortar share are the `station` composite
  over `menu` (`design/station.md`).
- The game blocks the use of an item under `ItemCooldowns` itself, so no "not ready" line can be shown for it; a
  feature that wants the line uses `Cooldown` and checks before the use.
- A registered payload has no size cap (the game's 32767-byte fallback applies only to unregistered ones), so a
  serverbound codec bounds its own lists and strings.
- A scene for a system that declares keys needs the real system, since a second instance would register its keys
  twice; how the game-test root reaches the mod's own modules without a static is undecided, and is decided with
  the first feature scene.

## Order and budget

Primitives in the table order, then composites in theirs, then shared systems in theirs. Every module gets an
independent review before the next starts; the last of the budget goes to whole-tree reviews.
