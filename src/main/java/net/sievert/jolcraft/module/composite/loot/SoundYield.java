package net.sievert.jolcraft.module.composite.loot;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/** A sound, with the volume and pitch the providers roll; the system plays it where it sees fit. */
@Value
public record SoundYield(Holder<SoundEvent> sound, SoundSource source, NumberProvider volume, NumberProvider pitch, List<Identifier> hooks) implements Yield {

    /** The game's sound sources by their names, which the game gives them but no codec. */
    public static final Codec<SoundSource> SOURCE_CODEC = Codec.stringResolver(SoundSource::getName,
            name -> Arrays.stream(SoundSource.values()).filter(source -> source.getName().equals(name)).findFirst().orElse(null));

    public static final MapCodec<SoundYield> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SoundEvent.CODEC.fieldOf(Dictionary.SOUND).forGetter(SoundYield::sound),
            SOURCE_CODEC.optionalFieldOf(Dictionary.SOURCE, SoundSource.PLAYERS).forGetter(SoundYield::source),
            NumberProviders.CODEC.optionalFieldOf(Dictionary.VOLUME, ConstantValue.exactly(1.0F)).forGetter(SoundYield::volume),
            NumberProviders.CODEC.optionalFieldOf(Dictionary.PITCH, ConstantValue.exactly(1.0F)).forGetter(SoundYield::pitch),
            Identifier.CODEC.listOf().optionalFieldOf(Dictionary.HOOKS, List.of()).forGetter(SoundYield::hooks)).apply(instance, SoundYield::new));

    public SoundYield {
        Objects.requireNonNull(sound, "a sound");
        Objects.requireNonNull(source, "a source");
        Objects.requireNonNull(volume, "a volume");
        Objects.requireNonNull(pitch, "a pitch");
        hooks = List.copyOf(hooks);
    }

    public static SoundYield of(Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, Identifier... hooks) {
        return new SoundYield(sound, source, ConstantValue.exactly(volume), ConstantValue.exactly(pitch), List.of(hooks));
    }

    @Override
    public MapCodec<SoundYield> codec() {
        return CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        Yield.super.validate(context);
        volume.validate(context.forField(Dictionary.VOLUME));
        pitch.validate(context.forField(Dictionary.PITCH));
    }

    @Override
    public List<? extends Yielded> roll(LootContext context) {
        return List.of(new Yielded.Sound(sound, source, volume.getFloat(context), pitch.getFloat(context)));
    }
}
