package net.sievert.jolcraft.gametest.module.primitive.stat;

import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/** The stat primitive against a real player: the declared statistic is registered, counts up as awarded and is sent to the player. Isolated: it adds a player. */
public final class StatScenes {

    @Scene(maxTicks = 40, isolated = true)
    public void aStatisticIsRegisteredCountsUpAndReachesThePlayer(TestScene scene) {
        TestPlayer player = scene.addPlayer("Counter");
        scene.check("the statistics are registered", () -> scene.assertTrue(TestStats.PINGS.isRegistered() && TestStats.WALKED.isRegistered(), "registered"));
        scene.check("the game holds the distance statistic with its formatter from registration on", () -> scene.assertEquals(
                StatFormatter.DISTANCE.format(250), Stats.CUSTOM.get(TestStats.WALKED.id()).format(250), "formatted distance"));
        scene.check("it starts at nothing", () -> scene.assertEquals(0, JolCraftGameTests.stat().value(player.player(), TestStats.PINGS), "at first"));
        player.clearReceived();
        JolCraftGameTests.stat().award(player.player(), TestStats.PINGS, 3);
        JolCraftGameTests.stat().award(player.player(), TestStats.PINGS, 2);
        scene.check("awards add up", () -> scene.assertEquals(5, JolCraftGameTests.stat().value(player.player(), TestStats.PINGS), "after awards"));
        // The game sends changed statistics when it flushes them, on its own schedule; flushing here proves the statistic travels under its id.
        player.player().getStats().sendStats(player.player());
        scene.check("the player is sent the count", () -> scene.assertTrue(player.received(ClientboundAwardStatsPacket.class).stream()
                .anyMatch(packet -> packet.stats().getInt(TestStats.PINGS.stat()) == 5), "award packets " + player.received(ClientboundAwardStatsPacket.class).size()));
        scene.succeed();
    }
}
