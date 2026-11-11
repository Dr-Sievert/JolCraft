package net.sievert.jolcraft.gametest.module.system.flora;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.flora.FloraContent;

/**
 * What the flora scenes share: positions in the arena, the two kinds of ground, a closed box of stone for darkness
 * and the waiting for the light to settle. A scene that relies on darkness or on light waits until the cells really
 * are one or the other and states it as a named check. Every flora scene that builds blocks is {@code isolated}: the
 * game sets the arenas of one batch a few blocks apart, closer than these scenes build, and one scene's blocks stood in
 * another's dark box before they were (W8 in REVIEW_TODO.md).
 */
final class FloraArena {

    /** The arena row on which the ground is laid; the plant stands one above. */
    static final int FLOOR = 2;
    /** Raw brightness below this is dark for a scene, a step stricter than the plants (8 or less). */
    static final int DARK = 8;
    /** Raw brightness at or above this is light enough for vanilla's crops. */
    static final int LIT = 9;
    /** Raw brightness at or above this is more than a mushroom stands in. */
    static final int OPEN = 13;
    /**
     * How many ticks a wait is given before it is given up. The game-test server ticks as fast as it can while the light
     * settles on another thread, so a short wait gave up on a slow run with the box still lit; a long one costs nothing
     * when the light is there.
     */
    static final int GIVE_UP = 1000;
    /** The ticks a scene with one wait is allowed, and one with two. */
    static final int ONE_WAIT = GIVE_UP + 100;
    static final int TWO_WAITS = 2 * GIVE_UP + 100;

    private FloraArena() {
    }

    static BlockPos at(TestScene scene, int x, int y, int z) {
        return scene.helper().absolutePos(new BlockPos(x, y, z));
    }

    static void put(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** Puts a block and lets its neighbours know, as the game does when a player places one. */
    static void putAndUpdate(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    static BlockState verdantFarmland() {
        return FloraContent.VERDANT_FARMLAND.get().defaultBlockState();
    }

    static BlockState verdantSoil() {
        return FloraContent.VERDANT_SOIL.get().defaultBlockState();
    }

    static BlockState moistFarmland() {
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
    }

    /** A closed box of stone with one cell of air above {@code ground}, in the column (x, z); returns the cell. */
    static BlockPos darkBox(TestScene scene, int x, int z, BlockState ground) {
        return darkBox(scene, x, z, ground, 1);
    }

    /** A closed box of stone with {@code room} cells of air above {@code ground}, in the column (x, z); returns the lowest cell. */
    static BlockPos darkBox(TestScene scene, int x, int z, BlockState ground, int room) {
        return darkBox(scene, x, z, ground, room, FLOOR);
    }

    /** The same box with its ground on the arena row {@code floor}, which may lie outside the structure, to reach a Y the structure does not. */
    static BlockPos darkBox(TestScene scene, int x, int z, BlockState ground, int room, int floor) {
        ServerLevel level = scene.level();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean center = dx == 0 && dz == 0;
                put(level, at(scene, x + dx, floor, z + dz), center ? ground : Blocks.STONE.defaultBlockState());
                for (int dy = 1; dy <= room; dy++) {
                    put(level, at(scene, x + dx, floor + dy, z + dz), center ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
                }
                put(level, at(scene, x + dx, floor + room + 1, z + dz), Blocks.STONE.defaultBlockState());
            }
        }
        return at(scene, x, floor + 1, z);
    }

    /** Takes the box of {@link #darkBox(TestScene, int, int, BlockState, int)} away but for its ground, so the light comes in. */
    static void openBox(TestScene scene, int x, int z, int room) {
        ServerLevel level = scene.level();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= room + 1; dy++) {
                    BlockPos pos = at(scene, x + dx, FLOOR + dy, z + dz);
                    if (level.getBlockState(pos).is(Blocks.STONE)) {
                        // no shape update: the light comes in on another thread while the box is still being opened, and a
                        // plant in it that got word of a neighbour's change then would already be judged in the light
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
    }

    /**
     * Stops the level's own random ticks for the scene and gives them back when it ends, for an isolated scene that
     * leaves a plant standing over several ticks: farmland that dries tells the plant above it of a change.
     */
    static void withoutRandomTicks(TestScene scene) {
        ServerLevel level = scene.level();
        int speed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
        level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 0, level.getServer());
        scene.onEnd(() -> level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, speed, level.getServer()));
    }

