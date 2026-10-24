# JolCraft

Ground-up rewrite of the mod for **Minecraft 26.1.2 / NeoForge 26.1.2.122**, Java 25, ModDevGradle 2.0.148, Gradle 9.8.2, Groovy build scripts. Mod id `jolcraft`, root package `net.sievert.jolcraft`. The tree holds the module skeleton and no features yet: the tier types, the stereotypes, the `log`, `language`, `config`, `network`, `attachment`, `data` and `registration` primitives, the architecture test, the game-test scene adapter and the datagen mod. The 1.21.1 tree lives in `old/` as reference. **The rules are in `design/module-map.md`; this file says where things are and how to run them.**

## Toolchain pins (2026-10-23, latest stable of everything)

| Component | Pin | Note |
|---|---|---|
| Minecraft | 26.1.2 | `minecraft_version_range=[26.1.2]` until wider support is decided |
| NeoForge | 26.1.2.122 | `neo_version`; the mods.toml requires `[${neo_version},)` |
| Java | 25 | Gradle finds the installed JDK itself; CI installs it with setup-java. No foojay plugin |
| ModDevGradle | 2.0.148 | |
| Gradle | 9.8.2 | wrapper |
| JUnit | 6.1.3 | `src/test`, run under FML's JUnit launcher (which itself targets 5.13; 6.1.3 works) |
| ArchUnit | 1.5.1 | `src/test`; its SLF4J request is excluded, Minecraft pins SLF4J strictly |
| JEI | 29.47.0.111 | declared in `integrations.properties`; its MezzConfig comes from the `net.mezzdev.config` group on BlameJared |
| Jade | 26.1.12+neoforge | declared in `integrations.properties`, from the Modrinth maven |
| Mappings | none | vanilla ships Mojang names since 26.1; Parchment publishes nothing for 26.x |
| JER | dropped | no stable 26.1.2 build |
| Mixin / MixinExtras | not pinned | bundled with NeoForge (sponge-mixin 0.17.4, mixinextras 0.5.5) |

## Commands

```bash
./gradlew compileJava compileGametestJava compileDatagenJava   # fast correctness check
./gradlew test                              # JUnit under FML: the architecture test and unit tests
./gradlew runData                           # datagen into src/generated/resources; game dir run/data; fails on incomplete data; build, runClient and runGameTestServer refuse to run without it
./gradlew runGameTestServer                 # every @Scene, headless, game dir run/gametest (what CI runs)
./gradlew runClient                         # dev client in run/; runClient2 starts a second player "Dev2" in run/client2
./gradlew build                             # test + jar in build/libs
```

CI (`.github/workflows/build.yml`) runs `runData`, then `runGameTestServer`, then `build` on JDK 25. Gradle uses the configuration cache and an 8 GB heap. `check` also compiles the gametest and datagen source sets, so a broken dev mod fails the build.

## Source sets

