package net.sievert.jolcraft.gametest.module.system.flora;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.flora.BulbsBlock;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraHints;
import net.sievert.jolcraft.module.system.flora.FloraTags;
import net.sievert.jolcraft.module.system.flora.HopsBlock;
import net.sievert.jolcraft.module.system.flora.HopsVariety;

import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.FLOOR;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.assertDark;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.assertLit;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.at;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.dark;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.darkBox;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.inOrder;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.lit;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.moistFarmland;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.openBox;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.put;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.putAndUpdate;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.tick;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.tickUntil;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.verdantFarmland;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.verdantSoil;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.whenDark;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.withoutRandomTicks;

/**
 * The crops against the real game: barley, the four hops and the deepslate bulbs, on vanilla's farmland and the mod's
 * ground, in the light and in a closed box of stone. Randomness is either driven in a loop long enough that a failure
 * would be a fault, or the outcome is read over enough rolls to see every value it can take.
 */
public final class CropScenes {

    private static final int LIMIT = 20_000;
    private static final int MANY = 500;
    /** An absolute Y above the depth rule's limit, for the scene that needs one. */
    private static final int ABOVE_ZERO = 10;

    private static Block hops() {
        return FloraContent.ASGARNIAN_HOPS_CROP.get();
    }

    private static BlockState lower(int age) {
        return hops().defaultBlockState().setValue(HopsBlock.AGE, age);
    }

    private static BlockState upper(int age) {
        return lower(age).setValue(HopsBlock.HALF, DoubleBlockHalf.UPPER);
    }

    private static int hopsAge(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(hops()) ? state.getValue(HopsBlock.AGE) : -1;
    }

    /** A plant of the given age as the game grows it: the lower half and, from the age it is double, the upper above it. */
    private static void plant(ServerLevel level, BlockPos pos, int age) {
        put(level, pos, lower(age));
        put(level, pos.above(), HopsBlock.isDouble(age) ? upper(age) : Blocks.AIR.defaultBlockState());
    }

    private static long count(List<ItemStack> drops, Item item) {
        return drops.stream().filter(stack -> stack.is(item)).mapToLong(ItemStack::getCount).sum();
    }

    private static List<ItemStack> drops(ServerLevel level, BlockState state, BlockPos pos) {
        return Block.getDrops(state, level, pos, null);
    }

    // ------------------------------------------------------------------ barley

    @Scene(maxTicks = 20, isolated = true)
    public void barleyOnMoistFarmlandInTheLightGrowsToAgeSevenAndDropsBarleyThenSeeds(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell.below(), moistFarmland());
        BlockState barley = FloraContent.BARLEY_CROP.get().defaultBlockState();
        assertLit(scene, "the cell", cell);
        scene.check("barley survives on moist farmland in the light", () -> scene.assertTrue(barley.canSurvive(level, cell), "does not survive"));
        put(level, cell, barley);
        int used = tickUntil(level, cell, () -> level.getBlockState(cell).getValue(CropBlock.AGE) >= CropBlock.MAX_AGE, LIMIT);
        scene.check("it reaches age 7 when random-ticked", () -> scene.assertEquals(CropBlock.MAX_AGE, level.getBlockState(cell).getValue(CropBlock.AGE), "age after " + used + " ticks"));

