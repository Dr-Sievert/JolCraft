package net.sievert.jolcraft.module.primitive.stat;

import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping and the key; the game's registries are frozen here, so scenes in the game-test mod award a real player. */
class ModStatTest {

    private final StatKey trades = StatKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "trades"));
    private final StatKey walked = StatKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "walked")).shownAs(StatFormatter.DISTANCE);
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void statisticsAreCountedOncePerIdAndNamedUnderTheGamesCategory() {
        ModStat stat = new ModStat();
        stat.declare(trades);
        stat.declare(walked);
        Assertions.assertEquals(2, stat.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> stat.declare(trades.shownAs(StatFormatter.TIME)));
        Assertions.assertEquals("stat.jolcraft.trades", trades.name());
        Assertions.assertSame(StatFormatter.DEFAULT, trades.formatter());
        Assertions.assertSame(StatFormatter.DISTANCE, walked.formatter());
        Assertions.assertFalse(trades.isRegistered());
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, trades::stat);
        Assertions.assertTrue(refused.getMessage().contains("declared"), refused.getMessage());
    }

    @Test
    void awardingAnUndeclaredKeyIsRefusedBeforeTheGameIsAsked() {
        ModStat stat = new ModStat();
        stat.declare(trades);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stat.award(null, walked, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> stat.value(null, walked));
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModStat().declare(StatKey.of(Identifier.withDefaultNamespace("trades"))));
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModStat stat = new ModStat();
        stat.declare(trades);
        stat.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> stat.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> stat.declare(walked));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.STAT, new ModStat().id());
    }
}