    /** One wait of a scene: what must hold, and what runs once it does. */
    record Wait(BooleanSupplier ready, Runnable then) {
    }

    /** A wait until every cell is dark; {@code then} asserts the dark as a named check. */
    static Wait dark(TestScene scene, Runnable then, BlockPos... cells) {
        return new Wait(() -> {
            boolean dark = true;
            for (BlockPos cell : cells) {
                dark &= scene.level().getRawBrightness(cell, 0) < DARK;
            }
            return dark;
        }, then);
    }

    /** A wait until every cell is lit; {@code then} asserts the light as a named check. */
    static Wait lit(TestScene scene, Runnable then, BlockPos... cells) {
        return new Wait(() -> {
            boolean lit = true;
            for (BlockPos cell : cells) {
                lit &= scene.level().getRawBrightness(cell, 0) >= LIT;
            }
            return lit;
        }, then);
    }

    /**
     * Runs each wait's action once its condition holds, one wait after the other, and gives a wait up after
     * {@link #GIVE_UP} ticks so that the action's own named check fails rather than the scene timing out. Called from a
     * scene's body, never from inside a wait's action: every wait hangs on the one per-tick check registered here.
     * The game walks its schedule of a test while it runs a scheduled action, so registering another per-tick check
     * from inside one changes the schedule under it and can crash the server tick. A scene that waits twice passes
     * both waits here.
     */
    static void inOrder(TestScene scene, Wait... waits) {
        int[] next = {0};
        long[] since = {scene.helper().getTick()};
        scene.helper().onEachTick(() -> {
            if (next[0] >= waits.length) {
                return;
            }
            Wait wait = waits[next[0]];
            if (wait.ready().getAsBoolean() || scene.helper().getTick() - since[0] >= GIVE_UP) {
                next[0]++;
                since[0] = scene.helper().getTick();
                wait.then().run();
            }
        });
    }

    /** Runs {@code next} once every cell is dark, or when waiting is given up; {@code next} asserts the dark as a named check. From a scene's body only, see {@link #inOrder}. */
    static void whenDark(TestScene scene, Runnable next, BlockPos... cells) {
        inOrder(scene, dark(scene, next, cells));
    }

    static void assertDark(TestScene scene, String what, BlockPos cell) {
        scene.check(what + " is dark", () -> scene.assertTrue(scene.level().getRawBrightness(cell, 0) < DARK, "raw brightness " + scene.level().getRawBrightness(cell, 0)));
    }

    static void assertLit(TestScene scene, String what, BlockPos cell) {
        scene.check(what + " is lit", () -> scene.assertTrue(scene.level().getRawBrightness(cell, 0) >= LIT, "raw brightness " + scene.level().getRawBrightness(cell, 0)));
    }

    static void tick(ServerLevel level, BlockPos pos, int times) {
        for (int i = 0; i < times; i++) {
            level.getBlockState(pos).randomTick(level, pos, level.getRandom());
        }
    }

    /** Random-ticks the block at {@code pos} until {@code done} holds or {@code limit} ticks are used; answers the ticks used. */
    static int tickUntil(ServerLevel level, BlockPos pos, BooleanSupplier done, int limit) {
        int used = 0;
        while (used < limit && !done.getAsBoolean()) {
            level.getBlockState(pos).randomTick(level, pos, level.getRandom());
            used++;
        }
        return used;
    }

    /** Fills the box between two corners, inclusive, with a state. */
    static void fill(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            put(level, pos, state);
        }
    }

    /** How many blocks in the box between two corners, inclusive, the test holds for. */
    static int count(ServerLevel level, BlockPos from, BlockPos to, Predicate<BlockState> test) {
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            found += test.test(level.getBlockState(pos)) ? 1 : 0;
        }
        return found;
    }

    /** Whether the cell is in the open light the mushrooms' rule is about: raw brightness 13 or more. */
    static boolean openLight(ServerLevel level, BlockPos cell) {
        return level.getRawBrightness(cell, 0) >= OPEN;
    }
}
