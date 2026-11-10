package net.sievert.jolcraft.gametest.module.system.damage;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.damage.Affinity;
import net.sievert.jolcraft.module.system.damage.Stage;

/**
 * Damage affinity on real entities, against the three rows under {@code scene/data/jolcraft_gametest/jolcraft/affinity} (the scene pack, on only in the game-test server unless enabled by hand),
 * the first scenes to read a datapack registry in a running game. {@code fire}: fire damage except lava, resisted by attack
 * knockback and made worse by max absorption. {@code warmth}: fire and magic, resisted by explosion knockback resistance and
 * made worse by luck, which a mob does not have. {@code innate}: only {@code /kill}'s damage, with rabbits resisting through
 * knockback resistance and vulnerable through armor. Every attribute defaults to 0 on what a dev run meets once the pack is enabled and gear and
 * effects do not raise attack knockback; what does change in play: max absorption under Absorption, Blast Protection armour,
 * the mobs whose base attack knockback is above 0, and the Luck effect on a player. The one damage type of {@code innate}
 * is a command's.
 */
public final class DamageScenes {

    private static final float TOLERANCE = 1.0E-4F;
    private static final Identifier INNATE = Identifier.fromNamespaceAndPath("jolcraft_gametest", "innate");

    /** A cow whose health is large enough that no hit here kills it, so the health it loses is the damage it took. */
    private static Cow cow(TestScene scene) {
        Cow cow = scene.helper().spawn(EntityType.COW, new BlockPos(0, 1, 0));
        base(cow, Attributes.MAX_HEALTH, 1000.0D);
        cow.setHealth(1000.0F);
        return cow;
    }

    private static void base(LivingEntity entity, Holder<Attribute> attribute, double base) {
        entity.getAttribute(attribute).setBaseValue(base);
    }

    private static float dealt(TestScene scene, LivingEntity entity, DamageSource source, float amount) {
        entity.invulnerableTime = 0;
        float before = entity.getHealth();
        entity.hurtServer(scene.level(), source, amount);
        return before - entity.getHealth();
    }

    private static void assertNear(TestScene scene, float expected, float actual, String what) {
        scene.assertTrue(Math.abs(expected - actual) < TOLERANCE, what + ": expected " + expected + ", was " + actual);
    }

    private static long modifiers(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
        return entity.getAttribute(attribute).getModifiers().stream().filter(modifier -> modifier.is(id)).count();
    }

    // ------------------------------------------------------------------ rows

