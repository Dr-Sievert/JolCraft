package net.sievert.jolcraft.gametest;

import net.minecraft.world.level.block.Blocks;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * The adapter's own scenes, so a fresh checkout proves the tier works end to end: the framework came up, the
 * scenes were found, a block placed is there a tick later, and a player can join, be recorded and be answered.
 * Scenes of the mod's modules live under {@code gametest.module.<tier>.<folder>} instead.
 */
public final class ExampleScenes {

    /** The empty template is one block of air at (0, 0, 0); a block placed there is still there next tick. */
    @Scene(maxTicks = 40)
    public void blockPlacedIsThereNextTick(TestScene scene) {
        scene.assertEquals(Blocks.AIR, scene.blockAt(0, 0, 0), "the template cell before anything is placed");
        scene.setBlock(0, 0, 0, Blocks.STONE);
        scene.runAtTick(1, () -> {
            scene.check("the block is still there a tick later",
                    () -> scene.assertEquals(Blocks.STONE, scene.blockAt(0, 0, 0), "block at (0, 0, 0)"));
            scene.succeed();
        });
    }

    /** A player joins, which already sends it packets, and is answered when it runs a command. Isolated: it has a player. */
    @Scene(maxTicks = 60, isolated = true)
    public void playerJoinsAndIsAnswered(TestScene scene) {
        TestPlayer player = scene.addPlayer("Tester");
        scene.check("the player is online", () -> scene.assertTrue(
                scene.level().getServer().getPlayerList().getPlayer(player.id()) != null, "player should be in the player list"));
        scene.check("joining sent the player packets", () -> scene.assertFalse(player.received().isEmpty(), "login should have sent packets"));
        player.clearReceived();
        player.runCommand("help");
        scene.runAtTick(1, () -> {
            scene.check("the player was shown the command list", () -> scene.assertTrue(
                    player.chat().stream().anyMatch(line -> line.getString().startsWith("/")), "help should list commands in chat"));
            scene.succeed();
        });
    }
}
