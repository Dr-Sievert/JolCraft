package net.sievert.jolcraft.module.system.damage;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The affinity row: its codec and what it refuses, and the arithmetic the damage listener calls, none of which needs a level. */
class AffinityTest {

    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();
    private static final RegistryOps<JsonElement> OPS = RegistryOps.create(JsonOps.INSTANCE, LOOKUP);

    private static final Holder<DamageType> ON_FIRE = LOOKUP.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ON_FIRE);
    private static final Holder<DamageType> LAVA = LOOKUP.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.LAVA);
    private static final Holder<DamageType> MAGIC = LOOKUP.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC);

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    private static Affinity row(List<Holder<DamageType>> damage, List<Holder<DamageType>> except, Holder<Attribute> resistance, Holder<Attribute> vulnerability) {
        return new Affinity(HolderSet.direct(damage), HolderSet.direct(except), resistance, vulnerability, List.of());
    }

    private static double factor(Affinity row, Map<Holder<Attribute>, Double> values) {
        return Affinity.multiplier(List.of(row), ON_FIRE, attribute -> values.getOrDefault(attribute, 0.0D));
    }

    // ------------------------------------------------------------------ codec

    @Test
    void aRowDecodesAndDefaultsWhatItMay() {
        Affinity parsed = Affinity.CODEC.parse(OPS, json("""
                {"damage": ["minecraft:on_fire", "minecraft:magic"], "except": "minecraft:lava",
                 "resistance": "minecraft:armor_toughness", "vulnerability": "minecraft:max_absorption",
                 "innate": [{"entities": ["minecraft:rabbit"], "resistance": 0.5, "vulnerability": 2}]}""")).getOrThrow();
        Assertions.assertTrue(parsed.damage().contains(ON_FIRE) && parsed.damage().contains(MAGIC) && !parsed.damage().contains(LAVA));
        Assertions.assertTrue(parsed.except().contains(LAVA));
        Assertions.assertEquals(Attributes.ARMOR_TOUGHNESS, parsed.resistance());
        Assertions.assertEquals(Attributes.MAX_ABSORPTION, parsed.vulnerability());
        Assertions.assertEquals(0.5D, parsed.innate().getFirst().resistance());
        Assertions.assertEquals(2.0D, parsed.innate().getFirst().vulnerability());

        Affinity bare = Affinity.CODEC.parse(OPS, json("""
                {"damage": "minecraft:on_fire", "resistance": "minecraft:armor_toughness", "vulnerability": "minecraft:max_absorption"}""")).getOrThrow();
        Assertions.assertEquals(0, bare.except().size(), "except is empty by default");
        Assertions.assertTrue(bare.innate().isEmpty(), "innate is empty by default");
    }

    @Test
    void aRowNeedsItsDamageAndItsTwoAttributes() {
        Assertions.assertTrue(Affinity.CODEC.parse(OPS, json("{\"resistance\": \"minecraft:armor_toughness\", \"vulnerability\": \"minecraft:max_absorption\"}")).isError());
        Assertions.assertTrue(Affinity.CODEC.parse(OPS, json("{\"damage\": \"minecraft:on_fire\", \"vulnerability\": \"minecraft:max_absorption\"}")).isError());
        Assertions.assertTrue(Affinity.CODEC.parse(OPS, json("{\"damage\": \"minecraft:on_fire\", \"resistance\": \"minecraft:armor_toughness\"}")).isError());
        Assertions.assertTrue(Affinity.CODEC.parse(OPS, json("""
                {"damage": "minecraft:on_fire", "resistance": "minecraft:no_such_attribute", "vulnerability": "minecraft:max_absorption"}""")).isError());
    }

    @Test
    void anInnateResistanceAboveOneOrAnyNegativeShareIsRefused() {
        Assertions.assertTrue(Affinity.Innate.CODEC.parse(OPS, json("{\"entities\": \"minecraft:rabbit\", \"resistance\": 1}")).isSuccess(), "1 is allowed");
        Assertions.assertTrue(Affinity.Innate.CODEC.parse(OPS, json("{\"entities\": \"minecraft:rabbit\", \"resistance\": 1.01}")).isError());
        Assertions.assertTrue(Affinity.Innate.CODEC.parse(OPS, json("{\"entities\": \"minecraft:rabbit\", \"resistance\": -0.1}")).isError());
        Assertions.assertTrue(Affinity.Innate.CODEC.parse(OPS, json("{\"entities\": \"minecraft:rabbit\", \"vulnerability\": -0.1}")).isError());
        Assertions.assertTrue(Affinity.Innate.CODEC.parse(OPS, json("{\"entities\": \"minecraft:rabbit\", \"vulnerability\": 5}")).isSuccess(), "a vulnerability has no upper bound");
        Assertions.assertTrue(Affinity.Innate.CODEC.parse(OPS, json("{\"resistance\": 0.5}")).isError(), "entities is required");
    }

    // ------------------------------------------------------------------ arithmetic

    @Test
    void aRowGivesOnePlusVulnerabilityTimesOneMinusResistance() {
        Affinity row = row(List.of(ON_FIRE), List.of(), Attributes.ARMOR_TOUGHNESS, Attributes.MAX_ABSORPTION);
        Assertions.assertEquals(1.25D * 0.5D, factor(row, Map.of(Attributes.ARMOR_TOUGHNESS, 0.5D, Attributes.MAX_ABSORPTION, 0.25D)), 1.0E-9D);
        Assertions.assertEquals(1.0D, factor(row, Map.of()), "an entity without either attribute counts 0");
        Assertions.assertEquals(1.25D, factor(row, Map.of(Attributes.MAX_ABSORPTION, 0.25D)), 1.0E-9D, "an absent resistance counts 0");
    }

    @Test
    void resistanceIsClampedToZeroToOneAndVulnerabilityToZeroOrMore() {
        Affinity row = row(List.of(ON_FIRE), List.of(), Attributes.ARMOR_TOUGHNESS, Attributes.MAX_ABSORPTION);
        Assertions.assertEquals(0.0D, factor(row, Map.of(Attributes.ARMOR_TOUGHNESS, 3.0D)), "a resistance of more than 1 is immunity, not healing");
        Assertions.assertEquals(1.0D, factor(row, Map.of(Attributes.ARMOR_TOUGHNESS, -2.0D)), "a negative resistance is none");
        Assertions.assertEquals(1.0D, factor(row, Map.of(Attributes.MAX_ABSORPTION, -2.0D)), "a negative vulnerability is none");
        Assertions.assertEquals(4.0D, factor(row, Map.of(Attributes.MAX_ABSORPTION, 3.0D)), 1.0E-9D, "a vulnerability has no upper bound");
    }

    @Test
    void rowsMultiplyAndExceptExcludes() {
        Affinity first = row(List.of(ON_FIRE, LAVA), List.of(LAVA), Attributes.ARMOR_TOUGHNESS, Attributes.MAX_ABSORPTION);
        Affinity second = row(List.of(ON_FIRE, MAGIC), List.of(), Attributes.ATTACK_KNOCKBACK, Attributes.KNOCKBACK_RESISTANCE);
        Map<Holder<Attribute>, Double> values = Map.of(Attributes.ARMOR_TOUGHNESS, 0.5D, Attributes.MAX_ABSORPTION, 0.25D, Attributes.ATTACK_KNOCKBACK, 0.2D);
        Assertions.assertEquals(1.25D * 0.5D * 0.8D, Affinity.multiplier(List.of(first, second), ON_FIRE, attribute -> values.getOrDefault(attribute, 0.0D)), 1.0E-9D, "a type in two rows takes both");
        Assertions.assertEquals(0.8D, Affinity.multiplier(List.of(first, second), MAGIC, attribute -> values.getOrDefault(attribute, 0.0D)), 1.0E-9D, "only the row that holds it");
        Assertions.assertEquals(1.0D, Affinity.multiplier(List.of(first, second), LAVA, attribute -> values.getOrDefault(attribute, 0.0D)), "except excludes lava from the first, the second never held it");
        Assertions.assertEquals(1.0D, Affinity.multiplier(List.of(), ON_FIRE, _ -> 1.0D), "no rows, no change");
    }

    @Test
    void anInnateStrengthIsTheGreatestOfEachAmongTheEntriesThatHoldTheType() {
        Holder<EntityType<?>> rabbit = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityType.RABBIT);
        Holder<EntityType<?>> cow = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityType.COW);
        Holder<EntityType<?>> pig = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityType.PIG);
        Affinity row = new Affinity(HolderSet.direct(ON_FIRE), HolderSet.empty(), Attributes.ARMOR_TOUGHNESS, Attributes.MAX_ABSORPTION, List.of(
                new Affinity.Innate(HolderSet.direct(rabbit, cow), 0.2D, 0.5D),
                new Affinity.Innate(HolderSet.direct(rabbit), 0.6D, 0.1D),
                new Affinity.Innate(HolderSet.direct(cow), 0.0D, 0.0D)));
        Assertions.assertEquals(new Affinity.Strength(0.6D, 0.5D), row.innate(rabbit), "the greatest resistance and the greatest vulnerability, from different entries");
        Assertions.assertEquals(new Affinity.Strength(0.2D, 0.5D), row.innate(cow));
        Assertions.assertEquals(new Affinity.Strength(0.0D, 0.0D), row.innate(pig), "a type no entry holds has none");
    }

    @Test
    void theModifierIdsAreTheRowsIdAndAWord() {
        Identifier id = Identifier.fromNamespaceAndPath("jolcraft_gametest", "innate");
        Assertions.assertEquals(Identifier.fromNamespaceAndPath("jolcraft_gametest", "innate_innate_resistance"), Affinity.resistanceModifier(id));
        Assertions.assertEquals(Identifier.fromNamespaceAndPath("jolcraft_gametest", "innate_innate_vulnerability"), Affinity.vulnerabilityModifier(id));
    }
}
