# Skeleton review, 2026-10-24

Independent review of the 26.1.2 module skeleton: five finders (build, architecture test, game tests, datagen, docs), each finding then
challenged by a second reviewer against the code and the 26.1 sources. 34 findings survived, the reviewers added 11, 8 were refuted
(listed at the end). Overlapping findings are merged below into 33 items, most severe first. `- [ ]` is open; tick when fixed.

Format per item: Problem / Impact / Fix / Decision.

**Status 2026-10-24:** all 33 fixed and verified (`build` with the 20 architecture rules, `runData`, `runGameTestServer`); the
Decision line of an item says where the fix differs from the proposal.

## High

- [x] **S1. The test task does not see the class directories it checks.** `build.gradle` passes the main and gametest class dirs to the
  test task only as path strings, so Gradle treats the strings as the input, not the classes. With `org.gradle.caching=true` a
  gametest-only change leaves `test` UP-TO-DATE or FROM-CACHE (reproduced: new scene class, `:test UP-TO-DATE`). Impact: rule 10 and any
  rule over main can silently not run. Fix: a `CommandLineArgumentProvider` with `@InputFiles @PathSensitive(RELATIVE)` over both
  `classesDirs`, added via `jvmArgumentProviders`, replacing the two `systemProperty` lines. Decision:

- [x] **S2. Rule 10 checks only `gametest.module..`, and the map tells authors a different path.** A scene class anywhere else in the
  gametest source set (`gametest.trade`, or the map's own `gametest/<tier>/<folder>/`) is never checked; scene ids use the class's simple
  name only, so two `TradeScenes` in different tiers collide. Impact: false confidence in the one rule that ties tests to modules. Fix:
  select every top-level class that declares a `@Scene` method, require `gametest.module.<tier>.<folder>` for all but the adapter's own
  `ExampleScenes`, build the default id from `<tier>/<folder>/<method>`, correct `design/module-map.md` line 181. Decision:

- [x] **S3. Actor, value and content checks look at the annotated class only, and the bare `Module` type is allowed.** A nested, inner or
  anonymous class of an `@Actor` can hold or call a module unseen; a static field or return type declared as plain `Module` passes
  rules 5 and 8. Impact: the two rules that keep static seams out can be walked around without noticing. Fix: run `checkActor`,
  `checkValue`, `checkContent` and rule 8 over the whole unit (`owner(type)`), and make `isModule` include `Module` itself for fields
  and return types. Decision:

- [x] **S4. Root exemptions are granted to the package `net.sievert.jolcraft`, not to `JolCraft`.** Any class placed in the root package may
  return modules from static methods, call `ServerLifecycleHooks.getCurrentServer`, build implementations and depend on `JolCraft`.
  Impact: rule 5 has a hole the map says it must not have. Fix: exempt `JolCraft.class` by name everywhere the test says `ROOT`, and
  add a rule that `JolCraft` is the only top-level class in the root package. Decision:

- [x] **S5. Datapack-registry entries are filtered by the wrong namespace and created last.** `createDatapackRegistryObjects(builder)`
  filters by the event's mod id, `jolcraft_datagen`, so every `jolcraft` entry a `RegistryContributor` adds is dropped; and it runs
  after the other providers, which keep the plain vanilla lookup and can never resolve a profession or trade table. Impact: the data
  path item 1 relies on does not work. Fix: build the `RegistrySetBuilder` first, call `createDatapackRegistryObjects(registries,
  Set.of(JolCraft.MOD_ID))`, then create the other providers. Decision:

- [x] **S6. Zero discovered scenes passes the game-test server.** If the scan finds nothing (annotation renamed, source set
  mis-wired), `runGameTestServer` exits 0 with one info line. Impact: the CI gate can go green with no test run. Fix:
  `SceneRegistry.register` throws when `entries` is empty; `ExampleScenes` always ships, so empty means broken. Decision:

## Medium

- [x] **S7. Nothing proves the architecture test imported anything.** With the property absent (IDE JUnit runner) or pointing at an empty
  directory, the fallback can import zero classes and every rule passes vacuously; `allowEmptyShould` hides it. Fix: assert `CLASSES`
  contains `JolCraft`, `Log` and `ModLog`; fail, do not fall back, when the property is set but resolves to nothing. Decision:

- [x] **S8. `@Content` rejects NeoForge's own holder types.** The check matches simple names starting with `DeferredRegister` or
  `DeferredHolder`; `DeferredItem`, `DeferredBlock`, `DeferredRegister.Items` fail. Fix: `isAssignableTo` the two base classes by
  name; require `register` by its `IEventBus` parameter type. Decision:

- [x] **S9. Stereotype checks are looser than the table.** `@Actor` "extends a game type" is only "superclass is not Object";
  "no mutable static state" only forbids non-final statics (a `static final List` passes); the event primitive match is a prefix.
  Fix: require the superclass chain to reach `net.minecraft..` or `net.neoforged..`; require static final fields to be primitives,
  `String`, records, enums or holder types; match the event folder exactly. Decision:

- [x] **S10. Primitive folders and implementations are not counted.** A folder under `module.primitive` with no `@Primitive` is not
  reported; "one implementation per primitive" is unchecked, so a second `Log` implementor in a system folder escapes every
  implementation rule. Fix: take the folder set from all classes under `module.primitive..`; for each primitive require exactly one
  concrete implementor, `@Implementation`, in its folder. Decision:

- [x] **S11. `build` and `runClient` succeed without generated resources.** `src/generated` is gitignored and nothing checks it exists; a
  fresh clone's `build` makes a jar with no data. Fix: a small verify task on `check` that fails when `src/generated/resources/.cache`
  is missing (`runData` always writes it), and a sentence in CLAUDE.md that `runData` precedes `build` and `runClient`. Decision: the guard runs on `check` and before `runClient`, `runClient2`, `runServer` and `runGameTestServer`, never before `runData`.
- [x] **S12. `Handmade` exemptions are never checked against reality.** An exempted id only leaves the walk; nothing checks the hand-made
  file exists in `src/main/resources` or that the id is still registered, so a typo or stale id passes. Fix: resolve each exemption
  through the event's resource manager (it holds `--existing`) and fail on a missing file or unknown id. Decision: names and sound definitions have no exemption (their files are generated whole); block and item models and block and entity loot are exempt per registry and verified against file and registry.
- [x] **S13. The name walk covers four registries; the map says every registered thing.** Creative tabs, attributes and anything else with a
  translatable name are not walked, and `Translations.add(Holder, String)` throws for them. Fix: walk every registry the mod registers
  into (add creative tabs now), give `add(Holder)` the matching branches; correct the CLAUDE.md sentence on entity loot (every entity
  type without `noLootTable()` needs a table, an empty one is fine). Decision:

- [x] **S14. Item tags cannot copy block tags.** `createBlockAndItemTags` hands the item provider the block-tag lookup; `JolCraftDatagen`
  drops it. Fix: `ItemTags` extends `BlockTagCopyingItemTagProvider`, receives the lookup, and `TagContributor` gets a `copies`
  hook. Decision:

- [x] **S15. Recipe and loot contributors cannot reach vanilla's protected helpers.** `Recipes` hands contributors only `RecipeOutput` and
  registries; `has(...)`, `shaped`, `shapeless`, the items getter and most `BlockLootSubProvider` helpers are protected. Fix: a named
  public `RecipeProvider` subclass that opens them and is passed to `recipes(...)`; open the needed loot helpers in `Loot.Blocks` the
  same way. Decision:

- [x] **S16. The docs overstate the root.** CLAUDE.md says `JolCraft` "holds the only `ModLog`"; the dev mods construct two more, and
  the `@Implementation` "used only by the root" rule is checked over main only. Fix: the map names the dev-mod entry classes as
  composition roots of their own and says ArchitectureTest checks main (plus rule 10); reword CLAUDE.md. Decision:

- [x] **S17. Scenes in one batch share the player list.** Up to 50 scenes run in one level; every other scene's player join or leave sends
  chat and player-info packets to this scene's players, so "any `ClientboundSystemChatPacket`" can pass for the wrong reason. Fix:
  `received()` filters on content for the example, and scenes that add players get an environment of their own (a `Scene` flag) so
  they run alone. Decision: content-based chat check in the example; scenes that add players set `isolated = true` and run in a batch of their own.
- [x] **S18. An `Error` from a scene crashes the server tick.** `run()` rethrows `Error`; `GameTestInfo` catches only exceptions, so an
  `AssertionError` or `NoClassDefFoundError` becomes a crash report and a dead batch; `end()` also catches only `RuntimeException`,
  so one failing `onEnd` skips player cleanup. Fix: wrap `Throwable` into `IllegalStateException` in `run()` and in the tick helpers;
  catch `Throwable` in both `end()` loops with the player cleanup in a `finally`. Decision:

- [x] **S19. Bundled packets are recorded as one bundle.** Entity spawn and tracking arrive as `ClientboundBundlePacket`, so
  `received(ClientboundAddEntityPacket.class)` is empty. Fix: `PacketRecorder.record` flattens a bundle's `subPackets()`. Decision:

- [x] **S20. `TestPlayer` is placed at spawn, never ticked, and never receives chunks.** `setPos` after `placeNewPlayer` moves the entity
  without a teleport; the player's chunk batches are never acknowledged so arena chunks stay pending and no entity is ever tracked to
  it; `doTick` never runs so effects never expire. Fix: `snapTo` the arena before `placeNewPlayer`; one `onEachTick` in `TestScene`
  that calls `doTick()` and `connection.chunkSender.onChunkBatchReceivedByClient(...)` per live player; `chunkSource.move(player)`
  after a reposition. Decision: placed first, then snapped to the arena: `snapTo` before placement dereferences the connection placement creates, so the reviewer's order was wrong.
- [x] **S21. Contributor placement and the map disagree on composites.** `checkPlace` accepts any tier segment and compares the second
  segment with the annotation id; the map says a module's id is its package name, which cannot hold for a composite in a primitive's
  folder. Fix: the map states that primitives and systems take their folder name as id and a composite keeps its own word;
  `checkPlace` requires a real tier and the module's folder; rule 10 also covers datagen classes. Decision: `@Contributes` names the module folder; the tier must be real; the architecture test checks the module exists.
## Low

- [x] **S22. Rule 9 lets the root depend on any client class.** The map allows only a `@Helper` bootstrap with common-type signatures. Fix:
  require that each `..client..` class `JolCraft` depends on is a `@Helper` whose public static signatures involve no client type. Decision:

- [x] **S23. The integrations reader is lenient.** A key with no dot throws a bare `StringIndexOutOfBounds`; unknown keys and an artifact
  with `.configuration` but no `.dependency` are ignored; `side` is unchecked. Fix: whitelist the key shapes, require both artifact
  keys, check `side`, name the file in every error. Decision:

- [x] **S24. CI has no timeout, no failure artifacts, no permissions or concurrency.** Fix: `timeout-minutes`, `permissions: contents:
  read`, a concurrency group, and `upload-artifact` of `build/reports`, `build/test-results` and `run/*/logs` on failure; drop the
  unused `fetch-depth`. Restricting push to `main` is a policy choice. Decision: timeout, read-only permissions, concurrency group and failure artifacts added; the push trigger stays for all branches, a policy call.
- [x] **S25. The dev source sets inherit `localRuntime` into compile scope.** `compileClasspath += runtimeClasspath` puts the JEI and Jade
  jars on the gametest and datagen compile classpath. Fix: `neoForge.addModdingDependenciesTo(sourceSets.gametest)` and datagen, the
  ModDevGradle way, instead of the manual `+=`. Decision: done with `addModdingDependenciesTo`; `runtimeClasspath.extendsFrom localRuntime` kept for the main runs.
- [x] **S26. Mixin config says `JAVA_21`; dev mods hardcode versions.** Fix: `compatibilityLevel` `JAVA_25` (bundled Mixin 0.17.4 supports
  it); dev mods may keep `0.1` or use `${file.jarVersion}`. Decision: compatibility level raised to JAVA_25; the dev mods keep version 0.1.
- [x] **S27. A contributor constructor that throws is reported as "needs a public no-argument constructor".** Fix: catch
  `InvocationTargetException` separately and rethrow its cause. Decision:

- [x] **S28. `Handmade` shares one id set between blocks and items, and between blocks and entities.** Exempting a block's model also drops
  its item's model. Fix: one set per registry (block model, item model, block loot, entity loot). Decision:

- [x] **S29. Three comments still say the gametest mod is "loaded by every run".** Data and unit-test runs do not load it. Fix: reword
  `build.gradle`, `JolCraftGameTests` and the gametest mods.toml. Decision:

- [x] **S30. The Dictionary exemptions live only in a javadoc.** The map's text rule lists no exemption for test code, dev mods, log and
  exception messages, or locale codes (`"en_us"` in `Translations`). Fix: write the exemptions into the map; add `EN_US` or exempt
  locale codes. Decision:

- [x] **S31. The map lists a `plural` operation `ModString` does not have.** Fix: list the real ones. Decision:

- [x] **S32. Scene validation is partial and late.** Non-public methods or classes, missing constructors and duplicate ids surface only
  when the scene runs; `maxTicks = 0` or `requiredSuccesses > attempts` pass unchecked. Fix: validate visibility, constructor, id
  collision and the numeric ranges at discovery, naming the method. Decision:

- [x] **S33. The map calls the Log primitive "JDK only, over SLF4J".** The interface is JDK-only; `ModLog` uses SLF4J. Fix: say so. Decision:

## Refuted by the second pass, for the record

Rule 3 and 4 only seeing `@Primitive` types (the map says declarations are not modules); `ModLog` putting the tag prefix into the
format string (tags are Dictionary words); the sound walk being bypassed by other `add` overloads (they are protected and `Sounds` is
final); a 26.3 upgrade note (maintenance, not a defect); plural provider names (the naming rule covers module types); javadoc lines
over 150 columns (no such rule is written in this repository); stale hook-seam history in the map (it records a decision); the
integrations header (already says what the reviewer asked for).


## 2026-10-24 overnight: whole-tree review, open items

Found by the last review of the night and left open on purpose; each is a decision or a day of its own.

- [ ] **W1.** Custom and datapack registries and the config file kinds have never been registered in a running game by the game-test mod; the first data-driven feature is the proof, or the game-test root declares one of each.
- [ ] **W2.** Every `Mod*` checks a key's namespace only against `minecraft`; giving each the namespace it owns (the root passes it) would make `Completeness.ours` exhaustive.
- [ ] **W3.** `Action` recovers on cancel, a lost actor or a failed step, not on a server stop; a pre-stop hook on `Schedule` driven from `ServerStoppingEvent` would run open recoveries before the save.
- [ ] **W4.** Command trees that share a root literal merge under the first-declared root, so a requirement on a later root is lost; the primitive could own the one root and take branches instead.
- [ ] **W5.** Two modules contributing to the same datapack registry would build two `RegistrySetBuilder`s; the datagen core should collect bootstraps per registry key.
- [ ] **W6.** The datagen walks have run on empty selections only, apart from the temporary proofs made by hand for config and sound; a fixture-driven test of each walk is wanted.
- [ ] **W7.** `TestPlayer` joins in the test level's game mode, creative, so damage on it is inert (it is marked client-loaded on join since 2026-10-24, as a real client reports itself); a survival mode needs W8 first, since a survival player inside the arena's barrier layer suffocates, and a respawn helper comes with it.
- [ ] **W8.** The default scene arena is the game's empty structure, a sealed barrier box; scenes spawn inside its barrier layer and get away with it; a template with room to move, or arena coordinates in `TestScene`, is wanted.
- [x] **W9.** `addPlayer` refuses a scene that is not isolated (2026-10-24).
- [ ] **W10.** The `@Actor` rule's whitelist of allowed static types will reject the constants every block, entity and menu declares; decide the rule by shape before the first actor.
- [ ] **W11.** Several architecture rules have never selected a class; a fixture package of deliberate violations would prove each can fail.
- [x] **W12.** `LootModifiers.registries()` hands contributors the lookup (2026-10-24).
- [ ] **W14.** `SceneRegistry` checks that scenes were found, not that the game ran them: a run where `RegisterGameTestsEvent` never reached it would pass with nothing executed; a `ServerStartingEvent` check against the test-instance registry would close it.
- [ ] **W15.** `AttachmentKey.update`'s one-sync guard (`getExistingDataOrNull`) is pinned by no scene; a sync count on a fresh synced holder would.
- [ ] **W16.** `SoundKey.playFor` is untested for a ranged key's raised volume and for the where-the-player-stands overload's position.
- [ ] **W17.** The data-less `MenuKey.open(player, title, server)` and the empty-buffer fallback are run by nothing.
- [ ] **W18.** `Language.plain` is tested only at its fallback; `ModConfig.build` only at one section depth; `whole` and `real` ranges are never tested.
- [ ] **W19.** `ModNetwork`'s disconnected-player guard never runs in a scene.
- [ ] **W20.** `ModConfig.get`'s quoted-boolean branch, a loaded STARTUP read, `ModLog`'s prefix and levels, and `ModNetwork`'s away-from-the-server guard have no unit test.
- [ ] **W21.** `Ledger.grant`'s stale-statistic and failing-grant branches and multi-book loop, `TriggerKey.fire(player)` and `criterion(instance)`, `ModSchedule`'s queue removal on stop, and `PacketRecorder`'s bundle unpacking are run by no scene.
- [ ] **W22.** No architecture rule keeps `LanguageKey` constants in `@Value` classes, which is all the English walk sees.
- [ ] **W23.** The adapter's own promises (reverse `onEnd`, players disconnected at the end, a failed check failing the scene, an Error becoming a failure, `isolated` batches) are proven by no scene.
- [ ] **W13.** A sound definition's audio file, a hand-made `.ogg` under `src/main/resources/assets/jolcraft/sounds`, is not checked to exist by `runData`.

## 2026-10-24 overnight: second whole-tree review, 33 confirmed, all applied

Behaviour: a scene's `succeed()` goes through a one-step sequence so the game's pass listener fires however the
scene was ended, and a `succeedWhen` condition that throws is the scene's failure (X1); `@Scene` lost `attempts`
and `requiredSuccesses`, which the game-test server never judged (X2); `ModLoot.declare` checks the id before the
codec (X3); `ModRegistration` refuses a double declaration and the game's namespace (X4); the generated-resources
guard asks for a completed run, not an empty cache directory, and runData clears its output first so a failed run
leaves nothing that passes it (X5, tightened by the third review). Tests: the loot id guard, the schedule thread guard, the loot roll, the attachment update, the
action's second step and its recovery message are each pinned on their own (X6). Docs: the attachment sync claim,
the map's menu, logging, architecture-test and player paragraphs, the `@Event` placement, the datapack registry
directory, the command merge rule, the loot contributor, the language key conventions, and the plan's
signatures, `ledger`, the new `outcome` shared system, the `flora` feature system and the tag and data-map rule (X7).

## 2026-10-24 overnight: third review, 5 confirmed of 29, all applied

Regressions in the night's fixes, documentation consistency, four call-level dry runs (war horn, deepslate compass,
strongbox, Muffhorn) and a datagen dry run. Applied: the block and entity loot sub-providers expose `registries()`
(looting and fortune functions need it); runData clears `src/generated/resources` first (Y1); the plan's shared
systems have one order; the map states the root-reaches-everything rule under rule 5
and names `FMLEnvironment.getDist()`; `TestPlayer.runCommand` runs as the live player. The dry runs' design notes
are in `design/module-plan.md`.

