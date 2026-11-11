package net.sievert.jolcraft.datagen.module.system.flora;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NetherForestVegetationConfig;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.sievert.jolcraft.datagen.Contributes;
import net.sievert.jolcraft.datagen.contribute.RegistryContributor;
import net.sievert.jolcraft.datagen.provider.DatapackEntries;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.system.flora.BulbsBlock;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraFeatures;
import net.sievert.jolcraft.module.system.flora.FloraTags;
import net.sievert.jolcraft.module.system.flora.LayeredMushroomFeature;
import net.sievert.jolcraft.module.system.worldgen.HeightFilter;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The flora system's world: eight configured features, nine placed features and seven biome modifiers.
 * <p>
 * The configured features are a single block each of bloodroot, skybell, cyanella, duskcap and the bulbs (at a random
 * age), the cyanella as the nether's own forest vegetation, and the two huge mushrooms under the keys the mushroom
 * blocks name, with a {@code foliage_radius} that is the whole height of the shape less four, which is what the
 * mushroom block's bonemeal check reads it for; the features themselves do not read it.
 * <p>
 * The placed features carry the old numbers in the old order. An old random patch becomes {@code count},
 * {@code random_offset} and a filter on air after the feature's own modifiers: the single-block feature itself checks
 * that the plant survives. The two of bonemeal carry no biome filter, which throws outside world generation, and are
 * placed by the flora system for the rows of its bonemeal data map. The biome modifiers add one placed feature each at
 * the vegetal decoration step.
 */
@Contributes(Dictionary.FLORA)
public final class FloraRegistries implements RegistryContributor {

    /** The height the game's bonemeal check adds the radius to. */
    private static final int BASE_HEIGHT = 4;
    /** The width and the height of the cyanella patch's spread. */
    private static final int CYANELLA_SPREAD_WIDTH = 5;
    private static final int CYANELLA_SPREAD_HEIGHT = 3;
    /** How far above its spot the bloodroot looks for a ceiling, in blocks. */
    private static final int CEILING_SCAN = 12;
    /** The top of the bloodroot's world, absolute Y. */
    private static final int BLOODROOT_TOP = 64;
    /** The lowest the two skybell patches generate, absolute Y. */
    private static final int SKYBELL_FLOOR = 100;
    private static final int SKYBELL_DENSE_FLOOR = 115;
    /** The skybell of bonemeal is placed above the grass block, and the old rule asked the block to be at the floor or above. */
    private static final int SKYBELL_BONEMEAL_FLOOR = SKYBELL_FLOOR + 1;
    /** The depth the duskcap and the bulbs generate in, absolute Y. */
    private static final int DEEP_BOTTOM = -64;
    private static final int DEEP_TOP = 0;

