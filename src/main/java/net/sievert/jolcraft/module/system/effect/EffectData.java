package net.sievert.jolcraft.module.system.effect;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * The data the effect rules read, as data map types on the mob effect and attribute registries, all server-only. The
 * features that own effects and attributes fill them in their data packs; nothing here names a mod effect.
 * {@link EffectContent} registers the types.
 */
@Value
public final class EffectData {

    // Declared first: the nested codecs below read it while the data map types are being built.
    /** {@code MobEffectCategory} is a plain enum; this reads and writes its lowercase name and refuses any other. */
    public static final Codec<MobEffectCategory> CATEGORY = Codec.STRING.comapFlatMap(EffectData::category, category -> category.name().toLowerCase(Locale.ROOT));

    /** The effects that stack: a second application raises the amplifier instead of replacing the first. */
    public static final DataMapType<MobEffect, Stacking> STACKING = DataMapType.builder(
            Ids.of(Dictionary.JOLCRAFT, Dictionary.STACKING), Registries.MOB_EFFECT, Stacking.CODEC).build();
    /** The effects that, while an entity has them, keep other effects off it, and may remove those already there. */
    public static final DataMapType<MobEffect, Protection> EFFECT_PROTECTION = DataMapType.builder(
            Ids.of(Dictionary.JOLCRAFT, Dictionary.EFFECT, Dictionary.PROTECTION), Registries.MOB_EFFECT, Protection.EFFECT_CODEC).build();
    /** The attributes whose value, clamped to 0 to 1, is the chance an entity refuses the effects they list. */
    public static final DataMapType<Attribute, Protection> ATTRIBUTE_PROTECTION = DataMapType.builder(
            Ids.of(Dictionary.JOLCRAFT, Dictionary.ATTRIBUTE, Dictionary.PROTECTION), Registries.ATTRIBUTE, Protection.ATTRIBUTE_CODEC).build();
    /** The attributes that lengthen or shorten the duration of the effects they filter. */
    public static final DataMapType<Attribute, Scaling> SCALING = DataMapType.builder(
            Ids.of(Dictionary.JOLCRAFT, Dictionary.SCALING), Registries.ATTRIBUTE, Scaling.CODEC).build();

    private EffectData() {
    }

    private static DataResult<MobEffectCategory> category(String name) {
        for (MobEffectCategory category : MobEffectCategory.values()) {
            if (category.name().toLowerCase(Locale.ROOT).equals(name)) {
                return DataResult.success(category);
            }
        }
        return DataResult.error(() -> "Unknown effect category '" + name + "'");
    }

    /** What a stacking effect may grow to. */
    public record Stacking(Optional<Integer> maxAmplifier) {

        public static final Codec<Stacking> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, MobEffectInstance.MAX_AMPLIFIER).optionalFieldOf(ModString.underscored(Dictionary.MAX, Dictionary.AMPLIFIER)).forGetter(Stacking::maxAmplifier)
        ).apply(instance, Stacking::new));

        /** The amplifier and duration of one application of this effect on top of another. */
        public record Merged(int amplifier, int duration) {
        }

        /**
         * The amplifiers add plus one, up to the cap but never below the amplifier already there and never above 255. The
         * duration is infinite if the incoming one is, the incoming duration if only the old one is infinite (the game's
         * own update then pushes the held infinite instance down the chain instead of the merge making the stack permanent),
         * and otherwise the longer of the two.
         */
        public Merged merge(int oldAmplifier, int oldDuration, int addedAmplifier, int addedDuration) {
            int cap = Math.max(maxAmplifier.orElse(MobEffectInstance.MAX_AMPLIFIER), oldAmplifier);
            int amplifier = Math.min(Math.min(oldAmplifier + addedAmplifier + 1, cap), MobEffectInstance.MAX_AMPLIFIER);
            int duration;
            if (addedDuration == MobEffectInstance.INFINITE_DURATION || oldDuration == MobEffectInstance.INFINITE_DURATION) {
                duration = addedDuration;
            } else {
                duration = Math.max(oldDuration, addedDuration);
            }
            return new Merged(amplifier, duration);
        }
    }

    /** The effects a protector keeps off the entity, and for an effect protector whether adding it removes those already there. */
    public record Protection(HolderSet<MobEffect> blocks, boolean cleanse) {

        public static final Codec<Protection> EFFECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf(Dictionary.BLOCKS).forGetter(Protection::blocks),
                Codec.BOOL.optionalFieldOf(Dictionary.CLEANSE, false).forGetter(Protection::cleanse)
        ).apply(instance, Protection::new));
        /** An attribute cannot cleanse: nothing tells when it changed. */
        public static final Codec<Protection> ATTRIBUTE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf(Dictionary.BLOCKS).forGetter(Protection::blocks)
        ).apply(instance, blocks -> new Protection(blocks, false)));

        /** Whether a protector of this strength refuses: never at 0, always at 1 or more without a draw, else one draw of {@code random}. */
        public static boolean roll(double strength, RandomSource random) {
            if (strength <= 0.0D) {
                return false;
            }
            return strength >= 1.0D || random.nextDouble() < strength;
        }
    }

    /** Which way an attribute moves the duration. */
    public enum Direction implements StringRepresentable {
        LENGTHEN(Dictionary.LENGTHEN),
        SHORTEN(Dictionary.SHORTEN);

        public static final Codec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);

        private final String serialized;

        Direction(String serialized) {
            this.serialized = serialized;
        }

        @Override
        public String getSerializedName() {
            return serialized;
        }
    }

    /** The effects an attribute scales, by category, by list or by both at once, and which way. */
    public record Scaling(Optional<MobEffectCategory> category, Optional<HolderSet<MobEffect>> effects, Direction direction) {

        public static final Codec<Scaling> CODEC = RecordCodecBuilder.<Scaling>create(instance -> instance.group(
                CATEGORY.optionalFieldOf(Dictionary.CATEGORY).forGetter(Scaling::category),
                RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).optionalFieldOf(Dictionary.EFFECTS).forGetter(Scaling::effects),
                Direction.CODEC.fieldOf(Dictionary.DIRECTION).forGetter(Scaling::direction)
        ).apply(instance, Scaling::new)).validate(scaling -> scaling.category.isPresent() || scaling.effects.isPresent()
                ? DataResult.success(scaling) : DataResult.error(() -> "A scaling needs a category, a list of effects or both"));

        /** Whether the effect is one this entry scales: every filter it has must hold. */
        public boolean matches(Holder<MobEffect> effect) {
            return category.map(wanted -> effect.value().getCategory() == wanted).orElse(true)
                    && effects.map(listed -> listed.contains(effect)).orElse(true);
        }

        /** The factor an attribute of this value puts on a duration: {@code 1 + value} to lengthen, {@code 1 - value} (0 to 1) to shorten. */
        public double factor(double attributeValue) {
            return direction == Direction.LENGTHEN ? 1.0D + Math.max(0.0D, attributeValue) : 1.0D - Mth.clamp(attributeValue, 0.0D, 1.0D);
        }

        /** Whether an attribute of this value keeps every effect this entry matches off the entity: a shortening of 1 or more. */
        public boolean immune(double attributeValue) {
            return direction == Direction.SHORTEN && attributeValue >= 1.0D;
        }

        /** A finite duration times a factor, rounded, at least one tick. */
        public static int scale(int duration, double factor) {
            return Math.max(1, (int) Math.round(duration * factor));
        }
    }
}
