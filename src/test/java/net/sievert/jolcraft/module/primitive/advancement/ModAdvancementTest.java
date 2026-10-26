package net.sievert.jolcraft.module.primitive.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping and the key formulas; the game's registries are frozen here, so scenes in the game-test mod fire real criteria for a real player. */
class ModAdvancementTest {

    record Waved(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        static final Codec<Waved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Waved::player)).apply(instance, Waved::new));
    }

    private final TriggerKey<Waved> waved = TriggerKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "waved"), Waved.CODEC);
    private final TriggerKey<Waved> bowed = TriggerKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "bowed"), Waved.CODEC);
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void kindsAreCountedOncePerIdAndBuildTheirTrigger() {
        ModAdvancement advancement = new ModAdvancement();
        advancement.declare(waved);
        advancement.declare(bowed);
        Assertions.assertEquals(2, advancement.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> advancement.declare(waved));
        Assertions.assertSame(Waved.CODEC, waved.build().codec());
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, waved::trigger);
        Assertions.assertTrue(refused.getMessage().contains("declared"), refused.getMessage());
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModAdvancement().declare(TriggerKey.of(Identifier.withDefaultNamespace("waved"), Waved.CODEC)));
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModAdvancement advancement = new ModAdvancement();
        advancement.declare(waved);
        advancement.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> advancement.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> advancement.declare(bowed));
    }

    @Test
    void theTextKeysAreTheGamesCategoryThenTheNamespaceThenThePath() {
        Identifier id = Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "first_trade");
        Assertions.assertEquals("advancements.jolcraft.first_trade.title", Advancement.title(id));
        Assertions.assertEquals("advancements.jolcraft.first_trade.description", Advancement.description(id));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.ADVANCEMENT, new ModAdvancement().id());
    }
}
