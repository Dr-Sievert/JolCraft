package net.sievert.jolcraft.gametest.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import net.sievert.jolcraft.module.primitive.log.Log;

/**
 * What a scene is written against: the arena, ticks, named checks and players, over vanilla's
 * {@link GameTestHelper}. Coordinates are relative to the structure's origin. The scene owns what it creates:
 * when it ends, passed or failed, every {@link #onEnd(Runnable)} runs in reverse order and every player it added
 * is disconnected and its recording dropped, so no scene writes a try/finally. Anything a scene throws, an Error
 * included, becomes that scene's failure rather than the server's.
 */
public final class TestScene {

    private final GameTestHelper helper;
    private final Log log;
    private final String name;
    private final List<Runnable> onEnd = new ArrayList<>();
    private final List<TestPlayer> players = new ArrayList<>();
    private boolean tickingPlayers;
    private boolean ended;

    TestScene(GameTestHelper helper, Log log, String name) {
        this.helper = helper;
        this.log = log.tagged(name);
        this.name = name;
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo info) {
            }

            @Override
            public void testPassed(GameTestInfo info, GameTestRunner runner) {
                end(true);
            }

            @Override
            public void testFailed(GameTestInfo info, GameTestRunner runner) {
                end(false);
            }

            @Override
            public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
            }
        });
    }

    /** Vanilla's helper, for what this class does not wrap. */
    public GameTestHelper helper() {
        return helper;
    }

    public ServerLevel level() {
        return helper.getLevel();
    }

    public Log log() {
        return log;
    }

    public String name() {
        return name;
    }

    /** The structure's origin in the level: relative (0, 0, 0). */
    public BlockPos origin() {
        return helper.absolutePos(BlockPos.ZERO);
    }

    public Block blockAt(int x, int y, int z) {
        return helper.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public void setBlock(int x, int y, int z, Block block) {
        helper.setBlock(x, y, z, block);
    }

    /** Runs {@code action} at the given tick of the scene; tick 0 is the scene's first tick. */
    public void runAtTick(long tick, Runnable action) {
        helper.runAtTickTime(tick, guarded(action));
    }

    public void runAfter(long ticks, Runnable action) {
        helper.runAfterDelay(ticks, guarded(action));
    }

    public void succeed() {
        helper.succeed();
    }

    /** Succeeds as soon as {@code condition} holds; fails at {@code maxTicks} if it never does. */
    public void succeedWhen(BooleanSupplier condition, String description) {
        helper.succeedWhen(guarded(() -> assertTrue(condition.getAsBoolean(), description)));
    }

    public void fail(String message) {
        throw helper.assertionException(Component.literal(message));
    }

    public void assertTrue(boolean condition, String message) {
        helper.assertTrue(condition, message);
    }

    public void assertFalse(boolean condition, String message) {
        helper.assertTrue(!condition, message);
    }

    public <T> void assertEquals(T expected, T actual, String what) {
        if (!Objects.equals(expected, actual)) {
            fail("Expected " + what + " to be " + expected + ", was " + actual);
        }
    }

    /** Runs a named check and logs that it held, so the log says what was verified rather than only that the scene passed. */
    public void check(String description, Runnable check) {
        try {
            check.run();
            log.info("ok: {}", description);
        } catch (RuntimeException | Error e) {
            log.error("failed: {}", description);
            throw e;
        }
    }

    /** Runs when the scene ends, passed or failed, after what was registered later. */
    public void onEnd(Runnable action) {
        onEnd.add(action);
    }

    /**
     * A player in the level, recording what it is sent from this moment on; ticked by the scene and disconnected
     * when the scene ends. A scene that adds players should be {@link Scene#isolated()}.
     */
    public TestPlayer addPlayer(String playerName) {
        TestPlayer player = TestPlayer.join(this, playerName);
        players.add(player);
        if (!tickingPlayers) {
            tickingPlayers = true;
            helper.onEachTick(this::tickPlayers);
        }
        return player;
    }

    private void tickPlayers() {
        for (TestPlayer player : players) {
            player.tick();
        }
    }

    /** The game's test loop catches exceptions, not errors; an assertion or linkage error inside a tick action would take the server down. */
    private static Runnable guarded(Runnable action) {
        return () -> {
            try {
                action.run();
            } catch (Error e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private void end(boolean passed) {
        if (ended) {
            return;
        }
        ended = true;
        try {
            for (int i = onEnd.size() - 1; i >= 0; i--) {
                try {
                    onEnd.get(i).run();
                } catch (Throwable e) {
                    log.error("onEnd action failed", e);
                }
            }
        } finally {
            for (TestPlayer player : players) {
                try {
                    player.leave();
                } catch (Throwable e) {
                    log.error("could not disconnect {}", player.name(), e);
                }
            }
            log.info(passed ? "passed" : "failed");
        }
    }
}