    @Override
    public void registries(DatapackEntries entries) {
        entries.add(Registries.CONFIGURED_FEATURE, FloraRegistries::configured);
        entries.add(Registries.PLACED_FEATURE, FloraRegistries::placed);
        entries.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, FloraRegistries::modifiers);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String... words) {
        return Ids.key(Registries.CONFIGURED_FEATURE, Dictionary.JOLCRAFT, words);
    }

    private static ResourceKey<PlacedFeature> placedKey(String... words) {
        return Ids.key(Registries.PLACED_FEATURE, Dictionary.JOLCRAFT, words);
    }

    private static ResourceKey<BiomeModifier> modifierKey(String... words) {
        return Ids.key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Dictionary.JOLCRAFT, words);
    }

    private static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        huge(context, FloraFeatures.HUGE_DUSKCAP, FloraContent.HUGE_DUSKCAP.get(), FloraContent.DUSKCAP_BLOCK.get(), FloraContent.DUSKCAP_STEM.get());
        huge(context, FloraFeatures.HUGE_FESTERLING, FloraContent.HUGE_FESTERLING.get(), FloraContent.FESTERLING_BLOCK.get(), FloraContent.FESTERLING_STEM.get());

        single(context, configuredKey(Dictionary.BLOODROOT), BlockStateProvider.simple(FloraContent.BLOODROOT.get()));
        single(context, configuredKey(Dictionary.SKYBELL), BlockStateProvider.simple(FloraContent.SKYBELL.get()));
        single(context, configuredKey(Dictionary.CYANELLA), BlockStateProvider.simple(FloraContent.CYANELLA.get()));
        single(context, configuredKey(Dictionary.DUSKCAP), BlockStateProvider.simple(FloraContent.DUSKCAP.get()));
        WeightedList.Builder<BlockState> ages = WeightedList.builder();
        for (int age = 0; age <= BulbsBlock.MAX_AGE; age++) {
            ages.add(FloraContent.DEEPSLATE_BULBS_CROP.get().getStateForAge(age), 1);
        }
        single(context, configuredKey(Dictionary.DEEPSLATE, Dictionary.BULBS), new WeightedStateProvider(ages));

        context.register(configuredKey(Dictionary.CYANELLA, Dictionary.PATCH), new ConfiguredFeature<>(Feature.NETHER_FOREST_VEGETATION,
                new NetherForestVegetationConfig(BlockStateProvider.simple(FloraContent.CYANELLA.get()), CYANELLA_SPREAD_WIDTH, CYANELLA_SPREAD_HEIGHT)));
    }

    private static void single(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, BlockStateProvider provider) {
        context.register(key, new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(provider)));
    }

    private static void huge(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, LayeredMushroomFeature feature, Block cap, Block stem) {
        context.register(key, new ConfiguredFeature<>(feature, new HugeMushroomFeatureConfiguration(BlockStateProvider.simple(cap), BlockStateProvider.simple(stem),
                feature.shape().height() - BASE_HEIGHT, BlockPredicate.matchesTag(BlockTags.HUGE_BROWN_MUSHROOM_CAN_PLACE_ON))));
    }

    // the game marks the count on every layer as deprecated and still places the nether's own vegetation with it
    @SuppressWarnings("deprecation")
    private static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder<ConfiguredFeature<?, ?>> bloodroot = features.getOrThrow(configuredKey(Dictionary.BLOODROOT));
        Holder<ConfiguredFeature<?, ?>> skybell = features.getOrThrow(configuredKey(Dictionary.SKYBELL));
        Holder<ConfiguredFeature<?, ?>> cyanella = features.getOrThrow(configuredKey(Dictionary.CYANELLA));
        Holder<ConfiguredFeature<?, ?>> duskcap = features.getOrThrow(configuredKey(Dictionary.DUSKCAP));
        Holder<ConfiguredFeature<?, ?>> bulbs = features.getOrThrow(configuredKey(Dictionary.DEEPSLATE, Dictionary.BULBS));
        Holder<ConfiguredFeature<?, ?>> cyanellaPatch = features.getOrThrow(configuredKey(Dictionary.CYANELLA, Dictionary.PATCH));

        context.register(placedKey(Dictionary.BLOODROOT), new PlacedFeature(bloodroot, bloodroot(List.of(CountPlacement.of(1), RarityFilter.onAverageOnceEvery(2)))));
        context.register(placedKey(Dictionary.BLOODROOT, Dictionary.DENSE), new PlacedFeature(bloodroot, bloodroot(List.of(CountPlacement.of(3)))));

        context.register(placedKey(Dictionary.CYANELLA, Dictionary.PATCH), new PlacedFeature(cyanellaPatch, List.of(
                CountOnEveryLayerPlacement.of(1), RarityFilter.onAverageOnceEvery(4), BiomeFilter.biome())));

        context.register(placedKey(Dictionary.SKYBELL, Dictionary.PATCH), new PlacedFeature(skybell, skybell(5, SKYBELL_FLOOR)));
        context.register(placedKey(Dictionary.SKYBELL, Dictionary.PATCH, Dictionary.DENSE), new PlacedFeature(skybell, skybell(3, SKYBELL_DENSE_FLOOR)));

        context.register(placedKey(Dictionary.DUSKCAP, Dictionary.PATCH), new PlacedFeature(duskcap, patched(List.of(
                RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(DEEP_BOTTOM), VerticalAnchor.absolute(DEEP_TOP)), BiomeFilter.biome()), 30, 7, 3)));
        context.register(placedKey(Dictionary.DEEPSLATE, Dictionary.BULBS, Dictionary.PATCH), new PlacedFeature(bulbs, patched(List.of(
                RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(DEEP_BOTTOM), VerticalAnchor.absolute(DEEP_TOP)),
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesTag(new Vec3i(0, -1, 0), FloraTags.SUPPORTS_DEEPSLATE_BULBS)), BiomeFilter.biome()), 25, 3, 2)));

        context.register(placedKey(Dictionary.SKYBELL, Dictionary.BONEMEAL), new PlacedFeature(skybell, List.of(
                RarityFilter.onAverageOnceEvery(4), new HeightFilter(SKYBELL_BONEMEAL_FLOOR, Integer.MAX_VALUE),
                CountPlacement.of(2), RandomOffsetPlacement.ofTriangle(3, 1), airWhereSurvives(FloraContent.SKYBELL.get()))));
        context.register(placedKey(Dictionary.CYANELLA, Dictionary.BONEMEAL), new PlacedFeature(cyanella, List.of(
                RarityFilter.onAverageOnceEvery(4),
                CountPlacement.of(2), RandomOffsetPlacement.ofTriangle(3, 1), airWhereSurvives(FloraContent.CYANELLA.get()))));
    }

    /** The bloodroot's modifiers: the count and rarity it is given, then a spot in the caves, up to a ceiling, one block under it. */
    private static List<PlacementModifier> bloodroot(List<PlacementModifier> start) {
        List<PlacementModifier> modifiers = new ArrayList<>(start);
        modifiers.add(InSquarePlacement.spread());
        modifiers.add(HeightRangePlacement.uniform(VerticalAnchor.absolute(DEEP_TOP), VerticalAnchor.absolute(BLOODROOT_TOP)));
        modifiers.add(EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, CEILING_SCAN));
        modifiers.add(RandomOffsetPlacement.vertical(ConstantInt.of(-1)));
        modifiers.add(new HeightFilter(Integer.MIN_VALUE, BLOODROOT_TOP));
        modifiers.add(BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(FloraContent.BLOODROOT.get().defaultBlockState(), BlockPos.ZERO)));
        modifiers.add(BiomeFilter.biome());
        return modifiers;
    }

    /** A skybell patch: a rarity, a spot on the surface at or above {@code floor}, then 15 tries within 5 and 3. */
    private static List<PlacementModifier> skybell(int rarity, int floor) {
        return patched(List.of(RarityFilter.onAverageOnceEvery(rarity), InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING),
                new HeightFilter(floor, Integer.MAX_VALUE), BiomeFilter.biome()), 15, 5, 3);
    }

    /** What the old random patch became, after the feature's own modifiers: so many tries at spots scattered about the origin, each only into air. */
    private static List<PlacementModifier> patched(List<PlacementModifier> start, int tries, int xz, int y) {
        List<PlacementModifier> modifiers = new ArrayList<>(start);
        modifiers.add(CountPlacement.of(tries));
        modifiers.add(RandomOffsetPlacement.ofTriangle(xz, y));
        modifiers.add(BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE));
        return modifiers;
    }

    private static BlockPredicateFilter airWhereSurvives(Block plant) {
        return BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.wouldSurvive(plant.defaultBlockState(), BlockPos.ZERO)));
    }

    private static void modifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        add(context, features, biomes.getOrThrow(BiomeTags.IS_OVERWORLD), Dictionary.BLOODROOT);
        add(context, features, HolderSet.direct(biomes.getOrThrow(Biomes.DRIPSTONE_CAVES)), Dictionary.BLOODROOT, Dictionary.DENSE);
        add(context, features, HolderSet.direct(biomes.getOrThrow(Biomes.WARPED_FOREST)), Dictionary.CYANELLA, Dictionary.PATCH);
        add(context, features, biomes.getOrThrow(FloraTags.GROWS_SKYBELL), Dictionary.SKYBELL, Dictionary.PATCH);
        add(context, features, HolderSet.direct(biomes.getOrThrow(Biomes.MEADOW)), Dictionary.SKYBELL, Dictionary.PATCH, Dictionary.DENSE);
        add(context, features, biomes.getOrThrow(BiomeTags.IS_OVERWORLD), Dictionary.DUSKCAP, Dictionary.PATCH);
        add(context, features, biomes.getOrThrow(BiomeTags.IS_OVERWORLD), Dictionary.DEEPSLATE, Dictionary.BULBS, Dictionary.PATCH);
    }

    /** One modifier, {@code add_<words>}, that adds the placed feature of the same words to the biomes at the vegetal decoration step. */
    private static void add(BootstrapContext<BiomeModifier> context, HolderGetter<PlacedFeature> features, HolderSet<Biome> biomes, String... words) {
        String[] named = new String[words.length + 1];
        named[0] = Dictionary.ADD;
        System.arraycopy(words, 0, named, 1, words.length);
        context.register(modifierKey(named), new BiomeModifiers.AddFeaturesBiomeModifier(biomes, HolderSet.direct(features.getOrThrow(placedKey(words))),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }
}
