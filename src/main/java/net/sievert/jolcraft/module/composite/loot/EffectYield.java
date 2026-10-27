package net.sievert.jolcraft.module.composite.loot;

import java.util.List;
import java.util.Objects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.storage.loot.LootContext;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/** An effect instance; each roll yields a copy, so the system may apply or merge it freely. */
@Value
public record EffectYield(MobEffectInstance effect, List<Identifier> hooks) implements Yield {

    public static final MapCodec<EffectYield> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MobEffectInstance.CODEC.fieldOf(Dictionary.EFFECT).forGetter(EffectYield::effect),
            Identifier.CODEC.listOf().optionalFieldOf(Dictionary.HOOKS, List.of()).forGetter(EffectYield::hooks)).apply(instance, EffectYield::new));

    public EffectYield {
        Objects.requireNonNull(effect, "an effect");
        hooks = List.copyOf(hooks);
    }

    public static EffectYield of(MobEffectInstance effect, Identifier... hooks) {
        return new EffectYield(effect, List.of(hooks));
    }

    @Override
    public MapCodec<EffectYield> codec() {
        return CODEC;
    }

    @Override
    public List<? extends Yielded> roll(LootContext context) {
        return List.of(new Yielded.Effect(new MobEffectInstance(effect)));
    }
}
