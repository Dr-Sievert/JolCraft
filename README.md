# JolCraft

A Minecraft mod for **NeoForge 26.1.2**: dwarven professions, trading, reputation,
brewing, bounties, structures and effects.

The mod is being rewritten from the ground up for 26.1.2 on the module model described in
`design/module-map.md`. The previous 1.21.1 code is kept in `old/` for reference only; nothing
in it is built.

## Requirements

- Java 25 (an installed JDK; Gradle finds it itself)
- Gradle wrapper (included); the build uses NeoForge `26.1.2.122` through ModDevGradle.
  No mappings are needed: vanilla ships Mojang names since 26.1.

## Building

`src/generated/resources` is datagen output and is gitignored, so run the data
generator before packaging:

```bash
./gradlew runData        # regenerate src/generated/resources; fails on incomplete data
./gradlew build          # unit tests + jar in build/libs/
```

## Development

```bash
./gradlew test                      # JUnit: the architecture test and unit tests
./gradlew runClient                 # dev client (runClient2 starts a second player "Dev2")
./gradlew runServer                 # dev server
./gradlew runGameTestServer         # headless game tests, the same step CI runs
```

Each run has its own folder under `run/`: the client uses `run/`, the second client
`run/client2`, the game tests `run/gametest` and datagen `run/data`.

Resources under `src/generated/resources` are produced by the datagen mod in `src/datagen`
(dev-only, `jolcraft_datagen`): one contributor per module and data kind, and a completeness
check that fails the run for anything a registered item, block, entity or sound still lacks.
Change the contributor and rerun `runData` instead of editing the JSON. Hand-authored assets
live in `src/main/resources`. Game tests are `@Scene` methods in `src/gametest`, loaded as the
dev-only mod `jolcraft_gametest`; neither dev mod ships in the jar.

Optional mod integrations (JEI, Jade) are declared in `integrations.properties`: the
artifacts for the dev environment and the optional dependency in the mod metadata.
Set `<name>.enabled=false` to drop one from both.

CI (`.github/workflows/build.yml`) runs `runData`, then `runGameTestServer`, then `build`
on JDK 25.

## License

MIT, see [LICENSE](LICENSE).
