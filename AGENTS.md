# JolCraft

Ground-up rewrite of the mod for **Minecraft 26.1.2 / NeoForge 26.1.2.122**, Java 25, ModDevGradle 2.0.148, Gradle 9.8.2, Groovy build scripts. Mod id `jolcraft`, root package `net.sievert.jolcraft`. The tree currently holds only the build skeleton: an entry class, the mod metadata template and a dev-only game-test mod with one smoke test. No gameplay code has been migrated yet; the 1.21.1 tree lives in `old/` as reference.

## Toolchain pins (2026-10-23, latest stable of everything)

| Component | Pin | Note |
|---|---|---|
| Minecraft | 26.1.2 | `minecraft_version_range=[26.1.2]` until wider support is decided |
| NeoForge | 26.1.2.122 | `neo_version`; the mods.toml requires `[${neo_version},)` |
| Java | 25 | Gradle finds the installed JDK itself; CI installs it with setup-java. No foojay plugin |
| ModDevGradle | 2.0.148 | |
| Gradle | 9.8.2 | wrapper |
| JEI | 29.47.0.111 | declared in `integrations.properties`; its MezzConfig comes from the `net.mezzdev.config` group on BlameJared |
| Jade | 26.1.12+neoforge | declared in `integrations.properties`, from the Modrinth maven |
| Mappings | none | vanilla ships Mojang names since 26.1; Parchment publishes nothing for 26.x |
| JER | dropped | no stable 26.1.2 build |
| Mixin / MixinExtras | not pinned | bundled with NeoForge (sponge-mixin 0.17.4, mixinextras 0.5.5) |

## Commands

```bash
./gradlew compileJava compileGametestJava   # fast correctness check
./gradlew runData                           # datagen into src/generated/resources (no providers yet); game dir run/data
./gradlew runGameTestServer                 # headless game tests, game dir run/gametest (what CI runs)
./gradlew runClient                         # dev client in run/; runClient2 starts a second player "Dev2" in run/client2
./gradlew build                             # jar in build/libs
```

CI (`.github/workflows/build.yml`) runs `runData`, then `runGameTestServer`, then `build` on JDK 25. Gradle uses the configuration cache and an 8 GB heap.

## Layout

- `src/main/java` — mod code. Only `net/sievert/jolcraft/JolCraft.java` so far.
- `integrations.properties` — optional mod integrations (JEI, Jade). Per mod: the Gradle artifacts (`compileOnly` API, `localRuntime` jar for the dev runs) and the optional `[[dependencies]]` entry that `generateModMetadata` writes into `neoforge.mods.toml`. `<name>.enabled=false` drops a mod from both. Minecraft and NeoForge are pinned in `gradle.properties`, never here. Code that touches an integration API must stay behind a `ModList` guard, because the mods are optional.
- `src/main/templates/META-INF/neoforge.mods.toml` — expanded by the `generateModMetadata` task from `gradle.properties` and the enabled integrations. In 26.1 the file has no `modLoader`, `loaderVersion`, `neo_version_range` or `loader_version_range`; the 26.1 MDK dropped them.
- `src/main/resources` — hand-authored assets only. `src/generated/resources` is gitignored datagen output; never hand-edit a generated JSON, change the provider and rerun `runData`.
- `src/gametest` — separate source set compiled against main, loaded as the dev-only mod `jolcraft_gametest` by every run and never put in the jar. Tests are registered in code in `gametest/JolCraftGameTests.java`: test functions in `BuiltInRegistries.TEST_FUNCTION` through a `DeferredRegister`, test instances through NeoForge's `RegisterGameTestsEvent` (fired from the patched `RegistryDataLoader`). A test not registered there does not run. The old `neoforge.enabledGameTestNamespaces` property no longer exists in 26.1: every registered instance runs, including vanilla's `minecraft:always_pass`.
- `old/` — the complete 1.21.1 tree (`src`, its Gradle files, `CLAUDE.md`, `AGENTS.md`, `REVIEW_TODO.md`), kept for migration reference only. Nothing in it is compiled. Its layout map is `old/CLAUDE.md`.
- Common code must not import `net.minecraft.client.*`; the mod must run on a dedicated server.

## Verifying framework behaviour

Decompiled sources exist after any Gradle run. Vanilla with NeoForge's patches: `build/moddev/artifacts/minecraft-patched-26.1.2.122-sources.jar`. NeoForge's own classes are a separate dependency; their sources are in the Gradle cache at `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.122/<hash>/neoforge-26.1.2.122-sources.jar`. Check semantics there instead of guessing, for example:

```bash
unzip -p build/moddev/artifacts/minecraft-patched-26.1.2.122-sources.jar net/minecraft/gametest/framework/TestData.java
```

Renames to expect against the 1.21.1 code: `ResourceLocation` is `Identifier` (`Identifier.fromNamespaceAndPath`, `Identifier.withDefaultNamespace`).
