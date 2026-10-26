package net.sievert.jolcraft.gametest.module.primitive.advancement;

import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/**
 * The advancement primitive against a real player and the real data pack: firing the declared kind completes the
 * advancement whose criterion it matches and not before, the player is told, granting completes an advancement
 * outright, and an advancement the data pack lacks is neither had nor granted. Isolated: they add players.
 */
public final class AdvancementScenes {

    private static final Identifier PINGED = Ids.of(JolCraftGameTests.MOD_ID, "pinged");
    private static final Identifier GRANTED = Ids.of(JolCraftGameTests.MOD_ID, "granted");
    private static final Identifier MISSING = Ids.of(JolCraftGameTests.MOD_ID, "missing");
    private static final Identifier TWICE = Ids.of(JolCraftGameTests.MOD_ID, "pinged_twice");

    @Scene(maxTicks = 40, isolated = true)
    public void firingTheKindCompletesTheAdvancementWhoseCriterionItMatches(TestScene scene) {
        TestPlayer player = scene.addPlayer("Achiever");
        scene.check("not completed at first", () -> scene.assertFalse(JolCraftGameTests.advancement().has(player.player(), PINGED), "pinged at first"));
        TestTriggers.PINGED.fire(player.player(), instance -> 2 >= instance.atLeast());
        scene.check("two pings are not enough", () -> scene.assertFalse(JolCraftGameTests.advancement().has(player.player(), PINGED), "pinged after two"));
        player.clearReceived();
        TestTriggers.PINGED.fire(player.player(), instance -> 3 >= instance.atLeast());
        scene.check("three pings complete it", () -> scene.assertTrue(JolCraftGameTests.advancement().has(player.player(), PINGED), "pinged after three"));
        scene.runAtTick(2, () -> {
            // The game tells the player on its next tick, when it flushes what changed.
            scene.check("and the player was told", () -> scene.assertTrue(player.received(ClientboundUpdateAdvancementsPacket.class).stream()
                    .anyMatch(packet -> packet.getProgress().containsKey(PINGED)), "progress packet"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 40, isolated = true)
    public void grantingCompletesAnAdvancementAndAMissingOneIsNeitherHadNorGranted(TestScene scene) {
        TestPlayer player = scene.addPlayer("Granted");
        int experience = player.player().totalExperience;
        scene.check("granting completes it", () -> scene.assertTrue(
                JolCraftGameTests.advancement().grant(player.player(), GRANTED) && JolCraftGameTests.advancement().has(player.player(), GRANTED), "granted"));
        scene.check("through the game, which paid the reward", () -> scene.assertEquals(experience + 7, player.player().totalExperience, "experience"));
        scene.check("granting again changes nothing and still answers complete", () -> scene.assertTrue(JolCraftGameTests.advancement().grant(player.player(), GRANTED), "granted twice"));
        TestTriggers.PINGED.fire(player.player(), instance -> 1 >= instance.atLeast());
        scene.check("one of two criteria is not complete", () -> scene.assertFalse(JolCraftGameTests.advancement().has(player.player(), TWICE), "half done"));
        scene.check("granting completes the rest", () -> scene.assertTrue(
                JolCraftGameTests.advancement().grant(player.player(), TWICE) && JolCraftGameTests.advancement().has(player.player(), TWICE), "completed"));
        scene.check("a missing advancement is neither had nor granted", () -> scene.assertTrue(
                !JolCraftGameTests.advancement().has(player.player(), MISSING) && !JolCraftGameTests.advancement().grant(player.player(), MISSING), "missing"));
        scene.succeed();
    }
}
