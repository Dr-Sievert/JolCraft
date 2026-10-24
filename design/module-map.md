# JolCraft — Module Map

How the 26.1.2 code is organised. Feature behaviour will live in `design/feature-plan.md` once the 1.0 list is set.
The model is the one champions uses, carried over where a mod can keep it and changed where NeoForge cannot.

## Direction

Modular means clean separation: each part owns one job and talks through interfaces. There is no runtime
module system and no enable/disable machinery. Everything is wired once, by hand, in the composition root.

**Primitives** are the bottom tier. Each is self-contained and knows nothing about JolCraft and nothing
about the other primitives. A primitive is an interface that declares an **id** and nothing more, with one
implementation. Primitives keep no JolCraft-specific code, so another mod could lift them whole.

**Composites** are the middle tier. A composite is built from primitives only, never from other composites.
Its constructor takes exactly the primitives it uses.

**Systems** are the top tier and the features: dwarves, professions, trading, reputation, brewing,
bounties, structures. A system takes primitives, composites and other systems. Strictness decreases going
up and the top is unrestricted; there is never a reason for a fourth tier.

One `Module` interface at every tier, exposing `id()`, read from the tier annotation. Nothing else.

## Packages

One Gradle project, `net.sievert.jolcraft`, feature-first inside each tier. No java / game / client axis:
a module that has client code keeps it in its own `client` sub-package.

```
net.sievert.jolcraft
  JolCraft                          composition root: the @Mod class, wires every module in its constructor
  module/                           Module, @Primitive, @Composite, @SystemTier
  stereotype/                       the stereotype annotations (below)
  util/                             below every tier, JDK only; util/text holds Dictionary and ModString
  module/primitive/<id>/            one primitive per folder, id = folder name: log, registry, config,
                                    network, language, sound, attachment, ...
  module/primitive/<id>/client/     its client-only part, if any
  module/composite/<primitive>/     composites, in the folder of the primitive they are organised under
  module/system/<feature>/          one feature per folder, id = folder name: dwarf, trade, brew, bounty, ...
  module/system/<feature>/client/   screens, renderers, client events, client payload handling
  mixin/                            every mixin, outside the tiers; a mixin only posts events
```

A composite or a system that spans several files keeps them in its folder; a folder may hold several modules.

## What is a module and what is not

NeoForge wants some things static: registry holders, payload types, codecs, attachment types, mixins.
These are **declarations**, not modules, and the tier rules do not see them as modules:

- **Content** (`@Content`): a module folder's registry declarations. A final class with a private
  constructor whose public static final fields are `DeferredRegister`s and `DeferredHolder`s, plus one
  `register(IEventBus)`. It may depend on vanilla, NeoForge and its own folder's values, never on a module.
- **Values** (`@Value`): records, enums, closed sets, codecs, payload records, attachment types.
- **Events** (`@Event`): JolCraft's own `Event` subclasses on the NeoForge bus, the way the mod's parts talk
  to each other without holding each other: an actor or a mixin announces, a system listens. An event that
  needs an answer carries a result or a cancel flag, as NeoForge's own events do.
- **Actors** (`@Actor`): the classes the game creates and calls: entities, block entities, menus, goals,
  blocks, items. Public, extend a game type, hold no module and no mutable static state. An actor reads data
  (recipes, datapack registries, attachments, saved data) and posts events; it never asks a system anything,
  so it has no way to reach one and needs none.

Behaviour is a module and is an instance. Event handling is instance methods on modules, registered by
the composition root (`bus.addListener(system::onX)`); there are no `@EventBusSubscriber` classes.
Payload handlers are module methods the root registers at `RegisterPayloadHandlersEvent`.

**Systems drive actors; actors never drive systems.** A system gives an entity its behaviour when the entity
joins a server level (`EntityJoinLevelEvent`): it adds goals built with the system's own dependencies through
their constructors, and it handles the interaction and tick events of that entity kind. The entity stays thin.
A block entity or a block runs its own mechanics from data in its tick and announces what happened. Menus are
opened by our code, so the server-side constructor takes the system it needs and the `MenuType` builds the
client copy without it, as vanilla's merchant menu does. Commands, payload handlers, capability providers and
reload listeners are lambdas the root registers, capturing the systems directly. Anything that is really data
(profession tables, trade tables, corruption rules) is a datapack registry or a data map, readable on both sides
through `level.registryAccess()` and synced by the game; per-world mutable state is `SavedData` or an
attachment, so no system holds world state and nothing resets between worlds.

