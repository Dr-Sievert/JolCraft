package net.sievert.jolcraft.gametest.module.system.flora;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.flora.BulbsBlock;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraData;
import net.sievert.jolcraft.module.system.flora.FloraTags;

import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.FLOOR;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.assertDark;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.at;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.count;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.darkBox;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.fill;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.put;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.whenDark;

/**
 * The flora system's world against the real game: the configured and placed features and the biome modifiers as the
 * level loaded them, the biome tags, the single-block features placed by key, the two features of bonemeal placed
 * by key many times, and the bonemeal listener through the fixture rows of the scene pack on rooted dirt (a bonemeal
 * target the mod's own rows do not name) and through the mod's own rows with real bonemeal. The game-test level is a
 * flat world of plains. Every scene builds blocks and so runs in a batch of its own.
 */
public final class WorldScenes {

    /** Placements of a bonemeal feature in one run; the features fire on one try in four, so these are many runs of the chance. */
    private static final int MANY = 400;
    /** Applications of real bonemeal to a nylium floor. */
    private static final int APPLICATIONS = 150;
    /** The absolute Y of the grass platform of the skybell of bonemeal, above its floor of 101 and below the build height. */
    private static final int PLATFORM = 110;
    /** The floor's half width: wider than the three blocks the bonemeal features reach, so a placement beyond them would land on it. */
    private static final int REACH = 4;

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("jolcraft", path);
    }

    private static ResourceKey<PlacedFeature> placedKey(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, id(path));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, id(path));
    }

    private static boolean placeConfigured(ServerLevel level, String path, BlockPos origin) {
        ConfiguredFeature<?, ?> feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(configuredKey(path)).value();
        return feature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), origin);
    }

    private static boolean placePlaced(ServerLevel level, String path, BlockPos origin) {
        PlacedFeature feature = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(placedKey(path)).value();
        return feature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), origin);
    }

    private static boolean hasFeature(ServerLevel level, ResourceKey<Biome> biome, String placed) {
        Holder<Biome> holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome);
        return holder.value().getGenerationSettings().features().stream().anyMatch(set -> set.stream().anyMatch(feature -> feature.is(id(placed))));
    }

    private static List<String> absent(Registry<?> registry, List<String> paths) {
        return paths.stream().filter(path -> !registry.containsKey(id(path))).toList();
    }

    // ------------------------------------------------------------------ the level's data

    @Scene(maxTicks = 20, isolated = true)
    public void theLevelHoldsTheModsConfiguredAndPlacedFeaturesAndBiomeModifiers(TestScene scene) {
        ServerLevel level = scene.level();
        List<String> configured = List.of("bloodroot", "skybell", "cyanella", "duskcap", "deepslate_bulbs", "cyanella_patch", "huge_duskcap", "huge_festerling");
        List<String> placed = List.of("bloodroot", "bloodroot_dense", "cyanella_patch", "skybell_patch", "skybell_patch_dense", "duskcap_patch", "deepslate_bulbs_patch",
                "skybell_bonemeal", "cyanella_bonemeal");
        List<String> modifiers = List.of("add_bloodroot", "add_bloodroot_dense", "add_cyanella_patch", "add_skybell_patch", "add_skybell_patch_dense", "add_duskcap_patch",
                "add_deepslate_bulbs_patch");
        Registry<ConfiguredFeature<?, ?>> configuredRegistry = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        Registry<PlacedFeature> placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        Registry<?> modifierRegistry = level.registryAccess().lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        scene.check("the level's configured features hold the mod's eight", () -> scene.assertTrue(absent(configuredRegistry, configured).isEmpty(), "missing " + absent(configuredRegistry, configured)));
        scene.check("the level's placed features hold the mod's nine", () -> scene.assertTrue(absent(placedRegistry, placed).isEmpty(), "missing " + absent(placedRegistry, placed)));
        scene.check("the level's biome modifiers hold the mod's seven", () -> scene.assertTrue(absent(modifierRegistry, modifiers).isEmpty(), "missing " + absent(modifierRegistry, modifiers)));
        scene.check("and no other configured, placed or modifier entry of the mod's namespace is there", () -> scene.assertTrue(
                configuredRegistry.keySet().stream().filter(key -> key.getNamespace().equals("jolcraft")).count() == configured.size()
                        && placedRegistry.keySet().stream().filter(key -> key.getNamespace().equals("jolcraft")).count() == placed.size()
                        && modifierRegistry.keySet().stream().filter(key -> key.getNamespace().equals("jolcraft")).count() == modifiers.size(),
                "extra entries"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theBiomeTagsHoldTheMeadowTheForestAndNotPlains(TestScene scene) {
        Registry<Biome> biomes = scene.level().registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<Biome> meadow = biomes.getOrThrow(Biomes.MEADOW);
        Holder<Biome> forest = biomes.getOrThrow(Biomes.FOREST);
        Holder<Biome> plains = biomes.getOrThrow(Biomes.PLAINS);
        scene.check("the meadow is in grows_skybell", () -> scene.assertTrue(meadow.is(FloraTags.GROWS_SKYBELL), "not in"));
        scene.check("the meadow sprouts a skybell from bonemeal too", () -> scene.assertTrue(meadow.is(FloraTags.SPROUTS_SKYBELL), "not in"));
        scene.check("a forest is in sprouts_skybell and not in grows_skybell", () -> scene.assertTrue(forest.is(FloraTags.SPROUTS_SKYBELL) && !forest.is(FloraTags.GROWS_SKYBELL), "forest tags"));
        scene.check("plains is in neither", () -> scene.assertTrue(!plains.is(FloraTags.SPROUTS_SKYBELL) && !plains.is(FloraTags.GROWS_SKYBELL), "plains tags"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theBiomeModifiersReachedTheBiomes(TestScene scene) {
        ServerLevel level = scene.level();
        scene.check("dripstone caves hold bloodroot_dense", () -> scene.assertTrue(hasFeature(level, Biomes.DRIPSTONE_CAVES, "bloodroot_dense"), "absent"));
        scene.check("the warped forest holds cyanella_patch", () -> scene.assertTrue(hasFeature(level, Biomes.WARPED_FOREST, "cyanella_patch"), "absent"));
        scene.check("the meadow holds both skybell patches", () -> scene.assertTrue(hasFeature(level, Biomes.MEADOW, "skybell_patch") && hasFeature(level, Biomes.MEADOW, "skybell_patch_dense"), "absent"));
        scene.check("plains holds bloodroot, duskcap_patch and deepslate_bulbs_patch", () -> scene.assertTrue(hasFeature(level, Biomes.PLAINS, "bloodroot")
                && hasFeature(level, Biomes.PLAINS, "duskcap_patch") && hasFeature(level, Biomes.PLAINS, "deepslate_bulbs_patch"), "absent"));
        scene.check("and no skybell patch, no dense bloodroot and no cyanella", () -> scene.assertTrue(!hasFeature(level, Biomes.PLAINS, "skybell_patch") && !hasFeature(level, Biomes.PLAINS, "skybell_patch_dense")
                && !hasFeature(level, Biomes.PLAINS, "bloodroot_dense") && !hasFeature(level, Biomes.PLAINS, "cyanella_patch"), "present"));
        scene.check("the bonemeal features are in no biome", () -> scene.assertTrue(!hasFeature(level, Biomes.MEADOW, "skybell_bonemeal") && !hasFeature(level, Biomes.WARPED_FOREST, "cyanella_bonemeal"), "present"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ single-block features

    @Scene(maxTicks = 20, isolated = true)
    public void theSkybellFeatureStandsOnGrassAndNotOnStone(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos grassCell = at(scene, 3, FLOOR + 1, 3);
        BlockPos stoneCell = at(scene, 9, FLOOR + 1, 3);
        put(level, grassCell.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        put(level, stoneCell.below(), Blocks.STONE.defaultBlockState());
        put(level, grassCell, Blocks.AIR.defaultBlockState());
        put(level, stoneCell, Blocks.AIR.defaultBlockState());
        scene.check("the skybell feature on a grass block places and gives a skybell", () -> scene.assertTrue(placeConfigured(level, "skybell", grassCell)
                && level.getBlockState(grassCell).is(FloraContent.SKYBELL.get()), "state " + level.getBlockState(grassCell)));
        scene.check("on stone it places nothing", () -> scene.assertTrue(!placeConfigured(level, "skybell", stoneCell) && level.getBlockState(stoneCell).isAir(), "state " + level.getBlockState(stoneCell)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theBloodrootFeatureHangsUnderStone(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell, Blocks.AIR.defaultBlockState());
        put(level, cell.above(), Blocks.STONE.defaultBlockState());
        scene.check("the bloodroot feature under stone places and gives a bloodroot", () -> scene.assertTrue(placeConfigured(level, "bloodroot", cell)
                && level.getBlockState(cell).is(FloraContent.BLOODROOT.get()), "state " + level.getBlockState(cell)));
        BlockPos open = at(scene, 9, FLOOR + 1, 3);
        put(level, open, Blocks.AIR.defaultBlockState());
        put(level, open.above(), Blocks.AIR.defaultBlockState());
        scene.check("with no ceiling it places nothing", () -> scene.assertTrue(!placeConfigured(level, "bloodroot", open) && level.getBlockState(open).isAir(), "state " + level.getBlockState(open)));
        scene.succeed();
    }

    @Scene(maxTicks = FloraArena.ONE_WAIT, isolated = true)
    public void theBulbsFeaturePlacesABulbOfSomeAgeOnDeepslateInTheDark(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = darkBox(scene, 3, 3, Blocks.DEEPSLATE.defaultBlockState());
        whenDark(scene, () -> {
            assertDark(scene, "the box", cell);
            scene.check("the arena is below Y 0, deep enough for the bulbs", () -> scene.assertTrue(BulbsBlock.deepEnough(cell.getY()), "Y " + cell.getY()));
            scene.check("the bulbs feature on deepslate in the dark places a bulb of some age", () -> scene.assertTrue(placeConfigured(level, "deepslate_bulbs", cell)
                    && level.getBlockState(cell).is(FloraContent.DEEPSLATE_BULBS_CROP.get())
                    && level.getBlockState(cell).getValue(BulbsBlock.AGE) >= 0, "state " + level.getBlockState(cell)));
            scene.succeed();
        }, cell);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theCyanellaPatchGrowsOnWarpedNyliumAndNotOnStone(TestScene scene) {
        ServerLevel level = scene.level();
        BlockState nylium = Blocks.WARPED_NYLIUM.defaultBlockState();
        BlockPos nyliumOrigin = at(scene, 6, FLOOR + 1, 6);
        BlockPos stoneOrigin = at(scene, 20, FLOOR + 1, 6);
        fill(level, nyliumOrigin.offset(-REACH, -1, -REACH), nyliumOrigin.offset(REACH, -1, REACH), nylium);
        fill(level, stoneOrigin.offset(-REACH, -1, -REACH), stoneOrigin.offset(REACH, -1, REACH), Blocks.STONE.defaultBlockState());
        fill(level, nyliumOrigin.offset(-REACH, 0, -REACH), nyliumOrigin.offset(REACH, 2, REACH), Blocks.AIR.defaultBlockState());
        fill(level, stoneOrigin.offset(-REACH, 0, -REACH), stoneOrigin.offset(REACH, 2, REACH), Blocks.AIR.defaultBlockState());
        Block cyanella = FloraContent.CYANELLA.get();
        scene.check("the cyanella patch on a floor of warped nylium places at least one cyanella", () -> scene.assertTrue(placeConfigured(level, "cyanella_patch", nyliumOrigin)
                && count(level, nyliumOrigin.offset(-REACH, 0, -REACH), nyliumOrigin.offset(REACH, 2, REACH), state -> state.is(cyanella)) >= 1, "none placed"));
        scene.check("on a floor of stone it places none", () -> scene.assertTrue(!placeConfigured(level, "cyanella_patch", stoneOrigin)
                && count(level, stoneOrigin.offset(-REACH, 0, -REACH), stoneOrigin.offset(REACH, 2, REACH), state -> state.is(cyanella)) == 0, "some placed"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ the two features of bonemeal

    /** What a run of a bonemeal feature came to over {@link #MANY} placements. */
    private record Run(int placements, int most, int farthest) {
    }

    /**
     * Places the placed feature {@link #MANY} times at {@code origin}, over a floor of {@code floor} that reaches {@link #REACH}
     * blocks each way, and counts after each the plants of {@code plant} in the air above it; the air is cleared again before the
     * next. Answers how many placements put a plant, the most one placement put, and the farthest (horizontally) a plant stood from the origin.
     */
    private static Run run(ServerLevel level, String path, BlockPos origin, BlockState floor, Block plant) {
        BlockPos from = origin.offset(-REACH, 0, -REACH);
        BlockPos to = origin.offset(REACH, 2, REACH);
        fill(level, origin.offset(-REACH, -1, -REACH), origin.offset(REACH, -1, REACH), floor);
        fill(level, from, to, Blocks.AIR.defaultBlockState());
        int placements = 0;
        int most = 0;
        int farthest = 0;
        for (int i = 0; i < MANY; i++) {
            placePlaced(level, path, origin);
            int found = 0;
            for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
                if (level.getBlockState(pos).is(plant)) {
                    found++;
                    farthest = Math.max(farthest, Math.max(Math.abs(pos.getX() - origin.getX()), Math.abs(pos.getZ() - origin.getZ())));
                    put(level, pos, Blocks.AIR.defaultBlockState());
                }
            }
            placements += found > 0 ? 1 : 0;
            most = Math.max(most, found);
        }
        return new Run(placements, most, farthest);
    }

    @Scene(maxTicks = 40, isolated = true)
    public void theCyanellaBonemealFeatureSometimesPlacesUpToTwoWithinThreeBlocks(TestScene scene) {
        ServerLevel level = scene.level();
        Run run = run(level, "cyanella_bonemeal", at(scene, 6, FLOOR + 1, 6), Blocks.WARPED_NYLIUM.defaultBlockState(), FloraContent.CYANELLA.get());
        scene.check("over " + MANY + " placements the cyanella feature places a cyanella sometimes (" + run.placements() + " times)", () -> scene.assertTrue(run.placements() > 0 && run.placements() < MANY, "placed " + run.placements()));
        scene.check("never more than two at once (most " + run.most() + ")", () -> scene.assertTrue(run.most() <= 2, "most " + run.most()));
        scene.check("and never further than three blocks from the origin (farthest " + run.farthest() + ")", () -> scene.assertTrue(run.farthest() <= 3, "farthest " + run.farthest()));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void theSkybellBonemealFeatureNeverPlacesBelowYOneHundredAndOne(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos origin = at(scene, 6, FLOOR + 1, 6);
        Run run = run(level, "skybell_bonemeal", origin, Blocks.GRASS_BLOCK.defaultBlockState(), FloraContent.SKYBELL.get());
        scene.check("the arena is far below Y 101", () -> scene.assertTrue(origin.getY() < 101, "Y " + origin.getY()));
        scene.check("over " + MANY + " placements on grass at the arena's height the skybell feature places nothing", () -> scene.assertEquals(0, run.placements(), "placements"));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void theSkybellBonemealFeatureSometimesPlacesUpToTwoOnGrassAboveYOneHundredAndOne(TestScene scene) {
        ServerLevel level = scene.level();
        // the platform is built on arena rows far above the structure and taken away again
        int row = PLATFORM + 1 - scene.origin().getY();
        BlockPos origin = at(scene, 6, row, 6);
        scene.onEnd(() -> fill(level, origin.offset(-REACH, -1, -REACH), origin.offset(REACH, 2, REACH), Blocks.AIR.defaultBlockState()));
        Run run = run(level, "skybell_bonemeal", origin, Blocks.GRASS_BLOCK.defaultBlockState(), FloraContent.SKYBELL.get());
        scene.check("the platform's origin is at Y " + origin.getY() + ", at 101 or above", () -> scene.assertTrue(origin.getY() >= 101, "Y " + origin.getY()));
        scene.check("over " + MANY + " placements the skybell feature places a skybell sometimes (" + run.placements() + " times)", () -> scene.assertTrue(run.placements() > 0 && run.placements() < MANY, "placed " + run.placements()));
        scene.check("never more than two at once (most " + run.most() + ")", () -> scene.assertTrue(run.most() <= 2, "most " + run.most()));
        scene.check("and never further than three blocks from the origin (farthest " + run.farthest() + ")", () -> scene.assertTrue(run.farthest() <= 3, "farthest " + run.farthest()));
        scene.succeed();
    }

    // ------------------------------------------------------------------ the listener

    private static boolean bonemeal(ServerLevel level, BlockPos pos) {
        return BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL, 64), level, pos, null);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theListenerPlacesTheFixtureRowsThatHoldAndSkipsTheOthers(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos dirt = at(scene, 3, FLOOR, 3);
        BlockPos bare = at(scene, 9, FLOOR, 3);
        fill(level, dirt.offset(0, -1, 0), dirt.offset(0, 4, 0), Blocks.AIR.defaultBlockState());
        fill(level, bare.offset(0, -1, 0), bare.offset(0, 4, 0), Blocks.AIR.defaultBlockState());
        put(level, dirt, Blocks.ROOTED_DIRT.defaultBlockState());
        put(level, bare, Blocks.ROOTED_DIRT.defaultBlockState());
        put(level, bare.below(), Blocks.STONE.defaultBlockState());
        scene.check("the level's biome at the arena is plains, which the fixture tags rely on", () -> scene.assertTrue(level.getBiome(dirt).is(Biomes.PLAINS), "biome " + level.getBiome(dirt).getRegisteredName()));
        scene.check("rooted dirt with air below is a valid bonemeal target and with stone below is not", () -> scene.assertTrue(((BonemealableBlock) Blocks.ROOTED_DIRT).isValidBonemealTarget(level, dirt, level.getBlockState(dirt))
                && !((BonemealableBlock) Blocks.ROOTED_DIRT).isValidBonemealTarget(level, bare, level.getBlockState(bare)), "targets"));
        scene.check("real bonemeal on the invalid target is not used", () -> scene.assertFalse(bonemeal(level, bare), "used"));
        scene.check("and no marker stands above it", () -> scene.assertTrue(count(level, bare.above(), bare.offset(0, 4, 0), state -> !state.isAir()) == 0, "something above"));
        scene.check("real bonemeal on the valid target is used", () -> scene.assertTrue(bonemeal(level, dirt), "not used"));
        scene.check("the row with no biome tag put its marker (gold) above the rooted dirt", () -> scene.assertTrue(level.getBlockState(dirt.above()).is(Blocks.GOLD_BLOCK), "state " + level.getBlockState(dirt.above())));
        scene.check("the row with the tag the biome is in put its marker (iron) one higher", () -> scene.assertTrue(level.getBlockState(dirt.above(2)).is(Blocks.IRON_BLOCK), "state " + level.getBlockState(dirt.above(2))));
        scene.check("the row with the tag the biome is not in put none (no diamond)", () -> scene.assertTrue(count(level, dirt.above(), dirt.offset(0, 4, 0), state -> state.is(Blocks.DIAMOND_BLOCK)) == 0, "a diamond block"));
        scene.check("the first fixture row names a feature the level lacks, and was skipped without stopping the rows after it", () -> scene.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                .get(ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("jolcraft_gametest", "sprout_missing"))).isEmpty(), "the feature exists"));
        scene.check("the game's own bonemeal went on: hanging roots grew below", () -> scene.assertTrue(level.getBlockState(dirt.below()).is(Blocks.HANGING_ROOTS), "state " + level.getBlockState(dirt.below())));
        scene.succeed();
    }

    // ------------------------------------------------------------------ the mod's own rows

    @Scene(maxTicks = 20, isolated = true)
    public void theModsOwnBonemealRowsAreAsTheGameLoadedThem(TestScene scene) {
        ServerLevel level = scene.level();
        List<FloraData.Sprout> grass = BuiltInRegistries.BLOCK.getData(FloraData.BONEMEAL, BuiltInRegistries.BLOCK.getResourceKey(Blocks.GRASS_BLOCK).orElseThrow());
        List<FloraData.Sprout> nylium = BuiltInRegistries.BLOCK.getData(FloraData.BONEMEAL, BuiltInRegistries.BLOCK.getResourceKey(Blocks.WARPED_NYLIUM).orElseThrow());
        Registry<PlacedFeature> features = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        scene.check("the grass block's entry is one row naming jolcraft:skybell_bonemeal with the tag jolcraft:sprouts_skybell", () -> scene.assertEquals(
                List.of(new FloraData.Sprout(placedKey("skybell_bonemeal"), Optional.of(FloraTags.SPROUTS_SKYBELL))), grass, "grass rows"));
        scene.check("warped nylium's entry is one row naming jolcraft:cyanella_bonemeal with no tag", () -> scene.assertEquals(
                List.of(new FloraData.Sprout(placedKey("cyanella_bonemeal"), Optional.empty())), nylium, "nylium rows"));
        scene.check("both features exist in the level's registry", () -> scene.assertTrue(features.containsKey(placedKey("skybell_bonemeal")) && features.containsKey(placedKey("cyanella_bonemeal")), "missing"));
        scene.check("and neither carries a biome filter, which would throw outside world generation", () -> scene.assertTrue(
                features.getOrThrow(placedKey("skybell_bonemeal")).value().placement().stream().noneMatch(modifier -> modifier.type() == PlacementModifierType.BIOME_FILTER)
                        && features.getOrThrow(placedKey("cyanella_bonemeal")).value().placement().stream().noneMatch(modifier -> modifier.type() == PlacementModifierType.BIOME_FILTER), "a biome filter"));
        scene.succeed();
    }

    @Scene(maxTicks = 100, isolated = true)
    public void realBonemealOnWarpedNyliumSproutsACyanellaSometimes(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos center = at(scene, 6, FLOOR, 6);
        fill(level, center.offset(-REACH, 0, -REACH), center.offset(REACH, 0, REACH), Blocks.WARPED_NYLIUM.defaultBlockState());
        BlockPos from = center.offset(-REACH, 1, -REACH);
        BlockPos to = center.offset(REACH, 5, REACH);
        fill(level, from, to, Blocks.AIR.defaultBlockState());
        Block cyanella = FloraContent.CYANELLA.get();
        int used = 0;
        int consumed = 0;
        int sprouted = 0;
        for (int i = 0; i < APPLICATIONS; i++) {
            BlockPos pos = center.offset(i % 3 - 1, 0, i / 3 % 3 - 1);
            ItemStack stack = new ItemStack(Items.BONE_MEAL, 64);
            used += BoneMealItem.applyBonemeal(stack, level, pos, null) ? 1 : 0;
            consumed += 64 - stack.getCount();
            sprouted += count(level, from, to, state -> state.is(cyanella)) > 0 ? 1 : 0;
            fill(level, from, to, Blocks.AIR.defaultBlockState());
        }
        int usedAll = used;
        int consumedAll = consumed;
        int sproutedAll = sprouted;
        // a sprout that takes the cell above the nylium would have the game refuse the bonemeal; the listener consumes it then
        scene.check("every one of the " + APPLICATIONS + " applications was accepted", () -> scene.assertEquals(APPLICATIONS, usedAll, "accepted applications"));
        scene.check("and each consumed exactly one bonemeal, so no cyanella grows for free", () -> scene.assertEquals(APPLICATIONS, consumedAll, "bonemeal consumed"));
        scene.check("a cyanella sprouted in " + sproutedAll + " of them, at least once and not every time", () -> scene.assertTrue(sproutedAll > 0 && sproutedAll < APPLICATIONS, "sprouted " + sproutedAll));
        scene.succeed();
    }
}