## 2026-10-24 overnight: fourth review, none confirmed of 19; the ledger composite

Documentation sufficiency for the next modules, a hostile client against the primitives, the dev mods across run
configurations: nothing stood. The dropped items that were true but small went in anyway: the `@Value` row names
its ban on handles and implementations, the map's names paragraph points at `Ids`, the README says the server
shares `run/` with the client, the plan's definition of
"built" fits composites, three more dry-run notes, W13. With the budget left, the `ledger` composite was built
(class, unit test, holder, isolated scene, both roots, `CLAUDE.md`), the last planned composite that needed no
scouting.

## 2026-10-24 overnight: the ledger's review, 6 confirmed of 13, all applied

`award` refuses a book never declared through the ledger, naming the remedy; a grant that throws is logged and
dropped so one bad entry cannot break a login; the root's log line counts the books; the unit test pins that taking
marks the book dirty and that a decoded book starts clean; the scene checks the grant right after the join, where
it happens; the test ids use `Dictionary.BOOK`; `CLAUDE.md` no longer calls the ledger planned.

## 2026-10-24 overnight: fifth review, 22 confirmed of 38

Test vacuity per area and every public method held to its javadoc. Applied: a flag is the game's boolean setting
so the config screen can toggle it, and `ModConfig.get` reads a quoted spelling as the boolean it names (Z1); the
architecture test demands a `register` call from the root for everything that has one (Z2); scenes now pin a holder
that only read its fallback, a bystander beside the sender, the recipe's serializer and category, a cancel after
the end, the told line with its sound and the sound's place, a
shown line's arguments, a type supplier left alone at declaration, the ledger's own duplicate refusal; the loot
modifier fixture was made to apply to every table so the missing-table check means something (since 2026-10-26 a second
fixture gated on the missing table does that instead); five javadocs match their code. Left open as W14 to W19.

