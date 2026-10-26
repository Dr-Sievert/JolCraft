package net.sievert.jolcraft.module.composite.stat;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.BusBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.primitive.data.DataKey;
import net.sievert.jolcraft.module.primitive.data.ModData;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.stat.ModStat;
import net.sievert.jolcraft.module.primitive.stat.StatKey;
import net.sievert.jolcraft.util.text.Dictionary;

/** The book's shape, the guards and the written form; awarding needs a server and is a scene in the game-test mod. */
class LedgerTest {

    private final StatKey brews = StatKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "brews"));
    private final StatKey trades = StatKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "trades"));
    private final Ledger.Book book = Ledger.Book.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, Dictionary.BOOK), brews, trades);

    private Ledger ledger(ModData data) {
        return new Ledger(new ModStat(), data, new ModLog(LogUtils.getLogger()));
    }

    @Test
    void aBookIsServerWideSavedDataUnderItsIdNamingItsStatistics() {
        Assertions.assertEquals(book.id(), book.data().id());
        Assertions.assertTrue(((DataKey.Spec<?>) book.data()).shared(), "one book for the server, not one per level");
        Assertions.assertEquals(List.of(brews.id(), trades.id()), List.copyOf(book.stats().keySet()).stream().sorted().toList());
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ledger.Book.of(book.id()), "no statistic");
        Assertions.assertThrows(IllegalArgumentException.class, () -> Ledger.Book.of(book.id(), brews, brews), "one statistic twice");
    }

    @Test
    void declaringReachesTheDataAndIsOverOnceAttached() {
        ModData data = new ModData();
        Ledger ledger = ledger(data);
        ledger.declare(book);
        Assertions.assertEquals(1, data.declared());
        Assertions.assertEquals(1, ledger.declared());
        Ledger.Book sameId = new Ledger.Book(book.id(), DataKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "other_book"), Ledger.Awards::new, Ledger.Awards.CODEC), book.stats());
        Assertions.assertTrue(Assertions.assertThrows(IllegalStateException.class, () -> ledger.declare(sameId)).getMessage().contains("declared twice"), "the same id twice is the ledger's own refusal");
        ledger.register(BusBuilder.builder().build());
        Assertions.assertThrows(IllegalStateException.class, () -> ledger.register(BusBuilder.builder().build()), "attached twice");
        Assertions.assertThrows(IllegalStateException.class, () -> ledger.declare(Ledger.Book.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "late"), brews)), "declared after attaching");
        Assertions.assertEquals(Dictionary.LEDGER, ledger.id());
    }

    @Test
    void anAwardIsForAStatisticOfTheBookAndAtLeastOne() {
        Ledger ledger = ledger(new ModData());
        StatKey other = StatKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "other"));
        UUID player = UUID.randomUUID();
        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.award(null, book, player, brews, 1)).getMessage().contains("never declared"), "an undeclared book");
        ledger.declare(book);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.award(null, book, player, other, 1), "not in the book");
        Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.award(null, book, player, brews, 0), "nothing to award");
    }

    @Test
    void waitingAwardsAreWrittenAndReadBackInOrder() {
        Ledger.Awards awards = new Ledger.Awards();
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        awards.add(new Ledger.Awards.Entry(one, brews.id(), 2));
        awards.add(new Ledger.Awards.Entry(two, trades.id(), 1));
        awards.add(new Ledger.Awards.Entry(one, trades.id(), 5));
        Assertions.assertTrue(awards.isDirty(), "an award marks the book dirty");
        JsonElement json = Ledger.Awards.CODEC.encodeStart(JsonOps.INSTANCE, awards).getOrThrow();
        Ledger.Awards read = Ledger.Awards.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        Assertions.assertEquals(3, read.size());
        Assertions.assertEquals(List.of(new Ledger.Awards.Entry(one, brews.id(), 2), new Ledger.Awards.Entry(one, trades.id(), 5)), read.waitingFor(one));
        Assertions.assertFalse(read.isDirty(), "a decoded book starts clean");
        Assertions.assertEquals(read.waitingFor(one), read.take(one));
        Assertions.assertTrue(read.isDirty(), "taking marks the book dirty");
        Assertions.assertEquals(List.of(), read.waitingFor(one));
        Assertions.assertEquals(1, read.size(), "the other player's award stays");
    }
}
