# JolCraft

Minecraft mod for **NeoForge 1.21.1** (`neo_version` 21.1.243), Java 21, Parchment mappings, built with ModDevGradle. Mod id `jolcraft`, root package `net.sievert.jolcraft`. Dwarven professions, trading, reputation, brewing, bounties, structures and effects. About 830 Java files.

## Commands

```bash
./gradlew compileJava compileGametestJava   # fast correctness check (a few minutes)
./gradlew runData                           # regenerate src/generated/resources (REQUIRED before build)
./gradlew runGameTestServer                 # headless game tests (what CI runs)
./gradlew runClient                         # dev client; runClient2 starts a second player "Dev2"
./gradlew build                             # jar + verifyModJar (fails if generated resources are missing)
```

CI (`.github/workflows/build.yml`) runs `runData`, then `runGameTestServer`, then `build`. Gradle uses the configuration cache and 8 GB heap.

## Resources are datagen output

- `src/generated/resources` is **gitignored** and produced by the `datagen` package (entry point `datagen/JolCraftDataGenerator.java`, which registers every provider on `GatherDataEvent`). Never hand-edit a generated JSON; change the provider and rerun `runData`.
- `src/main/resources` holds only hand-authored assets: textures, sounds (`.ogg`), structure NBTs, `jolcraft.mixins.json`, `META-INF/` (empty `accesstransformer.cfg`, `enumextensions.json`, the JEI `IModPlugin` service file).
- `src/main/templates/META-INF/neoforge.mods.toml` is expanded by the `generateModMetadata` task from `gradle.properties` and `integrations.properties`.
- A tag that is declared in `data/JolCraftTags.java` is empty unless a tag provider fills it. Code that reads a tag (repair ingredients, loot, recipes) silently gets nothing otherwise.

## Where things live

| Package under `net.sievert.jolcraft` | Contents |
|---|---|
| `JolCraft.java` | Mod constructor: every `DeferredRegister` is registered to the mod bus here. A new `JolCraft*` registry holder must be added to this list. |
| `data/id/**` | All id strings (`JolCraftIds`, one class per registry kind). Never write a raw `"jolcraft:..."` string elsewhere. |
| `data/language/` | `JolCraftLanguageKeys` (every translation key) and `JolCraftDictionary` (word constants keys and ids are built from). No raw `Component.translatable("...")` literals. |
| `data/JolCraftTags.java`, `data/JolCraftColors.java` | Tag keys and centralised colours. |
| `world/**` | Runtime game content: `entity` (dwarves, Muffhorn, effects, attachments, player data), `item`, `block` (block entities, fluids, brewing), `recipe` (custom recipe framework: `base/{input,output,condition}` plus `custom/*`), `worldgen`, `gui` (menus, screens, slots), `loot`, `sound`. |
| `world/entity/custom/dwarf/` | `base` (abstract entities), `ai/goal`, `interaction` (handler pipeline with gates), `action` (multi-tick transactions with interrupted-action recovery), `trade`, `profession`, `variant`, `loadout`. |
| `event/` | Event handlers, split by bus and side: `event/mod` (mod bus), `event/game/**` (game bus), `event/client/**` (client only). Helpers are suffixed `*EventsHelper`. |
| `network/` | Custom payloads (`packet/s2c`, `packet/c2s`) and proxies. |
| `config/` | `JolCraftCommonConfig` (ModConfigSpec) and data-driven JSON config managers under `config/base` and `config/custom` (dwarf professions, brewing corruption). |
| `datagen/` | Providers mirroring the runtime packages: `recipe/builder` and `recipe/subprovider/**`, `loot/table` and `loot/glm`, `tag/provider`, `client/{language,model,sound,atlas}`, `config`, `structure`, `advancement`. |
| `integration/` | JEI, Jade and JER. These APIs are `compileOnly`; the mods are optional, so nothing outside this package may reference it without a `ModList` guard. |
| `mixin/` | 36 mixins, all listed in `src/main/resources/jolcraft.mixins.json` with `defaultRequire: 1` (a failed injection crashes at startup). |
| `util/log/` | `JolCraftLogs` + `JolCraftLogTags`. Use these, not a raw logger. |

Client-only code lives in packages or classes named `client` (`world/gui/client`, `world/entity/client`, `world/item/client`, `event/client`). Common code must not import `net.minecraft.client.*` or call into those packages without a `Dist` guard; the mod must run on a dedicated server.

## Tests

- Game tests are a separate source set, `src/gametest/java`, with their own mixin config `src/gametest/resources/jolcraft.gametest.mixins.json` (included in `mods.toml` only when a task name contains `gametest`). Test classes are listed in `TEST_HOLDERS` in `gametest/JolCraftGameTests.java`; a test that is not in that array does not run.
- `src/test` is empty; JUnit is wired in `build.gradle` but unused.
- `ExperimentalTests` is a scratch holder. Do not put real regressions there.

## Verifying framework behaviour

The decompiled NeoForge + Minecraft sources for the exact version are at `build/moddev/artifacts/neoforge-21.1.243-sources.jar` (after any Gradle run). Check vanilla semantics there instead of guessing, for example:

```bash
unzip -p build/moddev/artifacts/neoforge-21.1.243-sources.jar net/minecraft/world/entity/LivingEntity.java | grep -n "addEffect"
```

## Review backlog

`REVIEW_TODO.md` in the repo root lists every verified finding from the 2026-10-17 repository review as numbered items (R1, R2, ...) plus the author's roadmap. Pick items from there; tick them off when fixed.
