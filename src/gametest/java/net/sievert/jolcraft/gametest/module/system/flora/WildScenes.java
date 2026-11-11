package net.sievert.jolcraft.gametest.module.system.flora;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.flora.BloodrootBlock;
import net.sievert.jolcraft.module.system.flora.FloraContent;

import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.FLOOR;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.at;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.count;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.fill;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.openLight;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.put;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.putAndUpdate;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.verdantSoil;

/**
 * The wild plants against the real game: bloodroot under its ceilings and its bonemeal, the two blooms on their ground,
 * the four pots, and the festerling and the duskcap on theirs. Every scene does its work in its first tick and runs in a batch of its
 * own: the game puts the arenas of one batch six blocks apart, and these scenes build beyond that, so in a shared batch they would reach
 * into another scene's arena.
 */
public final class WildScenes {

    private static final int MANY = 500;
    /** The row of a root, three above the arena's floor, with its ceiling in the row above. */
    private static final int ROOT_ROW = FLOOR + 3;

    private static BlockState bloodroot() {
        return FloraContent.BLOODROOT.get().defaultBlockState();
    }

    private static BloodrootBlock root() {
        return FloraContent.BLOODROOT.get();
    }

    private static BlockState air() {
        return Blocks.AIR.defaultBlockState();
    }

    private static boolean isRoot(BlockState state) {
        return state.is(FloraContent.BLOODROOT.get());
    }

    // ------------------------------------------------------------------ bloodroot

