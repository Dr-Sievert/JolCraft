package net.sievert.jolcraft.gametest.module.primitive.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.advancement.Advancement;
import net.sievert.jolcraft.module.primitive.advancement.TriggerKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** The criterion kind the advancement scenes fire; the game-test mod declares it in its root and its data pack holds three advancements. */
public final class TestTriggers {

    /** Fired with a count; an advancement asks for at least so many. */
    public record Pinged(Optional<ContextAwarePredicate> player, int atLeast) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Pinged> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Pinged::player),
                Codec.INT.optionalFieldOf("at_least", 0).forGetter(Pinged::atLeast)).apply(instance, Pinged::new));
    }

    public static final TriggerKey<Pinged> PINGED = TriggerKey.of(Ids.of(JolCraftGameTests.MOD_ID, "pinged"), Pinged.CODEC);

    private TestTriggers() {
    }

    public static void declare(Advancement advancement) {
        advancement.declare(PINGED);
    }
}
