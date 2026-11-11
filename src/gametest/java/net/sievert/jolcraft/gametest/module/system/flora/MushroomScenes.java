package net.sievert.jolcraft.gametest.module.system.flora;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraFeatures;
import net.sievert.jolcraft.module.system.flora.GrowthEvent;
import net.sievert.jolcraft.module.system.flora.LayeredMushroomFeature;
import net.sievert.jolcraft.module.system.flora.MushroomShape;

import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.FLOOR;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.at;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.count;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.fill;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.put;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.verdantSoil;

/**
 * The two huge mushrooms against the real game: each configured feature placed through the level's registry on verdant soil,
 * the blocks it builds counted against its shape and its faces against the one rule, the room it needs, and bonemeal
 * growing them from the small mushroom with the chance forced to one. Every scene does its work in its first tick and runs in a batch
 * of its own, since a mushroom is wider than the six blocks the game puts between the arenas of one batch.
 */
public final class MushroomScenes {

    /** Cells around the origin that the biggest shape reaches, sideways. */
    private static final int REACH = 3;
    /** Cells above the origin that the tallest shape reaches. */
    private static final int HIGH = 9;

    private static BlockState air() {
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockPos origin(TestScene scene) {
        return at(scene, 6, FLOOR + 1, 6);
    }

    private static BlockPos corner(BlockPos origin, int sign, int height) {
        return origin.offset(sign * REACH, height, sign * REACH);
    }

    /** Verdant soil under the whole site and nothing in the room above it. */
    private static void prepare(ServerLevel level, BlockPos origin) {
        fill(level, corner(origin, -1, -1), corner(origin, 1, -1), verdantSoil());
        fill(level, corner(origin, -1, 0), corner(origin, 1, HIGH), air());
    }

    private static boolean huge(BlockState state, Block cap, Block stem) {
        return state.is(cap) || state.is(stem);
    }

    private static ConfiguredFeature<?, ?> configured(ServerLevel level, ResourceKey<ConfiguredFeature<?, ?>> key) {
        return level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(key).value();
    }

    private static boolean place(ServerLevel level, ResourceKey<ConfiguredFeature<?, ?>> key, BlockPos origin) {
        return configured(level, key).place(level, level.getChunkSource().getGenerator(), level.getRandom(), origin);
    }

    private static boolean face(ServerLevel level, BlockPos origin, int dx, int dy, int dz, Direction side) {
        return level.getBlockState(origin.offset(dx, dy, dz)).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
    }

    private static void facing(TestScene scene, ServerLevel level, BlockPos origin, String what, int dx, int dy, int dz, Direction side, boolean shown) {
        scene.check(what + ": the " + side + " face is " + (shown ? "shown, towards the open" : "hidden, towards the mushroom"),
                () -> scene.assertEquals(shown, face(level, origin, dx, dy, dz, side), what + " " + side));
    }

    /** What the shape says the mushroom is made of, checked against the world cell by cell in the box around the origin. */
    private static void matchesShape(TestScene scene, ServerLevel level, BlockPos origin, MushroomShape shape, Block cap, Block stem) {
        int wrong = 0;
        int badFaces = 0;
        String first = "";
        for (int dy = 0; dy <= HIGH; dy++) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    BlockState state = level.getBlockState(origin.offset(dx, dy, dz));
                    boolean belongs = shape.contains(dx, dy, dz);
                    Block expected = !belongs ? Blocks.AIR : (dx == 0 && dz == 0 && dy < shape.stem() ? stem : cap);
                    if (!state.is(expected)) {
                        wrong++;
                        first = first.isEmpty() ? (dx + "," + dy + "," + dz + " is " + state) : first;
                    } else if (belongs) {
                        for (Direction side : Direction.values()) {
                            boolean shown = !shape.contains(dx + side.getStepX(), dy + side.getStepY(), dz + side.getStepZ());
                            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side)) != shown) {
                                badFaces++;
                                first = first.isEmpty() ? (dx + "," + dy + "," + dz + " " + side + " is " + state) : first;
                            }
                        }
                    }
                }
            }
        }
        int cellsWrong = wrong;
        int facesWrong = badFaces;
        String where = first;
        scene.check("every cell of the room is the stem, the cap or air as the shape says", () -> scene.assertEquals(0, cellsWrong, "cells that differ, the first " + where));
        scene.check("every face of every block is shown exactly where the neighbouring cell is no part of the mushroom", () -> scene.assertEquals(0, facesWrong, "faces that differ, the first " + where));
    }

    /** The stem in the origin's column and, height by height, as many cap blocks as the rings say and the tip. */
    private static void counts(TestScene scene, ServerLevel level, BlockPos origin, MushroomShape shape, Block cap, Block stem) {
        int stems = count(level, origin, origin.above(HIGH), state -> state.is(stem));
        scene.check("the stem is " + shape.stem() + " blocks, in the origin's column", () -> scene.assertTrue(stems == shape.stem()
                && count(level, origin, origin.above(shape.stem() - 1), state -> state.is(stem)) == shape.stem(), "stem blocks " + stems));
        Map<Integer, Integer> expected = new HashMap<>();
        shape.rings().forEach(ring -> expected.merge(ring.height(), ring.count(), Integer::sum));
        expected.merge(shape.stem(), 1, Integer::sum);
        for (int dy = 0; dy <= HIGH; dy++) {
            int height = dy;
            int found = count(level, origin.offset(-REACH, height, -REACH), origin.offset(REACH, height, REACH), state -> state.is(cap));
            scene.check("at height " + height + " the cap has " + expected.getOrDefault(height, 0) + " blocks", () -> scene.assertEquals(expected.getOrDefault(height, 0), found, "cap blocks"));
        }
        scene.check("and nothing else of the mushroom is anywhere in the room", () -> scene.assertEquals(shape.stem() + expected.values().stream().mapToInt(Integer::intValue).sum(),
                count(level, origin.offset(-REACH, 0, -REACH), origin.offset(REACH, HIGH, REACH), state -> huge(state, cap, stem)), "blocks of the mushroom"));
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theHugeDuskcapIsBuiltAsItsShapeSaysAndShowsItsFacesByOneRule(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos origin = origin(scene);
        MushroomShape shape = FloraContent.HUGE_DUSKCAP.get().shape();
        Block cap = FloraContent.DUSKCAP_BLOCK.get();
        Block stem = FloraContent.DUSKCAP_STEM.get();
        prepare(level, origin);
        boolean placed = place(level, FloraFeatures.HUGE_DUSKCAP, origin);
        scene.check("the feature is placed on verdant soil at a free spot", () -> scene.assertTrue(placed, "not placed"));
        counts(scene, level, origin, shape, cap, stem);
        matchesShape(scene, level, origin, shape, cap, stem);
        facing(scene, level, origin, "a stem block under the skirt", 0, 3, 0, Direction.EAST, true);
        facing(scene, level, origin, "the same", 0, 3, 0, Direction.UP, false);
        facing(scene, level, origin, "the same", 0, 3, 0, Direction.DOWN, false);
        facing(scene, level, origin, "the skirt", 2, 3, 0, Direction.WEST, true);
        facing(scene, level, origin, "the skirt", 2, 3, 0, Direction.EAST, true);
        facing(scene, level, origin, "the skirt", 2, 3, 0, Direction.NORTH, false);
        facing(scene, level, origin, "the skirt", 2, 3, 0, Direction.UP, false);
        facing(scene, level, origin, "a corner of the lowest ring", 2, 2, 2, Direction.DOWN, true);
        facing(scene, level, origin, "a corner of the lowest ring", 2, 2, 2, Direction.UP, true);
        facing(scene, level, origin, "the tip", 0, 7, 0, Direction.UP, true);
        facing(scene, level, origin, "the tip", 0, 7, 0, Direction.DOWN, false);
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theHugeFesterlingIsBuiltAsItsShapeSaysAndShowsItsFacesByOneRule(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos origin = origin(scene);
        MushroomShape shape = FloraContent.HUGE_FESTERLING.get().shape();
        Block cap = FloraContent.FESTERLING_BLOCK.get();
        Block stem = FloraContent.FESTERLING_STEM.get();
        prepare(level, origin);
        boolean placed = place(level, FloraFeatures.HUGE_FESTERLING, origin);
        scene.check("the feature is placed on verdant soil at a free spot", () -> scene.assertTrue(placed, "not placed"));
        counts(scene, level, origin, shape, cap, stem);
        matchesShape(scene, level, origin, shape, cap, stem);
        facing(scene, level, origin, "the stem", 0, 1, 0, Direction.EAST, true);
        facing(scene, level, origin, "the stem", 0, 0, 0, Direction.DOWN, true);
        facing(scene, level, origin, "the brim", 2, 1, 0, Direction.WEST, true);
        facing(scene, level, origin, "the brim", 2, 1, 0, Direction.NORTH, false);
        facing(scene, level, origin, "the brim", 2, 1, 0, Direction.UP, true);
        facing(scene, level, origin, "the upper ring", 1, 2, 0, Direction.WEST, false);
        facing(scene, level, origin, "the upper ring", 1, 2, 0, Direction.DOWN, true);
        facing(scene, level, origin, "the tip", 0, 3, 0, Direction.DOWN, false);
        facing(scene, level, origin, "the tip", 0, 3, 0, Direction.UP, true);
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aMushroomPlacesNothingWhereABlockStandsInsideItsRoom(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos origin = origin(scene);
        prepare(level, origin);
        put(level, origin.offset(1, 3, 0), Blocks.STONE.defaultBlockState());
        boolean duskcap = place(level, FloraFeatures.HUGE_DUSKCAP, origin);
        int duskcapBlocks = count(level, corner(origin, -1, 0), corner(origin, 1, HIGH), state -> huge(state, FloraContent.DUSKCAP_BLOCK.get(), FloraContent.DUSKCAP_STEM.get()));
        scene.check("a stone inside the duskcap's skirt: nothing is placed", () -> scene.assertTrue(!duskcap && duskcapBlocks == 0, "placed " + duskcap + ", blocks " + duskcapBlocks));
        prepare(level, origin);
        put(level, origin.offset(1, 1, 0), Blocks.STONE.defaultBlockState());
        boolean festerling = place(level, FloraFeatures.HUGE_FESTERLING, origin);
        int festerlingBlocks = count(level, corner(origin, -1, 0), corner(origin, 1, HIGH), state -> huge(state, FloraContent.FESTERLING_BLOCK.get(), FloraContent.FESTERLING_STEM.get()));
        scene.check("a stone in the gap of the festerling's brim: nothing is placed", () -> scene.assertTrue(!festerling && festerlingBlocks == 0, "placed " + festerling + ", blocks " + festerlingBlocks));
        scene.check("and the stone stays", () -> scene.assertTrue(level.getBlockState(origin.offset(1, 1, 0)).is(Blocks.STONE), "state " + level.getBlockState(origin.offset(1, 1, 0))));
        scene.succeed();
    }

    // ------------------------------------------------------------------ bonemeal

    /** Grows the small mushroom at the origin with bonemeal, the chance forced to one for that position, and checks the result. */
    private static void grows(TestScene scene, ServerLevel level, BlockPos origin, Block small, MushroomShape shape, Block cap, Block stem, String name) {
        prepare(level, origin);
        MushroomBlock mushroom = (MushroomBlock) small;
        BlockState state = small.defaultBlockState();
        put(level, origin, state);
        scene.check("the " + name + " is a bonemeal target with room above it", () -> scene.assertTrue(mushroom.isValidBonemealTarget(level, origin, state), "not a target"));
        int succeeded = 0;
        for (int i = 0; i < 100; i++) {
            succeeded += mushroom.isBonemealSuccess(level, level.getRandom(), origin, state) ? 1 : 0;
        }
        int won = succeeded;
        scene.check("with the bonemeal chance at 1 the " + name + " grows a hundred times of a hundred", () -> scene.assertEquals(100, won, "successes"));
        mushroom.performBonemeal(level, level.getRandom(), origin, state);
        scene.check("it leaves a " + name + " stem where the mushroom stood", () -> scene.assertTrue(level.getBlockState(origin).is(stem), "state " + level.getBlockState(origin)));
        scene.check("and the tip of the huge " + name + " on top of the stem", () -> scene.assertTrue(level.getBlockState(origin.above(shape.stem())).is(cap), "state " + level.getBlockState(origin.above(shape.stem()))));
        counts(scene, level, origin, shape, cap, stem);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void bonemealGrowsTheSmallMushroomsIntoTheirHugeFormsWhenTheChanceIsOne(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos origin = origin(scene);
        Consumer<GrowthEvent.Bonemeal> force = event -> {
            if (event.pos().equals(origin)) {
                event.setChance(1.0F);
            }
        };
        // after the flora system's own listener, so this answer is the last word
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, force);
        scene.onEnd(() -> NeoForge.EVENT_BUS.unregister(force));
        grows(scene, level, origin, FloraContent.DUSKCAP.get(), FloraContent.HUGE_DUSKCAP.get().shape(), FloraContent.DUSKCAP_BLOCK.get(), FloraContent.DUSKCAP_STEM.get(), "duskcap");
        grows(scene, level, origin, FloraContent.FESTERLING.get(), FloraContent.HUGE_FESTERLING.get().shape(), FloraContent.FESTERLING_BLOCK.get(), FloraContent.FESTERLING_STEM.get(), "festerling");
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theFeatureTypesAreTheModsOwnAndTheConfiguredFeaturesUseThem(TestScene scene) {
        ServerLevel level = scene.level();
        LayeredMushroomFeature duskcap = FloraContent.HUGE_DUSKCAP.get();
        scene.check("the configured huge duskcap is of the mod's feature", () -> scene.assertTrue(configured(level, FloraFeatures.HUGE_DUSKCAP).feature() == duskcap, "feature " + configured(level, FloraFeatures.HUGE_DUSKCAP).feature()));
        scene.check("and so is the huge festerling", () -> scene.assertTrue(configured(level, FloraFeatures.HUGE_FESTERLING).feature() == FloraContent.HUGE_FESTERLING.get(), "feature"));
        scene.succeed();
    }
}
