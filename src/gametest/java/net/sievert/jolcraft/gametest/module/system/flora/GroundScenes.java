package net.sievert.jolcraft.gametest.module.system.flora;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraTags;
import net.sievert.jolcraft.module.system.flora.GrowthEvent;

import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.FLOOR;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.assertDark;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.at;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.darkBox;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.moistFarmland;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.put;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.tick;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.verdantFarmland;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.verdantSoil;
import static net.sievert.jolcraft.gametest.module.system.flora.FloraArena.whenDark;

/**
 * The verdant ground against the real game: the crops and mushrooms of vanilla on verdant farmland and soil, through
 * the mixins, the events and the flora system's listeners. A scene that needs darkness builds a closed box of stone
 * around the plant and waits until the light is really gone before it relies on it. Randomness is either driven in
 * a loop long enough that a failure to grow would be a fault, or forced by a listener to 0 or 1.
 */
public final class GroundScenes {

    private static int age(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
    }

    // ------------------------------------------------------------------ crops

    @Scene(maxTicks = FloraArena.ONE_WAIT, isolated = true)
    public void wheatInTheDarkSurvivesAndGrowsOnVerdantFarmlandAndNotOnPlain(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos verdant = darkBox(scene, 3, 3, verdantFarmland());
        BlockPos plain = darkBox(scene, 9, 3, moistFarmland());
        whenDark(scene, () -> {
            assertDark(scene, "the box over verdant farmland", verdant);
            assertDark(scene, "the box over plain farmland", plain);
            BlockState wheat = Blocks.WHEAT.defaultBlockState();
            scene.check("wheat on plain farmland in the dark does not survive", () -> scene.assertFalse(wheat.canSurvive(level, plain), "survives"));
            scene.check("wheat on verdant farmland in the dark survives", () -> scene.assertTrue(wheat.canSurvive(level, verdant), "does not survive"));
            put(level, verdant, wheat);
            tick(level, verdant, 400);
            scene.check("and grows when random-ticked", () -> scene.assertTrue(age(level, verdant) > 0, "age " + age(level, verdant)));
            scene.succeed();
        }, verdant, plain);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void growthSpeedOnVerdantFarmlandIsOneAndAHalfTimesMoistFarmland(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos verdant = at(scene, 3, FLOOR + 1, 3);
        BlockPos plain = at(scene, 9, FLOOR + 1, 9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                put(level, verdant.offset(dx, -1, dz), verdantFarmland());
                put(level, plain.offset(dx, -1, dz), moistFarmland());
            }
        }
        BlockState wheat = Blocks.WHEAT.defaultBlockState();
        float onPlain = CropBlock.getGrowthSpeed(wheat, level, plain);
        float onVerdant = CropBlock.getGrowthSpeed(wheat, level, verdant);
        scene.check("wheat's speed on moist farmland is the game's own, above 1", () -> scene.assertTrue(onPlain > 1.0F, "speed " + onPlain));
        scene.check("on verdant farmland in the same surroundings it is 1.5 times that", () -> scene.assertTrue(Math.abs(onVerdant - 1.5F * onPlain) < 1.0E-4F,
                "verdant " + onVerdant + " plain " + onPlain));
        scene.succeed();
    }

    @Scene(maxTicks = FloraArena.ONE_WAIT, isolated = true)
    public void aPitcherCropInTheDarkGrowsOnVerdantFarmland(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = darkBox(scene, 3, 3, verdantFarmland());
        whenDark(scene, () -> {
            assertDark(scene, "the box", cell);
            BlockState pitcher = Blocks.PITCHER_CROP.defaultBlockState();
            scene.check("a pitcher crop stands on verdant farmland in the dark", () -> scene.assertTrue(pitcher.canSurvive(level, cell), "does not survive"));
            put(level, cell, pitcher);
            tick(level, cell, 400);
            scene.check("and gains age when random-ticked", () -> scene.assertTrue(level.getBlockState(cell).is(Blocks.PITCHER_CROP)
                    && level.getBlockState(cell).getValue(PitcherCropBlock.AGE) > 0, "state " + level.getBlockState(cell)));
            scene.succeed();
        }, cell);
    }

    // ------------------------------------------------------------------ soil