    @Scene(maxTicks = 20, isolated = true)
    public void bloodrootHangsUnderStoneAndDirtNotUnderAirAndIsGoneWhenItsCeilingIs(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos underStone = at(scene, 3, ROOT_ROW, 3);
        BlockPos underDirt = at(scene, 6, ROOT_ROW, 3);
        BlockPos underAir = at(scene, 9, ROOT_ROW, 3);
        put(level, underStone.above(), Blocks.STONE.defaultBlockState());
        put(level, underDirt.above(), Blocks.DIRT.defaultBlockState());
        put(level, underAir.above(), air());
        scene.check("bloodroot stands under stone", () -> scene.assertTrue(bloodroot().canSurvive(level, underStone), "does not survive"));
        scene.check("and under dirt", () -> scene.assertTrue(bloodroot().canSurvive(level, underDirt), "does not survive"));
        scene.check("and not under air", () -> scene.assertFalse(bloodroot().canSurvive(level, underAir), "survives"));

        put(level, underStone, bloodroot());
        putAndUpdate(level, underStone.above(), air());
        scene.check("with its ceiling taken away and the neighbours told, the root is gone", () -> scene.assertTrue(level.getBlockState(underStone).isAir(), "state " + level.getBlockState(underStone)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aWaterloggedBloodrootHoldsWater(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, ROOT_ROW, 3);
        put(level, cell.above(), Blocks.STONE.defaultBlockState());
        BlockState wet = bloodroot().setValue(BlockStateProperties.WATERLOGGED, true);
        scene.check("the state's fluid is water", () -> scene.assertTrue(wet.getFluidState().is(Fluids.WATER), "fluid " + wet.getFluidState()));
        scene.check("the default state holds none", () -> scene.assertTrue(bloodroot().getFluidState().isEmpty(), "fluid " + bloodroot().getFluidState()));
        put(level, cell, wet);
        scene.check("and so does the root in the world", () -> scene.assertTrue(level.getFluidState(cell).is(Fluids.WATER), "fluid " + level.getFluidState(cell)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void bloodrootIsABonemealTargetUnderDirtWithAFreeNeighbourAndNotUnderStone(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos spreading = at(scene, 4, ROOT_ROW, 4);
        BlockPos stone = at(scene, 12, ROOT_ROW, 4);
        BlockPos lone = at(scene, 4, ROOT_ROW, 12);
        // a dirt ceiling over three by three cells, the root in the middle
        fill(level, spreading.offset(-1, 1, -1), spreading.offset(1, 1, 1), Blocks.DIRT.defaultBlockState());
        put(level, spreading, bloodroot());
        // dirt all around under a stone ceiling over the root itself
        fill(level, stone.offset(-1, 1, -1), stone.offset(1, 1, 1), Blocks.DIRT.defaultBlockState());
        put(level, stone.above(), Blocks.STONE.defaultBlockState());
        put(level, stone, bloodroot());
        // a root under dirt whose neighbours are all under stone
        fill(level, lone.offset(-1, 1, -1), lone.offset(1, 1, 1), Blocks.STONE.defaultBlockState());
        put(level, lone.above(), Blocks.DIRT.defaultBlockState());
        put(level, lone, bloodroot());
        scene.check("a root under dirt with a free neighbour under dirt is a bonemeal target", () -> scene.assertTrue(root().isValidBonemealTarget(level, spreading, level.getBlockState(spreading)), "not a target"));
        scene.check("a root under a stone ceiling is not", () -> scene.assertFalse(root().isValidBonemealTarget(level, stone, level.getBlockState(stone)), "a target"));
        scene.check("a root under dirt with no neighbour under dirt is not", () -> scene.assertFalse(root().isValidBonemealTarget(level, lone, level.getBlockState(lone)), "a target"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void bonemealOnBloodrootPutsExactlyOneNewRootBesideItUnderDirt(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos middle = at(scene, 4, ROOT_ROW, 4);
        BlockPos from = middle.offset(-1, 0, -1);
        BlockPos to = middle.offset(1, 0, 1);
        fill(level, middle.offset(-1, 1, -1), middle.offset(1, 1, 1), Blocks.DIRT.defaultBlockState());
        put(level, middle, bloodroot());
        root().performBonemeal(level, level.getRandom(), middle, level.getBlockState(middle));
        scene.check("the three by three has two roots now, the old one among them", () -> scene.assertTrue(count(level, from, to, WildScenes::isRoot) == 2 && isRoot(level.getBlockState(middle)),
                "roots " + count(level, from, to, WildScenes::isRoot)));
        BlockPos added = BlockPos.betweenClosedStream(from, to).filter(pos -> !pos.equals(middle) && isRoot(level.getBlockState(pos))).findFirst().orElseThrow().immutable();
        scene.check("the new root hangs under dirt", () -> scene.assertTrue(level.getBlockState(added.above()).is(Blocks.DIRT), "above " + level.getBlockState(added.above())));
        scene.check("and holds no water, where there was none", () -> scene.assertFalse(level.getBlockState(added).getValue(BlockStateProperties.WATERLOGGED), "waterlogged"));

        // the same with every neighbour water: the one root that is put is waterlogged
        BlockPos wet = at(scene, 4, ROOT_ROW, 12);
        fill(level, wet.offset(-1, 1, -1), wet.offset(1, 1, 1), Blocks.DIRT.defaultBlockState());
        fill(level, wet.offset(-1, 0, -1), wet.offset(1, 0, 1), Blocks.WATER.defaultBlockState());
        put(level, wet, bloodroot());
        root().performBonemeal(level, level.getRandom(), wet, level.getBlockState(wet));
        BlockPos wetAdded = BlockPos.betweenClosedStream(wet.offset(-1, 0, -1), wet.offset(1, 0, 1)).filter(pos -> !pos.equals(wet) && isRoot(level.getBlockState(pos))).findFirst().orElseThrow().immutable();
        scene.check("a root put into water is waterlogged", () -> scene.assertTrue(level.getBlockState(wetAdded).getValue(BlockStateProperties.WATERLOGGED), "state " + level.getBlockState(wetAdded)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void bloodrootBonemealSucceedsSometimesAndFailsMoreOften(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, ROOT_ROW, 3);
        RandomSource random = level.getRandom();
        int successes = 0;
        for (int i = 0; i < MANY; i++) {
            successes += root().isBonemealSuccess(level, random, cell, bloodroot()) ? 1 : 0;
        }
        int won = successes;
        scene.check("over five hundred tries the bonemeal succeeds sometimes (" + won + ")", () -> scene.assertTrue(won > 0, "never"));
        scene.check("and fails more often than it succeeds", () -> scene.assertTrue(won < MANY - won, "successes " + won + " of " + MANY));
        scene.succeed();
    }

    // ------------------------------------------------------------------ the blooms

    @Scene(maxTicks = 20, isolated = true)
    public void skybellStandsOnAGrassBlockAndNotOnStone(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        BlockState skybell = FloraContent.SKYBELL.get().defaultBlockState();
        put(level, cell.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        scene.check("a skybell stands on a grass block", () -> scene.assertTrue(skybell.canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), Blocks.STONE.defaultBlockState());
        scene.check("and not on stone", () -> scene.assertFalse(skybell.canSurvive(level, cell), "survives"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void cyanellaStandsOnWarpedNyliumAndVerdantSoilNotOnAGrassBlock(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        BlockState cyanella = FloraContent.CYANELLA.get().defaultBlockState();
        put(level, cell.below(), Blocks.WARPED_NYLIUM.defaultBlockState());
        scene.check("a cyanella stands on warped nylium", () -> scene.assertTrue(cyanella.canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), verdantSoil());
        scene.check("and on verdant soil", () -> scene.assertTrue(cyanella.canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        scene.check("and not on a grass block", () -> scene.assertFalse(cyanella.canSurvive(level, cell), "survives"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ the pots

    @Scene(maxTicks = 20, isolated = true)
    public void theEmptyPotKnowsTheFourPlantsAndEachPottedBlockHoldsItsPlantAndDropsPotAndPlant(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        Map<Identifier, Supplier<? extends Block>> full = ((FlowerPotBlock) Blocks.FLOWER_POT).getFullPotsView();
        record Pair(Block plant, Block pot, Item item) {
        }
        List<Pair> pairs = List.of(
                new Pair(FloraContent.SKYBELL.get(), FloraContent.POTTED_SKYBELL.get(), FloraContent.SKYBELL_ITEM.get()),
                new Pair(FloraContent.CYANELLA.get(), FloraContent.POTTED_CYANELLA.get(), FloraContent.CYANELLA_ITEM.get()),
                new Pair(FloraContent.DUSKCAP.get(), FloraContent.POTTED_DUSKCAP.get(), FloraContent.DUSKCAP_ITEM.get()),
                new Pair(FloraContent.FESTERLING.get(), FloraContent.POTTED_FESTERLING.get(), FloraContent.FESTERLING_ITEM.get()));
        for (Pair pair : pairs) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(pair.plant());
            Supplier<? extends Block> filled = full.get(id);
            scene.check("the empty pot maps " + id + " to the mod's potted block", () -> scene.assertTrue(filled != null && filled.get() == pair.pot(), "mapped to " + (filled == null ? null : filled.get())));
            scene.check(id + "'s potted block holds its plant", () -> scene.assertTrue(((FlowerPotBlock) pair.pot()).getPotted() == pair.plant(), "holds " + ((FlowerPotBlock) pair.pot()).getPotted()));
            List<ItemStack> drops = Block.getDrops(pair.pot().defaultBlockState(), level, cell, null);
            Set<Item> items = new HashSet<>();
            drops.forEach(stack -> items.add(stack.getItem()));
            scene.check("and drops a flower pot and the plant, one each", () -> scene.assertTrue(drops.size() == 2 && items.equals(Set.of(Items.FLOWER_POT, pair.item())), "drops " + drops));
        }
        scene.succeed();
    }

    // ------------------------------------------------------------------ the mushrooms' ground

    @Scene(maxTicks = 20, isolated = true)
    public void theFesterlingStandsInFullLightOnAnUprightLogVerdantSoilAndPodzolNotOnALogOnItsSideOrStone(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        BlockState festerling = FloraContent.FESTERLING.get().defaultBlockState();
        put(level, cell.below(), Blocks.OAK_LOG.defaultBlockState());
        scene.check("the cell is in the open light", () -> scene.assertTrue(openLight(level, cell), "raw brightness " + level.getRawBrightness(cell, 0)));
        scene.check("a festerling stands on the top of an upright log", () -> scene.assertTrue(festerling.canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        scene.check("and not on a log lying on its side", () -> scene.assertFalse(festerling.canSurvive(level, cell), "survives"));
        put(level, cell.below(), verdantSoil());
        scene.check("it stands on verdant soil", () -> scene.assertTrue(festerling.canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), Blocks.PODZOL.defaultBlockState());
        scene.check("and on podzol", () -> scene.assertTrue(festerling.canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), Blocks.STONE.defaultBlockState());
        scene.check("and not on stone", () -> scene.assertFalse(festerling.canSurvive(level, cell), "survives"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theDuskcapStandsOnVerdantSoilInFullLightAndNotOnAGrassBlock(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        BlockState duskcap = FloraContent.DUSKCAP.get().defaultBlockState();
        put(level, cell.below(), verdantSoil());
        scene.check("the cell is in the open light", () -> scene.assertTrue(openLight(level, cell), "raw brightness " + level.getRawBrightness(cell, 0)));
        scene.check("a duskcap stands on verdant soil in full light", () -> scene.assertTrue(duskcap.canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        scene.check("and not on a grass block in full light", () -> scene.assertFalse(duskcap.canSurvive(level, cell), "survives"));
        scene.succeed();
    }
}
