package net.sievert.jolcraft.gametest.module.composite.stat;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.module.primitive.stat.TestStats;
import net.sievert.jolcraft.module.composite.stat.Ledger;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.util.text.Dictionary;

/** The game-test mod's book, naming the statistics its scenes award. */
public final class TestLedgers {

    public static final Ledger.Book BOOK = Ledger.Book.of(Ids.of(JolCraftGameTests.MOD_ID, Dictionary.BOOK), TestStats.PINGS, TestStats.WALKED);

    private TestLedgers() {
    }

    public static void declare(Ledger ledger) {
        ledger.declare(BOOK);
    }
}
