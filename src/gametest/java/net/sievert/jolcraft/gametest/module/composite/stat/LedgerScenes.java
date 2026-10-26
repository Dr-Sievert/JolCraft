package net.sievert.jolcraft.gametest.module.composite.stat;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.module.primitive.stat.TestStats;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * The ledger composite on the real server: a player who is online is awarded at once, an award for a player who is
 * absent waits in the book, marked dirty for the save, and is granted the moment that player joins.
 */
public final class LedgerScenes {

    @Scene(maxTicks = 40, isolated = true)
    public void anOnlinePlayerIsAwardedAtOnceAndAnAbsentOneWhenTheyJoin(TestScene scene) {
        MinecraftServer server = scene.level().getServer();
        TestPlayer here = scene.addPlayer("Here");
        int before = JolCraftGameTests.stat().value(here.player(), TestStats.PINGS);
        JolCraftGameTests.ledger().award(server, TestLedgers.BOOK, here.player().getUUID(), TestStats.PINGS, 2);
        scene.check("an online player is awarded at once", () -> scene.assertEquals(before + 2, JolCraftGameTests.stat().value(here.player(), TestStats.PINGS), "pings"));
        scene.check("and nothing waits for them", () -> scene.assertEquals(0, JolCraftGameTests.ledger().waiting(server, TestLedgers.BOOK, here.player().getUUID()), "waiting"));

        UUID away = UUID.randomUUID();
        TestLedgers.BOOK.data().get(server).setDirty(false);
        JolCraftGameTests.ledger().award(server, TestLedgers.BOOK, away, TestStats.PINGS, 3);
        JolCraftGameTests.ledger().award(server, TestLedgers.BOOK, away, TestStats.WALKED, 7);
        scene.check("awards for an absent player wait in the book", () -> scene.assertEquals(2, JolCraftGameTests.ledger().waiting(server, TestLedgers.BOOK, away), "waiting"));
        scene.check("and the book is marked for the save", () -> scene.assertTrue(TestLedgers.BOOK.data().get(server).isDirty(), "dirty"));

        TestPlayer joiner = scene.addPlayer("Joiner", away); // the login event is posted inside the placement, so the grant is done when this returns
        scene.check("the join granted both awards", () -> scene.assertTrue(
                JolCraftGameTests.stat().value(joiner.player(), TestStats.PINGS) == 3 && JolCraftGameTests.stat().value(joiner.player(), TestStats.WALKED) == 7, "pings and walked"));
        scene.check("and the book is empty for them", () -> scene.assertEquals(0, JolCraftGameTests.ledger().waiting(server, TestLedgers.BOOK, away), "waiting"));
        scene.succeed();
    }
}