    @Scene(maxTicks = 20)
    public void aRowGivesOnePlusVulnerabilityTimesOneMinusResistance(TestScene scene) {
        Cow cow = cow(scene);
        base(cow, Attributes.ATTACK_KNOCKBACK, 0.5D);
        base(cow, Attributes.MAX_ABSORPTION, 0.25D);
        scene.check("fire damage is 10 times 1.25 times 0.5", () -> assertNear(scene, 6.25F, dealt(scene, cow, scene.level().damageSources().onFire(), 10.0F), "damage"));
        scene.check("magic damage is not in the row", () -> assertNear(scene, 10.0F, dealt(scene, cow, scene.level().damageSources().magic(), 10.0F), "damage"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aTypeInTwoRowsTakesBoth(TestScene scene) {
        Cow cow = cow(scene);
        base(cow, Attributes.ATTACK_KNOCKBACK, 0.5D);
        base(cow, Attributes.MAX_ABSORPTION, 0.25D);
        base(cow, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.2D);
        scene.check("fire damage takes fire and warmth", () -> assertNear(scene, 10.0F * 1.25F * 0.5F * 0.8F, dealt(scene, cow, scene.level().damageSources().onFire(), 10.0F), "damage"));
        scene.check("magic damage takes warmth alone", () -> assertNear(scene, 8.0F, dealt(scene, cow, scene.level().damageSources().magic(), 10.0F), "damage"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void exceptExcludesATypeTheRowOtherwiseHolds(TestScene scene) {
        Cow cow = cow(scene);
        base(cow, Attributes.ATTACK_KNOCKBACK, 0.5D);
        base(cow, Attributes.MAX_ABSORPTION, 0.25D);
        scene.check("lava is in the fire tag and excepted from the row", () -> assertNear(scene, 10.0F, dealt(scene, cow, scene.level().damageSources().lava(), 10.0F), "damage"));
        scene.check("burning is not excepted", () -> assertNear(scene, 6.25F, dealt(scene, cow, scene.level().damageSources().onFire(), 10.0F), "damage"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aRowWhoseAttributesAreAbsentOrZeroChangesNothing(TestScene scene) {
        Cow cow = cow(scene);
        scene.check("the cow has no luck", () -> scene.assertFalse(cow.getAttributes().hasAttribute(Attributes.LUCK), "has the attribute"));
        scene.check("every attribute at 0 leaves fire damage as it was", () -> assertNear(scene, 10.0F, dealt(scene, cow, scene.level().damageSources().onFire(), 10.0F), "damage"));
        scene.check("and magic damage", () -> assertNear(scene, 10.0F, dealt(scene, cow, scene.level().damageSources().magic(), 10.0F), "damage"));
        base(cow, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.5D);
        scene.check("the resistance alone counts, the absent vulnerability as 0", () -> assertNear(scene, 5.0F, dealt(scene, cow, scene.level().damageSources().magic(), 10.0F), "damage"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ innate

    @Scene(maxTicks = 20)
    public void anInnateEntryGivesItsTypeTheGreatestShareAndNoOtherType(TestScene scene) {
        Rabbit rabbit = scene.helper().spawn(EntityType.RABBIT, new BlockPos(0, 1, 0));
        Cow cow = cow(scene);
        scene.check("a rabbit has the greatest resistance and the greatest vulnerability of the two entries", () -> {
            scene.assertEquals(0.6D, rabbit.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), "knockback resistance");
            scene.assertEquals(0.5D, rabbit.getAttributeValue(Attributes.ARMOR), "armor");
            scene.assertEquals(0.6D, rabbit.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(Affinity.resistanceModifier(INNATE)).amount(), "modifier");
        });
        scene.check("a cow has none", () -> {
            scene.assertEquals(0L, modifiers(cow, Attributes.KNOCKBACK_RESISTANCE, Affinity.resistanceModifier(INNATE)), "resistance modifiers");
            scene.assertEquals(0L, modifiers(cow, Attributes.ARMOR, Affinity.vulnerabilityModifier(INNATE)), "vulnerability modifiers");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void postingTheJoinAgainDoesNotThrowAndLeavesOneModifier(TestScene scene) {
        Rabbit rabbit = scene.helper().spawn(EntityType.RABBIT, new BlockPos(0, 1, 0));
        NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(rabbit, scene.level()));
        NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(rabbit, scene.level()));
        scene.check("one modifier of each, at the same amount", () -> {
            scene.assertEquals(1L, modifiers(rabbit, Attributes.KNOCKBACK_RESISTANCE, Affinity.resistanceModifier(INNATE)), "resistance modifiers");
            scene.assertEquals(1L, modifiers(rabbit, Attributes.ARMOR, Affinity.vulnerabilityModifier(INNATE)), "vulnerability modifiers");
            AttributeInstance armor = rabbit.getAttribute(Attributes.ARMOR);
            scene.assertEquals(0.5D, armor.getValue(), "armor");
        });
        scene.succeed();
    }

    // ------------------------------------------------------------------ stage

    @Scene(maxTicks = 20)
    public void aFinalListenerSeesTheDamageAfterAffinityAndAHigherOneBefore(TestScene scene) {
        Cow cow = cow(scene);
        base(cow, Attributes.ATTACK_KNOCKBACK, 0.5D);
        base(cow, Attributes.MAX_ABSORPTION, 0.25D);
        float[] before = {-1.0F};
        float[] after = {-1.0F};
        Consumer<LivingDamageEvent.Pre> first = event -> {
            if (event.getEntity() == cow) {
                before[0] = event.getNewDamage();
            }
        };
        Consumer<LivingDamageEvent.Pre> last = event -> {
            if (event.getEntity() == cow) {
                after[0] = event.getNewDamage();
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, first);
        NeoForge.EVENT_BUS.addListener(Stage.FINAL, last);
        scene.onEnd(() -> {
            NeoForge.EVENT_BUS.unregister(first);
            NeoForge.EVENT_BUS.unregister(last);
        });
        cow.hurtServer(scene.level(), scene.level().damageSources().onFire(), 10.0F);
        scene.check("a listener ahead of the affinity stage sees the damage as it came", () -> assertNear(scene, 10.0F, before[0], "damage"));
        scene.check("the final stage sees it after the row", () -> assertNear(scene, 6.25F, after[0], "damage"));
        scene.succeed();
    }
}