## 2026-10-24 overnight: sixth review, 19 confirmed of 20

A second test-vacuity pass. Applied in the last minutes of the budget: the fifth review's loot fixture change, which had landed outside the
source set, is in `src/gametest/resources`; a reordered attachment chain, a second criterion for the grant, the told
sound's source, volume and pitch, exactly one sound for a shown line, and an unattached schedule's refusal are
pinned. The rest is W20 to W23.

## 2026-10-24 morning: the economy system removed

The author: the coin, its tab and texture are a feature of the mod, not a shared module, and no feature is migrated
yet. `system/economy`, its datagen contributors, scene, test, both roots' wiring, the texture and its dictionary
words are gone; the `economy` shared system stays planned, unbuilt. What its scene alone proved (generated data
reaching the game, a tab's contents, the model, English and recipe walks on real content) is open again under W6.
Kept from its last fix: in one Gradle invocation `processResources` runs before `runData`, so a game-test run in
the same invocation reads the previous data; run `runData` on its own first.

## 2026-10-24 evening: what only IntelliJ flags

The author's editor, not javac, found two: a type variable of ours handed to a null-marked game generic
("Non-null type argument is expected") and a method type parameter hiding the record's. The game's and NeoForge's
packages are JSpecify `@NullMarked` in 26.1; the tree's answer is a use-site `@NotNull` where a type variable of
ours enters a bounded null-marked generic (`Recipe<@NotNull I>`, `Criterion<@NotNull T>`,
`BlockEntityType<@NotNull B>`, `DeferredHolder<R, @NotNull T>`), never a bound, which every user would inherit; a
nested type is annotated as `Outer.@NotNull Inner`. A sweep of the whole tree for the rest of IntelliJ's defaults
applied: `@NotNull` on the overriding signatures of `SoundKey.Spec.toString`, `Sounds.add` and `ItemTags`; nullable
returns checked before use in three tests; the deprecated `SavedDataStorage` and `RegistryFriendlyByteBuf`
constructors replaced; unused lambda parameters and a pattern variable renamed `_`; two unused imports gone; the
three composites the root builds without a holder marked as such. Two are settings in the author's IDE, not code:
`TestScene.level()` returns an `AutoCloseable`, so the resource inspection wants it ignored (or renamed
`getLevel()`); `@Scene` methods are entry points by reflection, so the unused-declaration inspection wants
`net.sievert.jolcraft.gametest.scene.Scene` listed under its entry-point annotations. Decided against for now: a
package-level `@NullMarked` for the mod, which would close the category at once but make every unannotated
reference non-null in the editor's eyes; the author's call.

## 2026-10-26: review of the current tree, 9 confirmed of 16, all applied

Eight lenses over the tree as it stands. Applied: a data-less menu open hands the client an empty buffer, as the
javadoc promised and the game does not (it passes null); the map and two javadocs give the true reason for the dev
mods' statics (the adapter instantiates scene classes through a no-argument constructor); the loot fixture gates on
the dungeon chest again and a second fixture gated on the missing table keeps the missing-table check meaningful,
so dev runs no longer see a star in every roll; the second-review section sits in its chronological place; the
remaining unused lambda parameters in the tests and the dev mods are `_`; the flag test, the trigger holder's count
and CLAUDE.md's registry sentence say what is so.
