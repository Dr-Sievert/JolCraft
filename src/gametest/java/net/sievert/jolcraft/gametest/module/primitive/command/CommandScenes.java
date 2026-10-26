package net.sievert.jolcraft.gametest.module.primitive.command;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/** The command primitive against a real player: both declared branches answer under the shared root. Isolated: it adds a player. */
public final class CommandScenes {

    @Scene(maxTicks = 40, isolated = true)
    public void bothBranchesAnswerUnderTheSharedRoot(TestScene scene) {
        TestPlayer player = scene.addPlayer("Commander");
        player.clearReceived();
        player.runCommand(JolCraftGameTests.MOD_ID + " ping 3");
        player.runCommand(JolCraftGameTests.MOD_ID + " echo hello there");
        scene.runAtTick(1, () -> {
            scene.check("ping answered", () -> scene.assertTrue(player.chat().stream().anyMatch(line -> line.getString().equals("pong 3")), "chat " + player.chat()));
            scene.check("echo answered", () -> scene.assertTrue(player.chat().stream().anyMatch(line -> line.getString().equals("hello there")), "chat " + player.chat()));
            scene.succeed();
        });
    }
}