## Composition root

`JolCraft`'s constructor builds every primitive, composite and system, registers each folder's `Content`,
adds the listeners and payload handlers, and, when `FMLEnvironment.dist` is the client, calls each
module's client bootstrap. No static field holds a module and no static method outside the root returns one,
so nothing reaches a module through a locator; a module gets what it needs through its constructor.

## Client code

- Everything that imports `net.minecraft.client`, `net.neoforged.neoforge.client` or `com.mojang.blaze3d`
  sits in a package named `client`. Nothing else may import them, so the mod runs on a dedicated server.
- Nothing outside a `client` package depends on a class inside one, except the composition root, which may
  call a client bootstrap: a `@Helper` class in the module's `client` package with static entry points whose
  signatures use only common types, invoked after a dist check. That keeps client classes unloaded on a server
  and gives the module its proxies the way the old network proxies worked.

## Mixins

Mixins are outside the tiers, in `mixin`, listed in `jolcraft.mixins.json`, and are not stereotyped. A mixin
holds no reference to anything of ours: it posts an event, exactly as NeoForge's own patches do, and a system
listens. Mixins stay few; a NeoForge event is preferred wherever one exists.

## Stereotypes

Every top-level class in a primitive or composite folder carries exactly one annotation: a tier annotation or
one stereotype. System folders are unrestricted, but a class that carries one is held to its rule.

| Stereotype | Rule |
|---|---|
| `@Implementation` | public, implements its folder's primitive, extends no game type, used only by a composition root: the mod's `JolCraft`, or the dev mods' own entry classes `JolCraftGameTests` and `JolCraftDatagen`, which are roots of their own mods |
| `@Value` | record, enum or closed set; depends on no module |
| `@Handle` | interface whose every implementor is nested inside its own folder |
| `@Contract` | open interface callers implement |
| `@Helper` | final, private constructor, static members only |
| `@Event` | extends NeoForge's `Event`; sits in a composite, a system or the event primitive |
| `@Bridge` | the only class that may import an optional mod's API (JEI, Jade); behind a `ModList` guard |
| `@Internal` | package-private, used only inside its folder: the goals a system builds, helpers |
| `@Content` | new, mod only: the registry declarations described above |
| `@Actor` | new, mod only: a public class the game creates, extending a game type; holds no module and no mutable static state; reads data and posts events |

## Names, ids and text

A word used in an id, a key, a path or a tag is never a string literal: it is a constant in `Dictionary`
(`util/text`), one lowercase word per constant. Longer strings are built with `ModString` (`dotted`,
`underscored`, `slashed`, `dashed`, `spaced`, `tag`). Every `Identifier` of the mod is built from Dictionary words in one
place per kind, as `JolCraftIds` did; every translation key likewise, so one name runs from the registry to the
language file. A module's id is one Dictionary word, unique across the tiers. A primitive's or a system's id is its folder
name; a composite sits in a primitive's folder and keeps an id of its own. The rule covers module code: test
code, the dev mods' own plumbing (a locale code such as `en_us`, a separator) and log, assertion and exception
messages are outside it.

## Naming

A package and a module type are one singular, neutral noun: `trade` and `Trade`, not trading; `log`, not logging.
A type takes the `Mod` prefix only when its noun is a JDK or Minecraft type every implementation needs
(`ModString`), never a plural to dodge the clash. A primitive's or a system's id is its folder name, a composite's is its own word; a primitive's
implementation is `Server` plus the noun (`ServerLog`), the one the mod runs with.

## Logging

A `Log` primitive, whose interface is JDK-only and whose `ServerLog` implements it over SLF4J, is handed to
every module, which tags it with itself and logs through
it, `[TRADE] [OFFER] message`. No module takes a raw logger. Every system and composite logs at three levels:
info for what the player or the server owner should know happened, warn or error for every problem, debug for
the detail someone chasing a problem needs.

## Source sets and tests

| Source set | Holds | Ships |
|---|---|---|
| `main` | the mod | yes |
| `test` | JUnit: the architecture test, tests of JDK-only modules, and tests that need game classes without a world (codecs, registries), run under ModDevGradle's FML-bootstrapped unit tests | no |
| `gametest` | the dev-only mod `jolcraft_gametest`: scenes and the scene adapter | no |
| `datagen` | the dev-only mod `jolcraft_datagen`: every data provider; `runData` loads it beside the mod | no |

