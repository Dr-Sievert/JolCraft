package net.sievert.jolcraft.module.system.damage;

import java.util.List;
import java.util.function.ToDoubleFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;

import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * One entry of the {@code jolcraft:affinity} datapack registry: the damage types it governs, and the two attributes
 * that bend them. An entity's resistance (clamped to 0 to 1) takes that share off and its vulnerability (0 or more)
 * adds that share on, for every damage type in {@code damage} and not in {@code except}; {@code innate} gives whole
 * kinds of entity a share of either without their carrying the attribute by any other means. Rows multiply.
 */
@Value
public record Affinity(HolderSet<DamageType> damage, HolderSet<DamageType> except, Holder<Attribute> resistance, Holder<Attribute> vulnerability, List<Innate> innate) {

    public static final Codec<Affinity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).fieldOf(Dictionary.DAMAGE).forGetter(Affinity::damage),
            RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).optionalFieldOf(Dictionary.EXCEPT, HolderSet.empty()).forGetter(Affinity::except),
            Attribute.CODEC.fieldOf(Dictionary.RESISTANCE).forGetter(Affinity::resistance),
            Attribute.CODEC.fieldOf(Dictionary.VULNERABILITY).forGetter(Affinity::vulnerability),
            Innate.CODEC.listOf().optionalFieldOf(Dictionary.INNATE, List.of()).forGetter(Affinity::innate)
    ).apply(instance, Affinity::new));

    /** A share of resistance and of vulnerability that every entity of the listed types has by nature. */
    public record Innate(HolderSet<EntityType<?>> entities, double resistance, double vulnerability) {

        public static final Codec<Innate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf(Dictionary.ENTITIES).forGetter(Innate::entities),
                Codec.doubleRange(0.0D, 1.0D).optionalFieldOf(Dictionary.RESISTANCE, 0.0D).forGetter(Innate::resistance),
                Codec.doubleRange(0.0D, Double.MAX_VALUE).optionalFieldOf(Dictionary.VULNERABILITY, 0.0D).forGetter(Innate::vulnerability)
        ).apply(instance, Innate::new));
    }

    /** The shares an entity type has by nature in one row: the greatest of each among the entries that hold it. */
    public record Strength(double resistance, double vulnerability) {
    }

    /** Whether this row governs the damage type: it is in {@code damage} and not in {@code except}. */
    public boolean applies(Holder<DamageType> type) {
        return damage.contains(type) && !except.contains(type);
    }

    /** The multiplier on damage of attributes at these values: {@code (1 + max(0, vulnerability)) * (1 - clamp(resistance, 0, 1))}. */
    public static double factor(double resistanceValue, double vulnerabilityValue) {
        return (1.0D + Math.max(0.0D, vulnerabilityValue)) * (1.0D - Mth.clamp(resistanceValue, 0.0D, 1.0D));
    }

    /** What the rows that govern the damage type multiply it by, reading each attribute through {@code attribute} (0 for an entity without it). */
    public static double multiplier(Iterable<Affinity> rows, Holder<DamageType> type, ToDoubleFunction<Holder<Attribute>> attribute) {
        double multiplier = 1.0D;
        for (Affinity row : rows) {
            if (row.applies(type)) {
                multiplier *= factor(attribute.applyAsDouble(row.resistance), attribute.applyAsDouble(row.vulnerability));
            }
        }
        return multiplier;
    }

    /** The shares an entity of this type has by nature in this row: the greatest resistance and the greatest vulnerability among the entries that hold the type. */
    public Strength innate(Holder<EntityType<?>> type) {
        double resistanceShare = 0.0D;
        double vulnerabilityShare = 0.0D;
        for (Innate entry : innate) {
            if (entry.entities.contains(type)) {
                resistanceShare = Math.max(resistanceShare, entry.resistance);
                vulnerabilityShare = Math.max(vulnerabilityShare, entry.vulnerability);
            }
        }
        return new Strength(resistanceShare, vulnerabilityShare);
    }

    /** The id of the transient modifier a row's innate resistance is written under: the row's id and words. */
    public static Identifier resistanceModifier(Identifier row) {
        return Ids.of(row.getNamespace(), row.getPath(), Dictionary.INNATE, Dictionary.RESISTANCE);
    }

    /** The id of the transient modifier a row's innate vulnerability is written under. */
    public static Identifier vulnerabilityModifier(Identifier row) {
        return Ids.of(row.getNamespace(), row.getPath(), Dictionary.INNATE, Dictionary.VULNERABILITY);
    }
}
