# JolCraft

A Minecraft mod for **NeoForge 1.21.1**: dwarven professions, trading, reputation,
brewing, bounties, structures and effects.

## Requirements

- Java 21
- Gradle wrapper (included); the build uses NeoForge `21.1.243` with Parchment mappings

## Building

`src/generated/resources` is datagen output and is gitignored, so the data generator has
to run before anything that packages or runs the mod:

```bash
./gradlew runData        # regenerate src/generated/resources (required first)
./gradlew build          # jar + verifyModJar (fails if generated resources are missing)
```

The jar ends up in `build/libs/`.

## Development

```bash
./gradlew runClient                 # dev client (runClient2 starts a second player)
./gradlew runServer                 # dev server
./gradlew runGameTestServer         # headless game tests, the same step CI runs
./gradlew runData -Pjolcraft.datagen.dev=true   # also emits the dev-only test data
```

Resources under `src/generated/resources` are produced by the providers in the
`datagen` package; change a provider and rerun `runData` instead of editing the JSON.
Hand-authored assets (textures, sounds, structure NBTs, mixin configs) live in
`src/main/resources`.

CI (`.github/workflows/build.yml`) runs `runData`, then `runGameTestServer`, then `build`.

## License

MIT, see [LICENSE](LICENSE).