**Datagen.** The dev-only mod `jolcraft_datagen`, loaded by `runData` beside the mod; nothing of it ships. One
small contributor per module and kind (`TradeRecipes`, `TradeLang`, `TradeModels`), found by the scan like scenes,
in folders that mirror the module tree. Datagen walks the registries and demands completeness: every registered
thing has what its kind requires (an item a model and a name, a block a blockstate, a model, a loot table and a
name, a sound a definition, and so on) and `runData` fails on anything missing; a hand-made block state, item definition or loot table is
exempted explicitly, by name and per registry in `Handmade`, and every exemption is checked against its file and
its registry entry when datagen runs; names and sound definitions have no exemption, those files are generated whole. Validation, the exemption list and the common helpers live once in the datagen core; a
contributor only supplies content, with vanilla's builders available for the rare case. Keys live in main next to
the content; the language contributor in datagen holds the text, every language the same way. Datapack-registry
content (professions, trade tables, corruption rules) goes through the same contributor path, created first and under the mod's namespace so every other provider can
resolve it. A contributor names its module's folder in `@Contributes` and sits in `datagen.module.<tier>.<folder>`.

**Architecture test.** `ArchitectureTest` in `test`, over ArchUnit, fails `build` on a broken layer. The rules:

1. a primitive depends on no other primitive;
2. a primitive depends on nothing JolCraft-specific outside `util` and the primitives;
3. a composite depends on no other composite and on no system;
4. a composite or system depends only on the primitives its constructor takes, and so does the rest of its folder;
5. no static field holds a module and no static method outside the root returns one; nothing below the root
   depends on the root; there is no exception;
6. `util` depends on the JDK alone;
7. tier annotations sit on modules; each primitive folder declares one primitive whose id is the folder name;
   a composite sits in a primitive's folder; every id is one Dictionary word, unique across tiers; no module
   declares `id()` itself;
8. an `@Actor` and a mixin depend on no module; nothing outside the root reaches a server through
   `ServerLifecycleHooks.getCurrentServer` or `Minecraft.getSingleplayerServer`;
9. the client rule and the stereotype rules above;
10. every class that declares a scene sits in `gametest.module.<tier>.<folder>` of an existing module (the adapter's
    own `ExampleScenes` excepted), and every datagen contributor in `datagen.module.<tier>.<folder>` of one. The test
    first asserts that its import holds the mod, so no rule passes on an empty import.

**Game scenes.** A test is a method annotated `@Scene` on a class in `gametest`, taking a `TestScene` that
wraps `GameTestHelper` (arena blocks, ticks, named checks that log what was verified, players). The adapter finds every `@Scene` through NeoForge's
class-scan data (`ModFileScanData.getAnnotatedBy(Scene.class, METHOD)`), registers one test function per method
in `TEST_FUNCTION` and one instance per method at `RegisterGameTestsEvent`, using vanilla's empty structure or a
template the annotation names. No JSON, no central list: a scene exists once it is written. Scene classes mirror
the module they test, `gametest/module/<tier>/<folder>/<Name>Scenes.java`, and a scene's id is its place,
`system/trade/trade_scenes/offers_match`, so ids cannot collide and rule 10 ties every scene to a module. Finding
no scene at all fails the run. A scene's shape and values are checked when it is found, not when it runs. A scene
that adds players is `isolated`: it runs in a batch of its own, so no other scene's players, chat or packets reach it.

**Players.** `scene.addPlayer(name)` returns a `TestPlayer`: a real `ServerPlayer` placed in the level through
vanilla's mock player, with command and interaction helpers and `received()`, the packets and payloads sent to
it, captured by one test-only mixin on the packet listener's `send`. A player records from the moment it exists.
The scene owns its players and recordings: when it ends, pass or fail, it disconnects every player it created and
drops their recordings, so no test writes a `try/finally` and nothing leaks between scenes.

**Dividing line.** Asserted in one tick with no world: a unit test. Needs ticks, blocks, entities or players: a scene.

## What is decided and what is open

Decided (2026-10-24): the three tiers and their rules; feature-first packages with `client` sub-packages;
one Gradle project with the four source sets; annotated scenes; actors read data and post events while systems
drive them, with no static seam of any kind (a hook table was proposed, reviewed and rejected as a service
locator); datagen as mirrored contributors with registry-driven completeness checks and text in datagen; scenes
own their players and recordings. Nothing is left open up front; the stereotype of a new kind of class is decided
when its first instance is written.
