package net.sievert.jolcraft.gametest.module.system.flora;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.flora.FesterlingCropBlock;
import net.sievert.jolcraft.module.system.flora.FloraContent;

import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.FLOOR;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.at;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.put;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.tick;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.tickUntil;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.verdantSoil;

/**
 * The festerling crop against the real game: its growth on an upright log, its bonemeal, its drops and pick-block, and the
 * planting of rotten flesh on the top of a log by a real player through the server's own use path, with what a failed
 * planting falls back to. Each scene runs in a batch of its own, as the game puts the arenas of a batch six blocks apart. Randomness is driven in loops long enough that a failure would be a fault.
 */
public final class FesterlingScenes {

    private static final int LIMIT = 20_000;
    private static final int MANY = 2_000;

    private static FesterlingCropBlock crop() {
        return FloraContent.FESTERLING_CROP.get();
    }

    private static BlockState crop(int age) {
        return crop().defaultBlockState().setValue(FesterlingCropBlock.AGE, age);
    }

    private static int age(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(crop()) ? state.getValue(FesterlingCropBlock.AGE) : -1;
    }

    // ------------------------------------------------------------------ the crop

    @Scene(maxTicks = 20, isolated = true)
    public void theCropStandsOnUprightLogsAndVerdantSoilNotOnSideLogsOrStone(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell.below(), Blocks.OAK_LOG.defaultBlockState());
        scene.check("the crop stands on an upright log", () -> scene.assertTrue(crop(0).canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        scene.check("not on a log lying on its side", () -> scene.assertFalse(crop(0).canSurvive(level, cell), "survives"));
        put(level, cell.below(), verdantSoil());
        scene.check("on verdant soil", () -> scene.assertTrue(crop(0).canSurvive(level, cell), "does not survive"));
        put(level, cell.below(), Blocks.STONE.defaultBlockState());
        scene.check("and not on stone", () -> scene.assertFalse(crop(0).canSurvive(level, cell), "survives"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theCropOnALogGrowsToItsLastAgeAndTheNextGrowthTickMakesItAFesterling(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell.below(), Blocks.OAK_LOG.defaultBlockState());
        put(level, cell, crop(0));
        int used = tickUntil(level, cell, () -> age(level, cell) == FesterlingCropBlock.MAX_AGE, LIMIT);
        scene.check("random ticks bring the crop to its last age (after " + used + ")", () -> scene.assertEquals(FesterlingCropBlock.MAX_AGE, age(level, cell), "age"));
        tick(level, cell, 1);
        scene.check("the next tick makes it the festerling", () -> scene.assertTrue(level.getBlockState(cell).is(FloraContent.FESTERLING.get()), "state " + level.getBlockState(cell)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void bonemealOnTheCropAddsOneOrTwoAgesAndSucceedsTwoTimesInFive(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell.below(), Blocks.OAK_LOG.defaultBlockState());
        Set<Integer> added = new HashSet<>();
        for (int i = 0; i < MANY; i++) {
            put(level, cell, crop(0));
            crop().performBonemeal(level, level.getRandom(), cell, level.getBlockState(cell));
            added.add(age(level, cell));
        }
        scene.check("a bonemeal from age 0 lands on age 1 or 2, and both are seen", () -> scene.assertEquals(Set.of(1, 2), added, "ages reached"));
        put(level, cell, crop(2));
        crop().performBonemeal(level, level.getRandom(), cell, level.getBlockState(cell));
        scene.check("from age 2 it stops at the last age", () -> scene.assertEquals(FesterlingCropBlock.MAX_AGE, age(level, cell), "age"));
        scene.check("a crop at its last age is no bonemeal target, one below it is", () -> scene.assertTrue(!crop().isValidBonemealTarget(level, cell, crop(3))
                && crop().isValidBonemealTarget(level, cell, crop(2)), "targets"));
        int successes = 0;
        for (int i = 0; i < MANY; i++) {
            successes += crop().isBonemealSuccess(level, level.getRandom(), cell, crop(0)) ? 1 : 0;
        }
        int won = successes;
        scene.check("over " + MANY + " tries it succeeds about two in five (" + won + ")", () -> scene.assertTrue(won > MANY * 0.34 && won < MANY * 0.46, "successes " + won));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theCropDropsNothingBeforeItsLastAgeThenOneFesterlingAndPicksAsTheFesterling(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        for (int age = 0; age < FesterlingCropBlock.MAX_AGE; age++) {
            List<ItemStack> drops = Block.getDrops(crop(age), level, cell, null);
            int at = age;
            scene.check("at age " + at + " the crop drops nothing, the rotten flesh included", () -> scene.assertTrue(drops.isEmpty(), "drops " + drops));
        }
        List<ItemStack> mature = Block.getDrops(crop(FesterlingCropBlock.MAX_AGE), level, cell, null);
        scene.check("at its last age it drops exactly one festerling", () -> scene.assertTrue(mature.size() == 1 && mature.getFirst().is(FloraContent.FESTERLING_ITEM.get())
                && mature.getFirst().getCount() == 1, "drops " + mature));
        for (int age = 0; age <= FesterlingCropBlock.MAX_AGE; age++) {
            ItemStack picked = crop(age).getCloneItemStack(level, cell, false);
            int at = age;
            scene.check("at age " + at + " its pick-block is the festerling item", () -> scene.assertTrue(picked.is(FloraContent.FESTERLING_ITEM.get()), "picked " + picked));
        }
        scene.succeed();
    }

    // ------------------------------------------------------------------ planting

    private static void hold(ServerPlayer player, ItemStack stack) {
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    /** A click on the top face of the block, through the server's own use path, as the packet handler calls it. */
    private static InteractionResult clickTop(ServerPlayer player, BlockPos on) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(on).add(0.0, 0.5, 0.0), Direction.UP, on, false);
        return player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static int flesh(ServerPlayer player) {
        return player.getMainHandItem().is(Items.ROTTEN_FLESH) ? player.getMainHandItem().getCount() : 0;
    }

    private static int used(ServerPlayer player) {
        return player.getStats().getValue(Stats.ITEM_USED.get(Items.ROTTEN_FLESH));
    }

    @Scene(maxTicks = 20, isolated = true)
    public void rottenFleshPlantsTheCropOnTheTopOfAnUprightLogWithoutBeingEaten(TestScene scene) {
        ServerLevel level = scene.level();
        TestPlayer planter = scene.addPlayer("Planter").survival();
        ServerPlayer live = planter.player();
        live.getFoodData().setFoodLevel(10);
        BlockPos log = at(scene, 3, FLOOR, 3);
        put(level, log, Blocks.OAK_LOG.defaultBlockState());
        hold(live, new ItemStack(Items.ROTTEN_FLESH, 4));
        int usedBefore = used(live);

        InteractionResult result = clickTop(live, log);
        scene.check("the click is consumed", () -> scene.assertTrue(result.consumesAction(), "result " + result));
        scene.check("the festerling crop stands above the log, at age 0", () -> scene.assertEquals(0, age(level, log.above()), "age above the log"));
        scene.check("one rotten flesh is gone", () -> scene.assertEquals(3, flesh(live), "rotten flesh"));
        scene.check("the player began no eating", () -> scene.assertFalse(live.isUsingItem(), "using an item"));
        scene.check("the item's use statistic went up by one", () -> scene.assertEquals(usedBefore + 1, used(live), "uses of rotten flesh"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void rottenFleshOnTheTopOfALogOnItsSidePlantsNothingTakesNothingAndIsEatenInstead(TestScene scene) {
        ServerLevel level = scene.level();
        TestPlayer planter = scene.addPlayer("Refused").survival();
        ServerPlayer live = planter.player();
        live.getFoodData().setFoodLevel(10);
        BlockPos log = at(scene, 3, FLOOR, 3);
        put(level, log, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        hold(live, new ItemStack(Items.ROTTEN_FLESH, 4));
        int usedBefore = used(live);

        InteractionResult result = clickTop(live, log);
        scene.check("the click is not consumed", () -> scene.assertFalse(result.consumesAction(), "result " + result));
        scene.check("nothing is placed", () -> scene.assertTrue(level.getBlockState(log.above()).isAir(), "state " + level.getBlockState(log.above())));
        scene.check("nothing is taken", () -> scene.assertEquals(4, flesh(live), "rotten flesh"));
        scene.check("and no use is counted", () -> scene.assertEquals(usedBefore, used(live), "uses of rotten flesh"));
        live.gameMode.useItem(live, level, live.getMainHandItem(), InteractionHand.MAIN_HAND);
        scene.check("the game goes on to the item's own use: the player starts eating", () -> scene.assertTrue(live.isUsingItem(), "not using an item"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCreativePlayerPlantsWithoutLosingTheFlesh(TestScene scene) {
        ServerLevel level = scene.level();
        TestPlayer planter = scene.addPlayer("Maker");
        ServerPlayer live = planter.player();
        scene.check("a fresh player is in creative", () -> scene.assertTrue(live.hasInfiniteMaterials(), "not creative"));
        BlockPos log = at(scene, 3, FLOOR, 3);
        put(level, log, Blocks.OAK_LOG.defaultBlockState());
        hold(live, new ItemStack(Items.ROTTEN_FLESH, 1));

        InteractionResult result = clickTop(live, log);
        scene.check("the click is consumed and the crop stands above the log", () -> scene.assertTrue(result.consumesAction() && age(level, log.above()) == 0, "result " + result + ", age " + age(level, log.above())));
        scene.check("and the rotten flesh is still there", () -> scene.assertEquals(1, flesh(live), "rotten flesh"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aListenerThatRefusesThePlacementTakesTheCropBackAndNothingIsEaten(TestScene scene) {
        ServerLevel level = scene.level();
        TestPlayer planter = scene.addPlayer("Protected").survival();
        ServerPlayer live = planter.player();
        live.getFoodData().setFoodLevel(10);
        BlockPos log = at(scene, 3, FLOOR, 3);
        put(level, log, Blocks.OAK_LOG.defaultBlockState());
        hold(live, new ItemStack(Items.ROTTEN_FLESH, 4));
        int usedBefore = used(live);
        int[] asked = {0};
        Consumer<BlockEvent.EntityPlaceEvent> refuse = event -> {
            asked[0]++;
            event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, refuse);
        scene.onEnd(() -> NeoForge.EVENT_BUS.unregister(refuse));

        InteractionResult result = clickTop(live, log);
        scene.check("the entity-place event was posted once, for the player's block", () -> scene.assertEquals(1, asked[0], "events posted"));
        scene.check("the click fails", () -> scene.assertEquals(InteractionResult.FAIL, result, "result"));
        scene.check("the crop is taken back", () -> scene.assertTrue(level.getBlockState(log.above()).isAir(), "state " + level.getBlockState(log.above())));
        scene.check("nothing is taken and no use is counted", () -> scene.assertTrue(flesh(live) == 4 && used(live) == usedBefore, "flesh " + flesh(live) + ", uses " + used(live)));
        scene.check("and the player eats nothing", () -> scene.assertFalse(live.isUsingItem(), "using an item"));
        scene.succeed();
    }
}