        BlockState mature = FloraContent.BARLEY_CROP.get().getStateForAge(CropBlock.MAX_AGE);
        List<ItemStack> matureDrops = drops(level, mature, cell);
        scene.check("at age 7 its drops hold barley", () -> scene.assertTrue(count(matureDrops, FloraContent.BARLEY.get()) == 1, "drops " + matureDrops));
        List<ItemStack> youngDrops = drops(level, barley, cell);
        scene.check("at age 0 it drops exactly one barley seed", () -> scene.assertTrue(youngDrops.size() == 1 && youngDrops.getFirst().is(FloraContent.BARLEY_SEEDS.get())
                && youngDrops.getFirst().getCount() == 1, "drops " + youngDrops));
        scene.succeed();
    }

    // ------------------------------------------------------------------ hops growth

    @Scene(maxTicks = FloraArena.ONE_WAIT, isolated = true)
    public void aHopInTheDarkGrowsToItsLastAgeAndGainsItsUpperHalfExactlyAtAgeFive(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = darkBox(scene, 3, 3, moistFarmland(), 2);
        BlockPos top = cell.above();
        whenDark(scene, () -> {
            assertDark(scene, "the cell", cell);
            assertDark(scene, "the cell above it", top);
            scene.check("a hop survives on vanilla farmland in the dark", () -> scene.assertTrue(lower(0).canSurvive(level, cell), "does not survive"));
            put(level, cell, lower(0));
            boolean[] consistent = {true};
            int[] firstDouble = {-1};
            int used = tickUntil(level, cell, () -> {
                int age = hopsAge(level, cell);
                BlockState above = level.getBlockState(top);
                boolean isDouble = above.is(hops());
                if (isDouble && firstDouble[0] < 0) {
                    firstDouble[0] = age;
                }
                consistent[0] &= isDouble == HopsBlock.isDouble(age)
                        && (!isDouble || above.getValue(HopsBlock.AGE) == age && above.getValue(HopsBlock.HALF) == DoubleBlockHalf.UPPER);
                return age >= HopsBlock.MAX_AGE;
            }, LIMIT);
            scene.check("it reaches the last age, 9", () -> scene.assertEquals(HopsBlock.MAX_AGE, hopsAge(level, cell), "age after " + used + " ticks"));
            scene.check("the upper half appeared when the lower reached age 5", () -> scene.assertEquals(HopsBlock.DOUBLE_AGE, firstDouble[0], "age at the first upper half"));
            scene.check("after every random tick the upper half exists exactly from age 5 and carries the lower half's age", () -> scene.assertTrue(consistent[0], "an age and an upper half disagreed"));
            scene.succeed();
        }, cell, top);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aHopInTheLightDoesNotSurviveOnVanillaFarmlandAndGrowsOnVerdantFarmland(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos plain = at(scene, 3, FLOOR + 1, 3);
        BlockPos verdant = at(scene, 9, FLOOR + 1, 3);
        put(level, plain.below(), moistFarmland());
        put(level, verdant.below(), verdantFarmland());
        assertLit(scene, "the cell over vanilla farmland", plain);
        assertLit(scene, "the cell over verdant farmland", verdant);
        scene.check("a hop in the light on vanilla farmland does not survive", () -> scene.assertFalse(lower(0).canSurvive(level, plain), "survives"));
        scene.check("on verdant farmland in the light it survives", () -> scene.assertTrue(lower(0).canSurvive(level, verdant), "does not survive"));
        put(level, verdant, lower(0));
        int used = tickUntil(level, verdant, () -> hopsAge(level, verdant) >= HopsBlock.MAX_AGE, LIMIT);
        scene.check("and grows to its last age when random-ticked", () -> scene.assertEquals(HopsBlock.MAX_AGE, hopsAge(level, verdant), "age after " + used + " ticks"));
        scene.check("with an upper half above", () -> scene.assertEquals(upper(HopsBlock.MAX_AGE), level.getBlockState(verdant.above()), "block above"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aHopWithABlockDirectlyAboveItDoesNotGrow(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell.below(), verdantFarmland());
        put(level, cell, lower(0));
        tick(level, cell, MANY);
        scene.check("with room above, the same five hundred random ticks grow it", () -> scene.assertTrue(hopsAge(level, cell) > 0, "age " + hopsAge(level, cell)));

        put(level, cell, Blocks.AIR.defaultBlockState());
        put(level, cell.above(), Blocks.AIR.defaultBlockState());
        put(level, cell, lower(0));
        put(level, cell.above(), Blocks.STONE.defaultBlockState());
        tick(level, cell, MANY);
        scene.check("with a block directly above it, it is still at age 0 after five hundred random ticks", () -> scene.assertEquals(0, hopsAge(level, cell), "age"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void bonemealOnAHopAddsTwoToFiveAgesAndPlacesTheUpperHalfFromAgeFive(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell.below(), verdantFarmland());
        HopsBlock block = (HopsBlock) hops();
        Set<Integer> reached = new HashSet<>();
        boolean[] upperAgrees = {true};
        for (int i = 0; i < 200; i++) {
            plant(level, cell, 0);
            BlockState state = level.getBlockState(cell);
            if (!block.isValidBonemealTarget(level, cell, state)) {
                scene.fail("a young hop is no bonemeal target");
            }
            block.performBonemeal(level, level.getRandom(), cell, state);
            int age = hopsAge(level, cell);
            reached.add(age);
            BlockState above = level.getBlockState(cell.above());
            upperAgrees[0] &= HopsBlock.isDouble(age) ? above.equals(upper(age)) : above.isAir();
        }
        scene.check("two hundred bonemeals from age 0 land on 2, 3, 4 and 5 and nowhere else", () -> scene.assertEquals(Set.of(2, 3, 4, 5), reached, "ages reached"));
        scene.check("the upper half is there exactly when the age is 5 or more, carrying it", () -> scene.assertTrue(upperAgrees[0], "an upper half and an age disagreed"));

        plant(level, cell, HopsBlock.MAX_AGE);
        scene.check("at the last age the lower half is no target", () -> scene.assertFalse(block.isValidBonemealTarget(level, cell, level.getBlockState(cell)), "valid"));
        scene.check("and neither is the upper half", () -> scene.assertFalse(block.isValidBonemealTarget(level, cell.above(), level.getBlockState(cell.above())), "valid"));

        plant(level, cell, HopsBlock.DOUBLE_AGE);
        BlockPos topPos = cell.above();
        block.performBonemeal(level, level.getRandom(), topPos, level.getBlockState(topPos));
        scene.check("bonemeal from the upper half grows the lower", () -> scene.assertTrue(hopsAge(level, cell) > HopsBlock.DOUBLE_AGE, "age " + hopsAge(level, cell)));
        scene.check("and the upper half follows", () -> scene.assertEquals(upper(hopsAge(level, cell)), level.getBlockState(topPos), "upper half"));
        scene.succeed();
    }

    @Scene(maxTicks = FloraArena.TWO_WAITS, isolated = true)
    public void aHopLitAfterItWasPlantedStaysThroughRandomTicksAndIsGoneAfterANeighbourChange(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = darkBox(scene, 3, 3, moistFarmland(), 2);
        withoutRandomTicks(scene);
        inOrder(scene, dark(scene, () -> {
            assertDark(scene, "the cell", cell);
            put(level, cell, lower(0));
            scene.check("the hop stands in the dark", () -> scene.assertEquals(0, hopsAge(level, cell), "age"));
            openBox(scene, 3, 3, 2);
        }, cell, cell.above()), lit(scene, () -> {
            assertLit(scene, "the cell", cell);
            tick(level, cell, 100);
            scene.check("the lit hop is still there after random ticks", () -> scene.assertEquals(0, hopsAge(level, cell), "age"));
            putAndUpdate(level, cell.east(), Blocks.STONE.defaultBlockState());
            scene.check("after a neighbour changes, it is gone", () -> scene.assertTrue(level.getBlockState(cell).isAir(), "block " + level.getBlockState(cell)));
            scene.succeed();
        }, cell));
    }

    // ------------------------------------------------------------------ hops drops

    @Scene(maxTicks = 20, isolated = true)
    public void hopsDropAsTheTallFlowersDoFromEitherHalfAndNothingWhenTheOtherIsAbsent(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        Item seeds = FloraContent.ASGARNIAN_SEEDS.get();
        Item hopsItem = FloraContent.ASGARNIAN_HOPS.get();
        put(level, cell.below(), verdantFarmland());

        plant(level, cell, 2);
        List<ItemStack> single = drops(level, lower(2), cell);
        scene.check("a single plant gives exactly one seed", () -> scene.assertTrue(single.size() == 1 && single.getFirst().is(seeds) && single.getFirst().getCount() == 1, "drops " + single));

        plant(level, cell, HopsBlock.MAX_AGE);
        Set<Long> seedCounts = new HashSet<>();
        boolean[] shape = {true};
        for (int i = 0; i < MANY; i++) {
            for (BlockState half : List.of(lower(HopsBlock.MAX_AGE), upper(HopsBlock.MAX_AGE))) {
                BlockPos at = half.getValue(HopsBlock.HALF) == DoubleBlockHalf.LOWER ? cell : cell.above();
                List<ItemStack> given = drops(level, half, at);
                long seedsGiven = count(given, seeds);
                seedCounts.add(seedsGiven);
                shape[0] &= count(given, hopsItem) == 1 && (seedsGiven == 1 || seedsGiven == 2);
            }
        }
        scene.check("a mature double plant gives one hops and one or two seeds from either half, five hundred times each", () -> scene.assertTrue(shape[0], "a roll gave another amount"));
        scene.check("and both one and two seeds were seen", () -> scene.assertEquals(Set.of(1L, 2L), seedCounts, "seed counts seen"));

        put(level, cell.above(), Blocks.AIR.defaultBlockState());
        List<ItemStack> lowerAlone = drops(level, lower(HopsBlock.MAX_AGE), cell);
        scene.check("the lower half with the upper absent gives nothing", () -> scene.assertTrue(lowerAlone.isEmpty(), "drops " + lowerAlone));
        put(level, cell, Blocks.AIR.defaultBlockState());
        put(level, cell.above(), upper(HopsBlock.MAX_AGE));
        List<ItemStack> upperAlone = drops(level, upper(HopsBlock.MAX_AGE), cell.above());
        scene.check("the upper half with the lower absent gives nothing", () -> scene.assertTrue(upperAlone.isEmpty(), "drops " + upperAlone));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void destroyingEitherHalfOfAMaturePlantRemovesBothAndLeavesOneHopsItem(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell.below(), verdantFarmland());
        Item hopsItem = FloraContent.ASGARNIAN_HOPS.get();
        AABB around = new AABB(cell).inflate(3.0);
        for (BlockPos broken : List.of(cell, cell.above())) {
            level.getEntitiesOfClass(ItemEntity.class, around).forEach(ItemEntity::discard);
            plant(level, cell, HopsBlock.MAX_AGE);
            level.destroyBlock(broken, true);
            String half = broken.equals(cell) ? "lower" : "upper";
            scene.check("destroying the " + half + " half removes both blocks", () -> scene.assertTrue(level.getBlockState(cell).isAir() && level.getBlockState(cell.above()).isAir(),
                    "blocks " + level.getBlockState(cell) + " and " + level.getBlockState(cell.above())));
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, around);
            long hopsFound = items.stream().filter(entity -> entity.getItem().is(hopsItem)).count();
            long hopsTotal = items.stream().filter(entity -> entity.getItem().is(hopsItem)).mapToLong(entity -> entity.getItem().getCount()).sum();
            scene.check("and leaves exactly one hops item entity (" + half + " half)", () -> scene.assertTrue(hopsFound == 1 && hopsTotal == 1, "hops entities " + hopsFound + " holding " + hopsTotal));
        }
        scene.succeed();
    }

    // ------------------------------------------------------------------ bulbs

    @Scene(maxTicks = 20, isolated = true)
    public void bulbsOnVerdantSoilInTheLightSurviveAndGrowAndNeedNoStoneNoBonemeal(TestScene scene) {
        ServerLevel level = scene.level();
        BulbsBlock block = FloraContent.DEEPSLATE_BULBS_CROP.get();
        BlockPos onSoil = at(scene, 3, FLOOR + 1, 3);
        BlockPos onStone = at(scene, 9, FLOOR + 1, 3);
        put(level, onSoil.below(), verdantSoil());
        put(level, onStone.below(), Blocks.STONE.defaultBlockState());
        assertLit(scene, "the cell over the soil", onSoil);
        BlockState seed = block.defaultBlockState();
        scene.check("a bulb on verdant soil in the light survives", () -> scene.assertTrue(seed.canSurvive(level, onSoil), "does not survive"));
        scene.check("a bulb on stone does not", () -> scene.assertFalse(seed.canSurvive(level, onStone), "survives"));
        put(level, onSoil, seed);
        int used = tickUntil(level, onSoil, () -> level.getBlockState(onSoil).getValue(BulbsBlock.AGE) >= BulbsBlock.MAX_AGE, LIMIT);
        scene.check("it grows to age 9 when random-ticked, in the light", () -> scene.assertEquals(BulbsBlock.MAX_AGE, level.getBlockState(onSoil).getValue(BulbsBlock.AGE), "age after " + used + " ticks"));
        scene.check("it is breakable by no random tick: it is still there", () -> scene.assertTrue(level.getBlockState(onSoil).is(block), "block " + level.getBlockState(onSoil)));
        scene.check("it is no bonemeal target", () -> scene.assertFalse(block.isValidBonemealTarget(level, onSoil, seed), "valid"));
        scene.succeed();
    }

    @Scene(maxTicks = FloraArena.TWO_WAITS, isolated = true)
    public void bulbsOnDeepslateFollowTheDepthRuleInTheDarkAndNeverSurviveInTheLight(TestScene scene) {
        ServerLevel level = scene.level();
        BulbsBlock block = FloraContent.DEEPSLATE_BULBS_CROP.get();
        BlockPos cell = darkBox(scene, 3, 3, Blocks.DEEPSLATE.defaultBlockState());
        BlockState seed = block.defaultBlockState();
        inOrder(scene, dark(scene, () -> {
            assertDark(scene, "the cell", cell);
            int y = cell.getY();
            boolean deep = BulbsBlock.deepEnough(y);
            scene.log().info("the arena's plant row is at Y {}: {} of the depth rule", y, deep ? "the Y 0 or below half" : "the above Y 0 half");
            scene.check("on deepslate in the dark at Y " + y + " a bulb " + (deep ? "survives" : "does not survive, being above Y 0"),
                    () -> scene.assertEquals(deep, seed.canSurvive(level, cell), "survival"));
            if (deep) {
                put(level, cell, seed);
                int used = tickUntil(level, cell, () -> level.getBlockState(cell).getValue(BulbsBlock.AGE) >= 1, LIMIT);
                scene.check("it grows when random-ticked in the dark", () -> scene.assertTrue(level.getBlockState(cell).getValue(BulbsBlock.AGE) >= 1, "no growth in " + used + " ticks"));
            }
            openBox(scene, 3, 3, 1);
        }, cell), lit(scene, () -> {
            assertLit(scene, "the cell", cell);
            scene.check("on deepslate in the light a bulb does not survive", () -> scene.assertFalse(seed.canSurvive(level, cell), "survives"));
            scene.succeed();
        }, cell));
    }

    @Scene(maxTicks = FloraArena.ONE_WAIT, isolated = true)
    public void bulbsAboveYZeroDoNotSurviveOnDeepslateInTheDarkButDoOnVerdantSoil(TestScene scene) {
        ServerLevel level = scene.level();
        // the arena lies far below Y 0, so these boxes are built on arena rows outside the structure and taken away again
        int floor = ABOVE_ZERO - scene.origin().getY() - 1;
        BlockPos deepslate = darkBox(scene, 3, 3, Blocks.DEEPSLATE.defaultBlockState(), 1, floor);
        BlockPos soil = darkBox(scene, 9, 3, verdantSoil(), 1, floor);
        scene.onEnd(() -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        put(level, at(scene, 3 + dx, floor + dy, 3 + dz), Blocks.AIR.defaultBlockState());
                        put(level, at(scene, 9 + dx, floor + dy, 3 + dz), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        });
        whenDark(scene, () -> {
            assertDark(scene, "the box over deepslate", deepslate);
            assertDark(scene, "the box over verdant soil", soil);
            BlockState seed = FloraContent.DEEPSLATE_BULBS_CROP.get().defaultBlockState();
            scene.check("the boxes are at Y " + deepslate.getY() + ", above Y 0", () -> scene.assertFalse(BulbsBlock.deepEnough(deepslate.getY()), "deep enough"));
            scene.check("a bulb on deepslate in the dark above Y 0 does not survive", () -> scene.assertFalse(seed.canSurvive(level, deepslate), "survives"));
            scene.check("a bulb on verdant soil in the dark above Y 0 survives", () -> scene.assertTrue(seed.canSurvive(level, soil), "does not survive"));
            scene.succeed();
        }, deepslate, soil);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void bulbsDropOneWhenYoungAndTwoOrThreeWhenMature(TestScene scene) {
        ServerLevel level = scene.level();
        BulbsBlock block = FloraContent.DEEPSLATE_BULBS_CROP.get();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        Item bulbs = FloraContent.DEEPSLATE_BULBS.get();
        List<ItemStack> young = drops(level, block.getStateForAge(3), cell);
        scene.check("at age 3 a bulb drops exactly one bulbs", () -> scene.assertTrue(young.size() == 1 && young.getFirst().is(bulbs) && young.getFirst().getCount() == 1, "drops " + young));
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < MANY; i++) {
            seen.add(count(drops(level, block.getStateForAge(BulbsBlock.MAX_AGE), cell), bulbs));
        }
        scene.check("at age 9 five hundred rolls give two or three bulbs, and both are seen", () -> scene.assertEquals(Set.of(2L, 3L), seen, "amounts seen"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ barley seeds from grass

    @Scene(maxTicks = 20, isolated = true)
    public void grassGivesBarleySeedsUnlessShearedAndTallGrassToo(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        Item seeds = FloraContent.BARLEY_SEEDS.get();
        ItemStack shears = new ItemStack(Items.SHEARS);
        long shortBare = 0;
        long shortSheared = 0;
        long tallBare = 0;
        for (int i = 0; i < MANY; i++) {
            shortBare += count(Block.getDrops(Blocks.SHORT_GRASS.defaultBlockState(), level, cell, null), seeds);
            shortSheared += count(Block.getDrops(Blocks.SHORT_GRASS.defaultBlockState(), level, cell, null, null, shears), seeds);
            tallBare += count(Block.getDrops(Blocks.TALL_GRASS.defaultBlockState(), level, cell, null), seeds);
        }
        long found = shortBare;
        scene.check("short grass broken with an empty hand five hundred times gives barley seeds (" + found + ")", () -> scene.assertTrue(found >= 1, "none in five hundred"));
        long foundSheared = shortSheared;
        scene.check("with shears five hundred times it gives none", () -> scene.assertEquals(0L, foundSheared, "barley seeds"));
        long foundTall = tallBare;
        scene.check("tall grass without shears five hundred times gives barley seeds (" + foundTall + ")", () -> scene.assertTrue(foundTall >= 1, "none in five hundred"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ items and data

    private static ResourceKey<Recipe<?>> recipe(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("jolcraft", path));
    }

    private static void hint(TestScene scene, Item item, String key) {
        ItemLore lore = new ItemStack(item).get(DataComponents.LORE);
        scene.check(item + " carries one lore line, the hint " + key, () -> scene.assertTrue(lore != null && lore.lines().size() == 1
                && lore.lines().getFirst().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key), "lore " + lore));
        Component styled = lore.styledLines().getFirst();
        scene.check("the line is gray and not italic even among lore's own purple italics", () -> scene.assertTrue(!styled.getStyle().isItalic()
                && styled.getStyle().getColor() != null && styled.getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.GRAY)), "style " + styled.getStyle()));
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theSeedsAndTheBulbsCarryTheirHintsAsLore(TestScene scene) {
        for (HopsVariety variety : HopsVariety.values()) {
            hint(scene, FloraContent.seeds(variety).get(), FloraHints.HOPS_SEEDS.key());
        }
        hint(scene, FloraContent.DEEPSLATE_BULBS.get(), FloraHints.DEEPSLATE_BULBS.key());
        scene.check("barley seeds carry no lore", () -> scene.assertTrue(new ItemStack(FloraContent.BARLEY_SEEDS.get()).getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().isEmpty(), "lore"));
        scene.succeed();
    }

    private static float compost(Item item) {
        Compostable chance = BuiltInRegistries.ITEM.getData(NeoForgeDataMaps.COMPOSTABLES,
                BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
        return chance == null ? -1.0F : chance.chance();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theCompostChancesAreQuarterForSeedsAndJustUnderHalfForHarvestAndTheHayBalesForTheBlock(TestScene scene) {
        scene.check("barley seeds and a hops seed compost at 0.25", () -> scene.assertTrue(compost(FloraContent.BARLEY_SEEDS.get()) == 0.25F
                && compost(FloraContent.KRANDONIAN_SEEDS.get()) == 0.25F, "chances " + compost(FloraContent.BARLEY_SEEDS.get()) + " " + compost(FloraContent.KRANDONIAN_SEEDS.get())));
        scene.check("barley and a hops compost at 0.45", () -> scene.assertTrue(compost(FloraContent.BARLEY.get()) == 0.45F && compost(FloraContent.YANILLIAN_HOPS.get()) == 0.45F,
                "chances " + compost(FloraContent.BARLEY.get()) + " " + compost(FloraContent.YANILLIAN_HOPS.get())));
        scene.check("the barley block composts at the hay bale's 0.85", () -> scene.assertEquals(compost(Items.HAY_BLOCK), compost(FloraContent.BARLEY_BLOCK_ITEM.get()), "chance"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theThreeRecipesAreInTheLevelWithTheirResults(TestScene scene) {
        ServerLevel level = scene.level();
        ItemStack barley = new ItemStack(FloraContent.BARLEY.get());
        RecipeHolder<?> bread = level.recipeAccess().byKey(recipe("bread_from_barley")).orElseThrow();
        CraftingInput row = CraftingInput.of(3, 1, List.of(barley, barley, barley));
        scene.check("three barley in a row make bread", () -> scene.assertTrue(((CraftingRecipe) bread.value()).matches(row, level)
                && ((CraftingRecipe) bread.value()).assemble(row).is(Items.BREAD), "recipe " + bread));
        RecipeHolder<?> block = level.recipeAccess().byKey(recipe("barley_block_from_barley")).orElseThrow();
        CraftingInput nine = CraftingInput.of(3, 3, Collections.nCopies(9, barley));
        scene.check("nine barley make a barley block", () -> scene.assertTrue(((CraftingRecipe) block.value()).matches(nine, level)
                && ((CraftingRecipe) block.value()).assemble(nine).is(FloraContent.BARLEY_BLOCK_ITEM.get()), "recipe " + block));
        RecipeHolder<?> back = level.recipeAccess().byKey(recipe("barley_from_barley_block")).orElseThrow();
        CraftingInput one = CraftingInput.of(1, 1, List.of(new ItemStack(FloraContent.BARLEY_BLOCK_ITEM.get())));
        ItemStack made = ((CraftingRecipe) back.value()).assemble(one);
        scene.check("a barley block makes nine barley", () -> scene.assertTrue(((CraftingRecipe) back.value()).matches(one, level) && made.is(FloraContent.BARLEY.get()) && made.getCount() == 9,
                "made " + made));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theCropsAreInTheirTagsAndTheSeedsInTheCommonSeedsThroughTheNestedTag(TestScene scene) {
        BlockState bulbs = FloraContent.DEEPSLATE_BULBS_CROP.get().defaultBlockState();
        scene.check("barley, a hops block and the bulbs are in crops", () -> scene.assertTrue(FloraContent.BARLEY_CROP.get().defaultBlockState().is(BlockTags.CROPS)
                && FloraContent.DUSKHOLD_HOPS_CROP.get().defaultBlockState().is(BlockTags.CROPS) && bulbs.is(BlockTags.CROPS), "tags"));
        scene.check("a hops block keeps its farmland", () -> scene.assertTrue(FloraContent.YANILLIAN_HOPS_CROP.get().defaultBlockState().is(BlockTags.MAINTAINS_FARMLAND), "tag"));
        scene.check("the bulbs need an iron tool", () -> scene.assertTrue(bulbs.is(BlockTags.NEEDS_IRON_TOOL) && bulbs.is(BlockTags.MINEABLE_WITH_PICKAXE), "tags"));
        scene.check("the bulbs stand on deepslate and verdant soil and not on tuff", () -> scene.assertTrue(Blocks.DEEPSLATE.defaultBlockState().is(FloraTags.SUPPORTS_DEEPSLATE_BULBS)
                && verdantSoil().is(FloraTags.SUPPORTS_DEEPSLATE_BULBS) && !Blocks.TUFF.defaultBlockState().is(FloraTags.SUPPORTS_DEEPSLATE_BULBS), "tags"));
        ItemStack seed = new ItemStack(FloraContent.DUSKHOLD_SEEDS.get());
        scene.check("a hops seed is in jolcraft:hops_seeds and, through it, in c:seeds", () -> scene.assertTrue(seed.is(FloraTags.HOPS_SEEDS) && seed.is(Tags.Items.SEEDS), "tags"));
        scene.check("a hops is in jolcraft:hops and, through it, in c:crops", () -> scene.assertTrue(new ItemStack(FloraContent.KRANDONIAN_HOPS.get()).is(FloraTags.HOPS)
                && new ItemStack(FloraContent.KRANDONIAN_HOPS.get()).is(Tags.Items.CROPS), "tags"));
        scene.check("the bulbs are crops and raw materials, barley seeds are seeds", () -> scene.assertTrue(new ItemStack(FloraContent.DEEPSLATE_BULBS.get()).is(Tags.Items.CROPS)
                && new ItemStack(FloraContent.DEEPSLATE_BULBS.get()).is(Tags.Items.RAW_MATERIALS) && new ItemStack(FloraContent.BARLEY_SEEDS.get()).is(Tags.Items.SEEDS), "tags"));
        scene.succeed();
    }
}
