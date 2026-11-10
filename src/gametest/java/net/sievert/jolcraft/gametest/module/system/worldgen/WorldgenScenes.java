package net.sievert.jolcraft.gametest.module.system.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.worldgen.AddLootTableProcessor;
import net.sievert.jolcraft.module.system.worldgen.HangingProcessor;
import net.sievert.jolcraft.module.system.worldgen.HeightFilter;
import net.sievert.jolcraft.module.system.worldgen.RandomCaveInProcessor;
import net.sievert.jolcraft.module.system.worldgen.RandomCobwebProcessor;

/**
 * The worldgen kinds placing real templates. Each scene builds its piece in the arena from vanilla blocks, captures it
 * into a template keeping the air (structure voids are the only blocks ignored), clears the area, and places the
 * template through {@link StructurePlaceSettings} with the kind as its processor; a scene that needs to tell a dropped
 * block from a placed air block fills the piece's cells with dirt first, which the template does not hold.
 */
public final class WorldgenScenes {

    private static final int UPDATE_CLIENTS = 2;

    private static BlockPos corner(TestScene scene) {
        return scene.helper().absolutePos(new BlockPos(2, 2, 2));
    }

    private static void fill(ServerLevel level, BlockPos corner, Vec3i size, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(size).offset(-1, -1, -1))) {
            level.setBlock(pos, state, UPDATE_CLIENTS);
        }
    }

    private static StructureTemplate capture(ServerLevel level, BlockPos corner, Vec3i size) {
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, corner, size, false, List.of(Blocks.STRUCTURE_VOID));
        return template;
    }

    private static void place(ServerLevel level, StructureTemplate template, BlockPos corner, StructureProcessor... processors) {
        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (StructureProcessor processor : processors) {
            settings.addProcessor(processor);
        }
        template.placeInWorld(level, corner, corner, settings, level.getRandom(), UPDATE_CLIENTS);
    }

    private static StructurePlaceSettings settings(StructureProcessor processor) {
        return new StructurePlaceSettings().addProcessor(processor);
    }

    private static boolean is(ServerLevel level, BlockPos pos, Block block) {
        return level.getBlockState(pos).is(block);
    }

    // ------------------------------------------------------------------ add_loot_table

    @Scene(maxTicks = 20, isolated = true)
    public void aChestKeepsItsFacingAndWaterAndGetsTheTableWhileOtherBlocksPass(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos corner = corner(scene);
        Vec3i size = new Vec3i(3, 1, 1);
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST).setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(corner, chest, UPDATE_CLIENTS);
        level.setBlock(corner.east(), Blocks.BARREL.defaultBlockState(), UPDATE_CLIENTS);
        level.setBlock(corner.east(2), Blocks.STONE.defaultBlockState(), UPDATE_CLIENTS);
        StructureTemplate template = capture(level, corner, size);
        fill(level, corner, size, Blocks.AIR.defaultBlockState());

        place(level, template, corner, new AddLootTableProcessor(Blocks.CHEST, BuiltInLootTables.SIMPLE_DUNGEON));

        BlockState placed = level.getBlockState(corner);
        scene.check("the chest keeps its facing and its water", () -> scene.assertTrue(placed.is(Blocks.CHEST) && placed.getValue(ChestBlock.FACING) == Direction.EAST
                && placed.getValue(BlockStateProperties.WATERLOGGED), "state " + placed));
        scene.check("and carries the simple dungeon table", () -> scene.assertTrue(level.getBlockEntity(corner) instanceof RandomizableContainer container
                && BuiltInLootTables.SIMPLE_DUNGEON.equals(container.getLootTable()), "block entity " + level.getBlockEntity(corner)));
        BlockEntity barrel = level.getBlockEntity(corner.east());
        scene.check("a barrel passes untouched, with no table", () -> scene.assertTrue(is(level, corner.east(), Blocks.BARREL)
                && barrel instanceof RandomizableContainer container && container.getLootTable() == null, "barrel " + barrel));
        scene.check("and so does stone", () -> scene.assertTrue(is(level, corner.east(2), Blocks.STONE), "stone"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ random_cobweb

    /** Stone, air, stone, air, air in a row, with a stone of the world beside the piece's end and another above its fifth block; the piece placed through the processors. */
    private static BlockPos placeWebRow(TestScene scene, StructureProcessor... processors) {
        ServerLevel level = scene.level();
        BlockPos corner = corner(scene);
        Vec3i size = new Vec3i(5, 1, 1);
        fill(level, corner, size, Blocks.AIR.defaultBlockState());
        level.setBlock(corner, Blocks.STONE.defaultBlockState(), UPDATE_CLIENTS);
        level.setBlock(corner.east(2), Blocks.STONE.defaultBlockState(), UPDATE_CLIENTS);
        StructureTemplate template = capture(level, corner, size);
        fill(level, corner, size, Blocks.AIR.defaultBlockState());
        level.setBlock(corner.east(5), Blocks.STONE.defaultBlockState(), UPDATE_CLIENTS);
        level.setBlock(corner.east(4).above(), Blocks.STONE.defaultBlockState(), UPDATE_CLIENTS);
        place(level, template, corner, processors);
        return corner;
    }

    @Scene(maxTicks = 20, isolated = true)
    public void anAirBlockBetweenTwoBlockingBlocksOfThePieceBecomesACobwebAtChanceOne(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos corner = placeWebRow(scene, new RandomCobwebProcessor(1.0F));
        scene.check("the air between two blocks of the piece is a cobweb", () -> scene.assertTrue(is(level, corner.east(), Blocks.COBWEB), "x1 " + level.getBlockState(corner.east())));
        scene.check("an air block with a single blocking neighbour stays air", () -> scene.assertTrue(level.getBlockState(corner.east(3)).isAir(), "x3 " + level.getBlockState(corner.east(3))));
        scene.check("a solid block of the world beside the piece does not count", () -> scene.assertTrue(level.getBlockState(corner.east(4)).isAir(), "x4 " + level.getBlockState(corner.east(4))));
        scene.check("the piece's stone and the world's stone stay", () -> scene.assertTrue(is(level, corner, Blocks.STONE) && is(level, corner.east(2), Blocks.STONE)
                && is(level, corner.east(5), Blocks.STONE), "stones"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCobwebAtChanceZeroChangesNothing(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos corner = placeWebRow(scene, new RandomCobwebProcessor(0.0F));
        scene.check("every air block is still air", () -> scene.assertTrue(level.getBlockState(corner.east()).isAir() && level.getBlockState(corner.east(3)).isAir()
                && level.getBlockState(corner.east(4)).isAir(), "air"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCappedProcessorAfterTheCobwebStillAppliesItsDelegate(TestScene scene) {
        ServerLevel level = scene.level();
        RuleProcessor stoneToGold = new RuleProcessor(List.of(new ProcessorRule(new BlockMatchTest(Blocks.STONE), AlwaysTrueTest.INSTANCE, Blocks.GOLD_BLOCK.defaultBlockState())));
        BlockPos corner = placeWebRow(scene, new RandomCobwebProcessor(1.0F), new CappedProcessor(stoneToGold, ConstantInt.of(10)));
        scene.check("the capped delegate replaced the piece's stone", () -> scene.assertTrue(is(level, corner, Blocks.GOLD_BLOCK) && is(level, corner.east(2), Blocks.GOLD_BLOCK), "gold"));
        scene.check("and the cobweb is still there", () -> scene.assertTrue(is(level, corner.east(), Blocks.COBWEB), "web"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ random_cave_in

    private static final Vec3i BOX = new Vec3i(5, 6, 5);

    private static BlockState boxState(int x, int y, int z) {
        boolean wall = x == 0 || x == 4 || z == 0 || z == 4;
        return y == 0 || y >= 4 || wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    /** A hollow stone box with a floor and a two-thick ceiling, whose cells are filled with dirt before the template is placed. */
    private static StructureTemplate boxOverDirt(ServerLevel level, BlockPos corner) {
        for (int x = 0; x < BOX.getX(); x++) {
            for (int y = 0; y < BOX.getY(); y++) {
                for (int z = 0; z < BOX.getZ(); z++) {
                    level.setBlock(corner.offset(x, y, z), boxState(x, y, z), UPDATE_CLIENTS);
                }
            }
        }
        StructureTemplate template = capture(level, corner, BOX);
        fill(level, corner, BOX, Blocks.DIRT.defaultBlockState());
        return template;
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCaveInAtChanceOneCarvesColumnsDownToAirAndLeavesTheFillerWhereBlocksWereDropped(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos corner = corner(scene);
        StructureTemplate template = boxOverDirt(level, corner);
        place(level, template, corner, new RandomCaveInProcessor(1.0F));

        int carved = 0;
        List<String> wrong = new ArrayList<>();
        for (int x = 0; x < BOX.getX(); x++) {
            for (int z = 0; z < BOX.getZ(); z++) {
                BlockPos column = corner.offset(x, BOX.getY() - 1, z);
                if (is(level, column, Blocks.DIRT)) {
                    carved++;
                    BlockPos cursor = column;
                    while (is(level, cursor, Blocks.DIRT)) {
                        cursor = cursor.below();
                    }
                    if (!level.getBlockState(cursor).isAir()) {
                        wrong.add("column " + x + "," + z + " ends in " + level.getBlockState(cursor));
                    }
                } else {
                    for (int y = 0; y < BOX.getY(); y++) {
                        if (!level.getBlockState(corner.offset(x, y, z)).equals(boxState(x, y, z))) {
                            wrong.add("column " + x + "," + z + " changed at " + y);
                        }
                    }
                }
            }
        }
        int carvedColumns = carved;
        scene.check("at least one column was carved", () -> scene.assertTrue(carvedColumns >= 1, "carved " + carvedColumns));
        scene.check("the filler shows where blocks were dropped, the lowest carved block is air, every other column is as built", () -> scene.assertTrue(wrong.isEmpty(), wrong.toString()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCaveInAtChanceZeroLeavesThePieceUnchanged(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos corner = corner(scene);
        StructureTemplate template = boxOverDirt(level, corner);
        place(level, template, corner, new RandomCaveInProcessor(0.0F));
        List<String> wrong = new ArrayList<>();
        for (int x = 0; x < BOX.getX(); x++) {
            for (int y = 0; y < BOX.getY(); y++) {
                for (int z = 0; z < BOX.getZ(); z++) {
                    if (!level.getBlockState(corner.offset(x, y, z)).equals(boxState(x, y, z))) {
                        wrong.add(x + "," + y + "," + z);
                    }
                }
            }
        }
        scene.check("every cell is as the template holds it", () -> scene.assertTrue(wrong.isEmpty(), wrong.toString()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCaveInKeepsABlockEntityInTheColumnAndTheWalkGoesOnPastIt(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos corner = corner(scene);
        Vec3i size = new Vec3i(1, 5, 1);
        level.setBlock(corner, Blocks.AIR.defaultBlockState(), UPDATE_CLIENTS);
        level.setBlock(corner.above(1), Blocks.STONE.defaultBlockState(), UPDATE_CLIENTS);
        level.setBlock(corner.above(2), Blocks.CHEST.defaultBlockState(), UPDATE_CLIENTS);
        level.setBlock(corner.above(3), Blocks.STONE.defaultBlockState(), UPDATE_CLIENTS);
        level.setBlock(corner.above(4), Blocks.STONE.defaultBlockState(), UPDATE_CLIENTS);
        StructureTemplate template = capture(level, corner, size);
        fill(level, corner, size, Blocks.DIRT.defaultBlockState());

        place(level, template, corner, new RandomCaveInProcessor(1.0F));

        scene.check("the blocks above the block entity were dropped, the filler shows", () -> scene.assertTrue(is(level, corner.above(4), Blocks.DIRT) && is(level, corner.above(3), Blocks.DIRT), "top"));
        scene.check("the chest is kept", () -> scene.assertTrue(is(level, corner.above(2), Blocks.CHEST) && level.getBlockEntity(corner.above(2)) != null, "chest " + level.getBlockState(corner.above(2))));
        scene.check("the lowest carved block below it is air, as is the air under that", () -> scene.assertTrue(level.getBlockState(corner.above(1)).isAir() && level.getBlockState(corner).isAir(), "bottom"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCaveInCarvesTheSameColumnsForTheSameSeedAndPositionAndKeepsTheGivenOrder(TestScene scene) {
        ServerLevel level = scene.level();
        BlockPos corner = corner(scene);
        List<StructureTemplate.StructureBlockInfo> given = new ArrayList<>();
        for (int x = 0; x < BOX.getX(); x++) {
            for (int y = 0; y < BOX.getY(); y++) {
                for (int z = 0; z < BOX.getZ(); z++) {
                    given.add(new StructureTemplate.StructureBlockInfo(new BlockPos(x, y, z), boxState(x, y, z), null));
                }
            }
        }
        StructurePlaceSettings settings = settings(new RandomCaveInProcessor(1.0F));
        List<StructureTemplate.StructureBlockInfo> first = StructureTemplate.processBlockInfos(level, corner, corner, settings, given, null);
        List<StructureTemplate.StructureBlockInfo> second = StructureTemplate.processBlockInfos(level, corner, corner, settings, given, null);
        scene.check("the same seed and position answer the same list", () -> scene.assertTrue(first.equals(second), "lists differ"));
        scene.check("some blocks were dropped", () -> scene.assertTrue(first.size() < given.size(), "sizes " + first.size() + " of " + given.size()));
        List<BlockPos> order = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : given) {
            order.add(info.pos().offset(corner));
        }
        int previous = -1;
        boolean inOrder = true;
        for (StructureTemplate.StructureBlockInfo info : first) {
            int index = order.indexOf(info.pos());
            inOrder &= index > previous;
            previous = index;
        }
        boolean keptInOrder = inOrder;
        scene.check("what remains is in the given order", () -> scene.assertTrue(keptInOrder, "order"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ hanging

    private static void hangingPiece(TestScene scene, boolean solidAbove) {
        ServerLevel level = scene.level();
        BlockPos corner = corner(scene);
        Vec3i size = new Vec3i(1, 2, 1);
        level.setBlock(corner, Blocks.STONE_BRICKS.defaultBlockState(), UPDATE_CLIENTS);
        level.setBlock(corner.above(), Blocks.STONE_BRICKS.defaultBlockState(), UPDATE_CLIENTS);
        StructureTemplate template = capture(level, corner, size);
        fill(level, corner, size, Blocks.DIRT.defaultBlockState());
        level.setBlock(corner.above(2), solidAbove ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), UPDATE_CLIENTS);
        place(level, template, corner, new HangingProcessor());
        boolean whole = is(level, corner, Blocks.STONE_BRICKS) && is(level, corner.above(), Blocks.STONE_BRICKS);
        boolean none = is(level, corner, Blocks.DIRT) && is(level, corner.above(), Blocks.DIRT);
        if (solidAbove) {
            scene.check("under a solid world block the piece is placed whole", () -> scene.assertTrue(whole, "cells " + level.getBlockState(corner) + ", " + level.getBlockState(corner.above())));
        } else {
            scene.check("under air none of its blocks is placed and the filler stays", () -> scene.assertTrue(none, "cells " + level.getBlockState(corner) + ", " + level.getBlockState(corner.above())));
        }
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aHangingPieceUnderASolidBlockIsPlacedWhole(TestScene scene) {
        hangingPiece(scene, true);
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aHangingPieceUnderAirIsNotPlacedAtAll(TestScene scene) {
        hangingPiece(scene, false);
        scene.succeed();
    }

    // ------------------------------------------------------------------ height_filter

    @Scene(maxTicks = 20, isolated = true)
    public void aHeightFilterKeepsThePositionsWithinItsBoundsAndAnAbsentBoundFiltersNothingOnItsSide(TestScene scene) {
        ServerLevel level = scene.level();
        PlacementContext context = new PlacementContext(level, level.getChunkSource().getGenerator(), Optional.empty());
        HeightFilter bounded = new HeightFilter(10, 20);
        scene.check("the minimum and the maximum are kept", () -> scene.assertTrue(kept(bounded, context, level, 10) && kept(bounded, context, level, 20), "bounds"));
        scene.check("one below and one above are dropped", () -> scene.assertTrue(!kept(bounded, context, level, 9) && !kept(bounded, context, level, 21), "outside"));
        HeightFilter noMinimum = new HeightFilter(Integer.MIN_VALUE, 20);
        HeightFilter noMaximum = new HeightFilter(10, Integer.MAX_VALUE);
        scene.check("without a minimum nothing is filtered below", () -> scene.assertTrue(kept(noMinimum, context, level, -60) && !kept(noMinimum, context, level, 21), "no minimum"));
        scene.check("without a maximum nothing is filtered above", () -> scene.assertTrue(kept(noMaximum, context, level, 300) && !kept(noMaximum, context, level, 9), "no maximum"));
        scene.succeed();
    }

    private static boolean kept(HeightFilter filter, PlacementContext context, ServerLevel level, int y) {
        return filter.getPositions(context, level.getRandom(), new BlockPos(0, y, 0)).findAny().isPresent();
    }
}
