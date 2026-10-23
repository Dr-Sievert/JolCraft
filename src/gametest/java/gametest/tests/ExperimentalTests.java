package gametest.tests;

import gametest.GameTestGroup;
import gametest.JolCraftTestHelper;
import gametest.util.TestPlayerHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Scratch holder for trying things out. Its one test is kept registered on purpose: the game
 * test server refuses to start with an empty test list, and the body doubles as a smoke test
 * for the test-only mixins (packet recording) and the fake-player helpers.
 */
@GameTestGroup
public class ExperimentalTests {

    @GameTest(template = "basic")
    public static void experimental(GameTestHelper vanillaHelper) {
        JolCraftTestHelper helper = JolCraftTestHelper.of(vanillaHelper);
        String testName = "Experimental";

        helper.log().startTest(
                testName,
                "Fake player connect, disconnect and reconnect round trip"
        );

        helper.network().enablePacketRecording();

        ServerPlayer player = TestPlayerHelper.createPlayer(helper.getLevel());

        try {
            TestPlayerHelper.assertPlayerPresent(helper.log(), helper.getLevel(), player.getUUID());

            helper.network().clearAllPackets();

            TestPlayerHelper.disconnect(player);

            TestPlayerHelper.assertPlayerNotPresent(helper.log(), helper.getLevel(), player.getUUID());

            ServerPlayer reconnected = TestPlayerHelper.reconnect(helper.getLevel(), player);
            player = reconnected;

            TestPlayerHelper.assertPlayerPresent(helper.log(), helper.getLevel(), reconnected.getUUID());

            helper.log().checkPlayer(reconnected, () -> {
                if (helper.network().getSentPackets(reconnected).isEmpty()) {
                    throw new AssertionError("Expected the reconnect to send packets to the player, but none were recorded.");
                }
            }, "received packets on reconnect (packet recording mixin is live)");
        } finally {
            // Leave no fake player behind for later tests, whether or not a check failed.
            if (TestPlayerHelper.getOnlinePlayer(helper.getLevel(), player.getUUID()) != null) {
                TestPlayerHelper.disconnect(player);
            }

            helper.network().disablePacketRecording();
        }

        helper.log().endTest(testName);
    }
}