| Source set | Holds | Loaded by | Ships |
|---|---|---|---|
| `main` | the mod, `net.sievert.jolcraft` | every run | yes |
| `test` | `ArchitectureTest` and unit tests, JUnit with FML bootstrapped and only the main mod loaded | `test` | no |
| `gametest` | dev-only mod `jolcraft_gametest`: the scene adapter and the scenes | client, server and gameTestServer runs | no |
| `datagen` | dev-only mod `jolcraft_datagen`: the contributors and providers | the data run (`--mod jolcraft,jolcraft_datagen --flat`; without `--flat` NeoForge nests each mod's output in its own folder) | no |

Each run's `loadedMods` in `build.gradle` names exactly the mods it loads. The dev source sets get the modding dependencies from `addModdingDependenciesTo`; the integrations' jars reach their runtime classpath only, never their compile scope.

## Layout

- `JolCraft.java` — the composition root: builds every module by hand in its constructor, registers content, listeners and the primitives that register once (registration, attachment, config, network). Builds the mod's `ModLog`; the two dev mods are composition roots of their own and build theirs.
- `module/` — `Module` and the tier annotations `@Primitive`, `@Composite`, `@ModSystem`; a module's `id()` is read from its annotation. `module/primitive/<id>/`, `module/composite/<primitive>/`, `module/system/<feature>/`, each with an optional `client/` sub-package. Primitives so far: `module/primitive/log` (`Log`, `ModLog`), `module/primitive/attachment` (`Attachment`, `ModAttachment`, and the `AttachmentKey` handle: `of(id, fallback)` then `saved(mapCodec)`, `synced(streamCodec)` which goes to every player who sees the holder (the game offers no owner-only sync, so a player's own business travels as a network payload), `keptOnDeath()`; a module declares its keys while it is built, the root registers once at `RegisterEvent`, and the key reads and writes any holder with `get`, `isSet`, `set`, `update`, `reset`, or hands the game its `type()`; values are immutable, a change is a new value, and `set` syncs when the key is synced), `module/primitive/language` (`Language`, `ModLanguage`, and the `LanguageKey` value: Dictionary words joined with dots, the game's category first where the game has a convention, the mod's namespace first for its own text; `text(...)` gives the translatable component, the English lives in datagen) `module/primitive/data` (`Data`, `ModData`, and the `DataKey` handle: `of(id, fresh, codec)` for a `SavedData` the module owns, `serverWide()` for one instance in the server's storage instead of one per level; a module declares its keys while it is built, nothing registers since the game creates saved data on first ask, and the key reads with `get(level)` or `get(server)` and changes with `update(level, change)`, which marks the data dirty; an undeclared key refuses to be read), `module/primitive/config` (`Config` with its `File` kinds, `ModConfig`, and the `ConfigKey` handle: flag, whole, real, text or choice, path from Dictionary words, English comment, fallback; declared into a file while modules are built, registered once by the root, read live with `get`, which answers the fallback before the file is loaded; NeoForge's own `ModConfig` class is used only inside the implementation, by its full name; every setting and section carries the translation key `<mod id>.configuration.<path>`, `ConfigKey.translation(namespace, words)` being the one formula; the words title, button, tooltip, warning and section are refused because the screen derives keys from them, and a path may not be a section of another's; a STARTUP setting loads when the root registers the files, keeps its first value until a game restart, and reading it earlier is an error, as is reading a key never declared once the files are built; `config/client/ConfigScreen` is the client bootstrap that gives the mod NeoForge's config screen from the mod list, registered only when settings exist), `module/primitive/network` (`Network`, `ModNetwork`: a module declares each payload with its direction, `toServer(type, codec, handler)` with the handler run on the server's main thread with the sending player, `toClient(type, codec)`; the root registers once under the mod's version, so client and server on different versions refuse to connect; sending is the game's `PacketDistributor`; `network/client/ClientNetwork.handle(modBus, type, handler)` is where a module's client bootstrap gives a clientbound payload its handler, and a client without one refuses to start naming the payload) and `module/primitive/registration` (`Registration`, `ModRegistration`: custom and datapack registries declared by modules and attached to the mod bus by the root once; the `Ids` helper builds identifiers, registry keys and tag keys from Dictionary words with an explicit namespace). A primitive may not depend on another, so a primitive that must say something declares a small contract the root adapts from `Log` rather than taking `Log` itself.
- `stereotype/` — `@Implementation`, `@Value`, `@Handle`, `@Contract`, `@Helper`, `@Event`, `@Bridge`, `@Internal`, and the mod-only `@Content` (registry declarations) and `@Actor` (classes the game creates; they read data and post events, never reach a module).
- `util/text/` — `Dictionary` (every word used in an id, key or path) and `ModString` (joins them). `util` depends on the JDK alone.
- `src/test/java/.../ArchitectureTest.java` — the ten rules of the module map over ArchUnit, reading the compiled class directories (`jolcraft.main.classes`, `jolcraft.gametest.classes`, `jolcraft.datagen.classes`, set by the test task through a tracked `CommandLineArgumentProvider`, so a change in any of them re-runs the test). The test first asserts that its import holds the mod; after that a rule whose selection is still empty is allowed to be vacuous.
- `src/gametest/java/net/sievert/jolcraft/gametest/` — `JolCraftGameTests` (entry), `scene/Scene` (the annotation), `scene/SceneRegistry` (finds every `@Scene` method through `ModFileScanData.getAnnotatedBy`, registers one test function and one test instance each at `RegisterGameTestsEvent`; ids are the scene's place in snake case, `system/trade/trade_scenes/offers_match`, or `example_scenes/<method>` for the adapter's own; shape and values are validated at discovery, finding no scene fails the run, and a scene that adds players sets `isolated = true` to get a batch of its own), `scene/TestScene` (arena, ticks, named checks, `onEnd`, players; owns its cleanup through a `GameTestListener` on `helper.testInfo`), `scene/TestPlayer` (a real mock `ServerPlayer` snapped to the arena, ticked and chunk-fed by the scene; `runCommand`, `received()`, `chat()`; bundles are unpacked), `scene/PacketRecorder` fed by the one mixin in `mixin/` (config `jolcraft.gametest.mixins.json`). `ExampleScenes` are the adapter's own scenes; module scenes go under `gametest.module.<tier>.<folder>` (rule 10).
- `src/datagen/java/net/sievert/jolcraft/datagen/` — `JolCraftDatagen` (entry, listens to `GatherDataEvent.Client`), `Contributes` + `Contributors` (scan discovery; a contributor names its module's folder and sits in `datagen.module.<tier>.<folder>`), `contribute/*Contributor` (one interface per data kind: recipes, English text, models, tags, loot, sounds, data maps, datapack registries), `provider/*` (one provider per kind over NeoForge's, opening the helpers contributors need), `Completeness` (the registry walk) and `Handmade` (exemptions by name and per registry: block models, item models, block loot, entity loot, each checked against its file and its registry entry). `runData` creates the datapack-registry entries first, under the mod's namespace, then fails on any item or block without a model, any block without a loot table, any entity type vanilla expects one for (those without `noLootTable()`; an empty table is fine), any item, block, entity type, mob effect, attribute or creative tab without a name, any config setting or section without a label for the config screen (`Translations.add(ConfigKey, label)`, `Translations.section(label, words)`; the screen's title and per-file texts are written by the provider), and any sound event without a definition. Recipe contributors get `Recipes.Provider` with vanilla's helpers opened; tag contributors can copy block tags to item tags.
- `src/main/templates/META-INF/neoforge.mods.toml` — expanded by `generateModMetadata` from `gradle.properties` and the enabled integrations. No `modLoader`, `loaderVersion` or range keys in 26.1.
- `integrations.properties` — optional mod integrations (JEI, Jade): Gradle artifacts per mod and the optional `[[dependencies]]` entry. `<name>.enabled=false` drops a mod from both. Code touching an integration API is a `@Bridge` behind a `ModList` guard.
- `src/main/resources` — hand-authored assets only; `src/generated/resources` is gitignored datagen output. Never hand-edit a generated JSON.
- `old/` — the complete 1.21.1 tree, not compiled; its layout map is `old/CLAUDE.md`.