    @Scene(maxTicks = 20, isolated = true)
    public void verdantSoilHoldsMushroomsAndFlowersAndNoCrop(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos cell = at(scene, 3, FLOOR + 1, 3);
        put(level, cell.below(), verdantSoil());
        scene.check("the soil is in the open light", () -> scene.assertTrue(level.getRawBrightness(cell, 0) >= 13, "raw brightness " + level.getRawBrightness(cell, 0)));
        scene.check("a brown mushroom stands on verdant soil in full light", () -> scene.assertTrue(Blocks.BROWN_MUSHROOM.defaultBlockState().canSurvive(level, cell), "does not survive"));
        scene.check("a poppy stands on verdant soil", () -> scene.assertTrue(Blocks.POPPY.defaultBlockState().canSurvive(level, cell), "does not survive"));
        scene.check("wheat does not stand on verdant soil", () -> scene.assertFalse(Blocks.WHEAT.defaultBlockState().canSurvive(level, cell), "survives"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theVerdantNumbersComeThroughTheEvents(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos onSoil = at(scene, 3, FLOOR + 1, 3);
        BlockPos onDirt = at(scene, 6, FLOOR + 1, 3);
        put(level, onSoil.below(), verdantSoil());
        put(level, onDirt.below(), Blocks.DIRT.defaultBlockState());
        GrowthEvent.Spread soilSpread = NeoForge.EVENT_BUS.post(new GrowthEvent.Spread(level, onSoil));
        GrowthEvent.Bonemeal soilBonemeal = NeoForge.EVENT_BUS.post(new GrowthEvent.Bonemeal(level, onSoil));
        GrowthEvent.Spread dirtSpread = NeoForge.EVENT_BUS.post(new GrowthEvent.Spread(level, onDirt));
        GrowthEvent.Bonemeal dirtBonemeal = NeoForge.EVENT_BUS.post(new GrowthEvent.Bonemeal(level, onDirt));
        scene.check("over verdant soil a mushroom spreads at 0.06", () -> scene.assertEquals(0.06F, soilSpread.chance(), "spread chance"));
        scene.check("and grows huge from bonemeal at 0.6", () -> scene.assertEquals(0.6F, soilBonemeal.chance(), "bonemeal chance"));
        scene.check("over dirt it spreads at the game's 1 in 25", () -> scene.assertEquals(1.0F / 25.0F, dirtSpread.chance(), "spread chance"));
        scene.check("and grows huge at the game's 0.4", () -> scene.assertEquals(0.4F, dirtBonemeal.chance(), "bonemeal chance"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theMushroomMixinAnswersWhatTheListenersSay(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos marker = at(scene, 6, FLOOR + 1, 6);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                put(level, marker.offset(dx, -1, dz), verdantSoil());
            }
        }
        float[] chance = {1.0F};
        Consumer<GrowthEvent.Bonemeal> bonemeal = event -> {
            if (event.pos().equals(marker)) {
                event.setChance(chance[0]);
            }
        };
        Consumer<GrowthEvent.Spread> spread = event -> {
            if (event.pos().equals(marker)) {
                event.setChance(chance[0]);
            }
        };
        // after the flora system's own listener, so the marker's answer is the last word
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, bonemeal);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, spread);
        scene.onEnd(() -> {
            NeoForge.EVENT_BUS.unregister(bonemeal);
            NeoForge.EVENT_BUS.unregister(spread);
        });
        MushroomBlock mushroom = (MushroomBlock) Blocks.BROWN_MUSHROOM;
        BlockState state = mushroom.defaultBlockState();

        chance[0] = 1.0F;
        int succeeded = 0;
        for (int i = 0; i < 100; i++) {
            succeeded += mushroom.isBonemealSuccess(level, level.getRandom(), marker, state) ? 1 : 0;
        }
        int won = succeeded;
        scene.check("with the bonemeal chance at 1, a hundred of a hundred succeed", () -> scene.assertEquals(100, won, "successes"));
        chance[0] = 0.0F;
        succeeded = 0;
        for (int i = 0; i < 100; i++) {
            succeeded += mushroom.isBonemealSuccess(level, level.getRandom(), marker, state) ? 1 : 0;
        }
        int lost = succeeded;
        scene.check("with it at 0, none of a hundred succeed", () -> scene.assertEquals(0, lost, "successes"));

        chance[0] = 1.0F;
        put(level, marker, state);
        int ticks = 0;
        while (ticks < 60 && mushrooms(level, marker) < 2) {
            level.getBlockState(marker).randomTick(level, marker, level.getRandom());
            ticks++;
        }
        int took = ticks;
        scene.check("with the spread chance at 1 a mushroom has spread within sixty random ticks", () -> scene.assertTrue(mushrooms(level, marker) >= 2, "mushrooms " + mushrooms(level, marker) + " after " + took));

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                put(level, marker.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
            }
        }
        chance[0] = 0.0F;
        put(level, marker, state);
        tick(level, marker, 500);
        scene.check("with it at 0 it has not spread after five hundred", () -> scene.assertEquals(1, mushrooms(level, marker), "mushrooms"));
        scene.succeed();
    }

    private static int mushrooms(ServerLevel level, BlockPos center) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, 0, -4), center.offset(4, 1, 4))) {
            count += level.getBlockState(pos).is(Blocks.BROWN_MUSHROOM) ? 1 : 0;
        }
        return count;
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aHoeTillsVerdantSoilToVerdantFarmlandWhenTheSpaceAboveIsAir(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos soil = at(scene, 3, FLOOR, 3);
        put(level, soil, verdantSoil());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
        UseOnContext hoe = new UseOnContext(level, null, InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE), hit);
        UseOnContext stick = new UseOnContext(level, null, InteractionHand.MAIN_HAND, new ItemStack(Items.STICK), hit);
        BlockState state = level.getBlockState(soil);
        BlockState tilled = state.getToolModifiedState(hoe, ItemAbilities.HOE_TILL, true);
        scene.check("a hoe tills it to verdant farmland, not to vanilla's", () -> scene.assertTrue(tilled != null && tilled.is(FloraContent.VERDANT_FARMLAND.get()), "answer " + tilled));
        scene.check("a stack that cannot till gets nothing", () -> scene.assertTrue(state.getToolModifiedState(stick, ItemAbilities.HOE_TILL, true) == null, "answered"));
        put(level, soil.above(), Blocks.STONE.defaultBlockState());
        scene.check("with a block above, the hoe gets nothing", () -> scene.assertTrue(state.getToolModifiedState(hoe, ItemAbilities.HOE_TILL, true) == null, "answered"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ farmland

    @Scene(maxTicks = 40, isolated = true)
    public void verdantFarmlandUnderABlockTurnsToSoilNeverDirt(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos farmland = at(scene, 3, FLOOR, 3);
        put(level, farmland, verdantFarmland());
        Entity pig = scene.helper().spawn(EntityType.PIG, new BlockPos(3, FLOOR + 1, 3));
        verdantFarmland().getBlock().fallOn(level, level.getBlockState(farmland), farmland, pig, 10.0);
        scene.check("a fall from a height leaves it farmland", () -> scene.assertTrue(level.getBlockState(farmland).is(FloraContent.VERDANT_FARMLAND.get()), "block " + level.getBlockState(farmland)));
        pig.discard();

        put(level, farmland, verdantFarmland().setValue(FarmlandBlock.MOISTURE, 0));
        tick(level, farmland, 1);
        scene.check("moisture 0 is back at 7 after a random tick", () -> scene.assertEquals(FarmlandBlock.MAX_MOISTURE, level.getBlockState(farmland).getValue(FarmlandBlock.MOISTURE), "moisture"));

        put(level, farmland.above(), Blocks.STONE.defaultBlockState());
        scene.succeedWhen(() -> level.getBlockState(farmland).is(FloraContent.VERDANT_SOIL.get()), "a solid block on top turns the farmland to verdant soil");
    }

    // ------------------------------------------------------------------ loot and tags

    @Scene(maxTicks = 20, isolated = true)
    public void theSoilDropsItselfTheFarmlandDropsSoilAndBothAreVerdant(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos pos = at(scene, 3, FLOOR, 3);
        List<ItemStack> soilDrops = Block.getDrops(verdantSoil(), level, pos, null);
        List<ItemStack> farmlandDrops = Block.getDrops(verdantFarmland(), level, pos, null);
        scene.check("the soil drops itself", () -> scene.assertTrue(soilDrops.size() == 1 && soilDrops.getFirst().is(FloraContent.VERDANT_SOIL_ITEM.get()) && soilDrops.getFirst().getCount() == 1,
                "drops " + soilDrops));
        scene.check("the farmland drops one verdant soil", () -> scene.assertTrue(farmlandDrops.size() == 1 && farmlandDrops.getFirst().is(FloraContent.VERDANT_SOIL_ITEM.get()) && farmlandDrops.getFirst().getCount() == 1,
                "drops " + farmlandDrops));
        scene.check("both are in jolcraft:verdant", () -> scene.assertTrue(verdantSoil().is(FloraTags.VERDANT) && verdantFarmland().is(FloraTags.VERDANT), "tags"));
        scene.check("and the farmland is in supports_crops", () -> scene.assertTrue(verdantFarmland().is(BlockTags.SUPPORTS_CROPS), "tags"));
        scene.succeed();
    }
}