## Verifying framework behaviour

Decompiled sources exist after any Gradle run. Vanilla with NeoForge's patches: `build/moddev/artifacts/minecraft-patched-26.1.2.122-sources.jar`. NeoForge's own classes: `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.122/<hash>/neoforge-26.1.2.122-sources.jar`. FML (scan data, mod loading): the `net.neoforged.fancymodloader:loader` sources jar in the same cache. Check semantics there instead of guessing, for example:

```bash
unzip -p build/moddev/artifacts/minecraft-patched-26.1.2.122-sources.jar net/minecraft/gametest/framework/TestData.java
```

Renames and changes against the 1.21.1 code met so far: `ResourceLocation` is `Identifier`; `Entity.getServer()` is gone (go through `level().getServer()`); the packet listener's `send` takes a `ChannelFutureListener`; `GameTestHelper.testInfo` is public; game tests are registry-based (`TEST_FUNCTION` plus `RegisterGameTestsEvent`), and `neoforge.enabledGameTestNamespaces` no longer exists.

Mixins name their targets with `@Desc` and class literals (`@Inject(target = @Desc(value = "send", args = {Packet.class, ChannelFutureListener.class}), ...)`), never with a descriptor string: javac checks the types, and the Minecraft Development plugin (1.8.11) cannot resolve descriptor strings in this 26.x project and flags them.
