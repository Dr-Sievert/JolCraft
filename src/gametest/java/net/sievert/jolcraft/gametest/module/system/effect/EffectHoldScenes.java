package net.sievert.jolcraft.gametest.module.system.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.effect.Effect;
import net.sievert.jolcraft.module.system.effect.Effect.Held;
import net.sievert.jolcraft.module.system.effect.EffectTags;

/**
 * Cure and hold on real entities: a source's infinite effect through every shape of the chain, and what a clear and a
 * cure leave. The incurable tag fixture lists unluck; the dev mod's cure tag lists speed and unluck. Speed has no
 * other rule, so it serves where no fixture should act.
 */
public final class EffectHoldScenes {

    private static final Identifier SOURCE = Identifier.fromNamespaceAndPath("jolcraft_gametest", "source");
    private static final Identifier OTHER = Identifier.fromNamespaceAndPath("jolcraft_gametest", "other");
    private static final TagKey<MobEffect> CURE = TagKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath("jolcraft_gametest", "cure"));

    private static Cow cow(TestScene scene) {
        return scene.helper().spawn(EntityType.COW, new BlockPos(0, 1, 0));
    }

    private static Held infinite(Holder<MobEffect> effect, int amplifier) {
        return new Held(effect, amplifier, false, true, true);
    }

    private static MobEffectInstance of(Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier);
    }

    private static Effect effect() {
        return JolCraftGameTests.effect();
    }

    // ------------------------------------------------------------------ hold

    @Scene(maxTicks = 20)
    public void holdAddsAnInfiniteInstanceAndRecordsTheSourceAndASecondHoldChangesNothing(TestScene scene) {
        Cow cow = cow(scene);
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), true);
        MobEffectInstance first = cow.getEffect(MobEffects.SPEED);
        scene.check("an infinite instance with the held shape is there and the source recorded", () -> {
            scene.assertTrue(first != null && first.isInfiniteDuration() && first.getAmplifier() == 0 && first.isVisible() && first.showIcon() && !first.isAmbient(), "infinite speed");
            scene.assertTrue(Effect.OWNED.get(cow).has(SOURCE), "recorded");
        });
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), true);
        scene.check("a second hold changes nothing", () -> {
            scene.assertTrue(cow.getEffect(MobEffects.SPEED) == first, "the same instance");
            scene.assertEquals(1, Effect.OWNED.get(cow).sources().size(), "sources");
        });
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), false);
        scene.check("inactive removes it and forgets the source, leaving no attachment", () -> {
            scene.assertFalse(cow.hasEffect(MobEffects.SPEED), "present");
            scene.assertFalse(Effect.OWNED.isSet(cow), "attachment set");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void anInactiveCallOnAFreshEntityLeavesNoAttachment(TestScene scene) {
        Cow cow = cow(scene);
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), false);
        scene.check("nothing recorded, nothing created", () -> scene.assertFalse(Effect.OWNED.isSet(cow), "attachment set"));
        scene.succeed();
    }

    @Scene(maxTicks = 40)
    public void aStrongerFiniteEffectPushesTheHeldInstanceDownAndStoppingTheSourceLeavesNothingAfterItExpires(TestScene scene) {
        Cow cow = cow(scene);
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), true);
        cow.addEffect(of(MobEffects.SPEED, 10, 1));
        scene.check("the finite instance is the head and the held one sits below it", () -> {
            MobEffectInstance head = cow.getEffect(MobEffects.SPEED);
            scene.assertEquals(1, head.getAmplifier(), "head amplifier");
            scene.assertFalse(head.isInfiniteDuration(), "head infinite");
            scene.assertTrue(head.hiddenEffect != null && head.hiddenEffect.isInfiniteDuration(), "held instance hidden");
        });
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), false);
        scene.check("the held instance is gone from the chain at once, the finite one stays", () -> {
            MobEffectInstance head = cow.getEffect(MobEffects.SPEED);
            scene.assertTrue(head != null && head.getAmplifier() == 1 && head.hiddenEffect == null, "only the finite instance");
            scene.assertFalse(Effect.OWNED.isSet(cow), "attachment set");
        });
        scene.runAtTick(20, () -> {
            scene.check("after the finite one expires nothing is left", () -> scene.assertFalse(cow.hasEffect(MobEffects.SPEED), "present"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 20)
    public void anInfiniteInstanceTheSourceDidNotAddIsNotRemoved(TestScene scene) {
        Cow cow = cow(scene);
        cow.addEffect(of(MobEffects.SPEED, MobEffectInstance.INFINITE_DURATION, 0));
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), false);
        scene.check("an unrecorded source touches nothing", () -> {
            scene.assertTrue(cow.getEffect(MobEffects.SPEED).isInfiniteDuration(), "still there");
            scene.assertFalse(Effect.OWNED.isSet(cow), "attachment set");
        });
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), true);
        scene.check("an active call neither replaces it nor records the source", () -> scene.assertFalse(Effect.OWNED.isSet(cow), "attachment set"));
        effect().hold(cow, OTHER, infinite(MobEffects.GLOWING, 0), true);
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), false);
        scene.check("another source's record does not make this one the owner", () -> {
            scene.assertTrue(cow.hasEffect(MobEffects.SPEED), "speed present");
            scene.assertTrue(Effect.OWNED.get(cow).has(OTHER) && !Effect.OWNED.get(cow).has(SOURCE), "records");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aSourceWhoseInfiniteInstanceVanishedIsForgottenWhenAnotherInstanceIsThere(TestScene scene) {
        Cow cow = cow(scene);
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), true);
        cow.removeEffect(MobEffects.SPEED);
        cow.addEffect(of(MobEffects.SPEED, 200, 0));
        effect().hold(cow, SOURCE, infinite(MobEffects.SPEED, 0), true);
        scene.check("the foreign instance stays and the stale record goes", () -> {
            scene.assertEquals(200, cow.getEffect(MobEffects.SPEED).getDuration(), "duration");
            scene.assertFalse(Effect.OWNED.isSet(cow), "attachment set");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 40)
    public void aFiniteStackOnAHeldInstanceStaysFiniteExpiresAndTheHeldOneComesBackUntilTheSourceStops(TestScene scene) {
        Cow cow = cow(scene);
        effect().hold(cow, SOURCE, infinite(MobEffects.LUCK, 0), true);
        cow.addEffect(of(MobEffects.LUCK, 5, 0));
        scene.check("the stack is finite", () -> {
            scene.assertEquals(1, cow.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier");
            scene.assertFalse(cow.getEffect(MobEffects.LUCK).isInfiniteDuration(), "infinite");
        });
        scene.runAtTick(15, () -> {
            scene.check("it expired and the held instance is back", () -> {
                MobEffectInstance back = cow.getEffect(MobEffects.LUCK);
                scene.assertTrue(back != null && back.isInfiniteDuration() && back.getAmplifier() == 0, "held instance");
            });
            effect().hold(cow, SOURCE, infinite(MobEffects.LUCK, 0), false);
            scene.check("it is gone once the source stops", () -> scene.assertFalse(cow.hasEffect(MobEffects.LUCK), "present"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 20)
    public void stoppingTheSourceSplicesTheHeldInstanceOutFromUnderAFiniteHeadAndLeavesTheHeadUntouched(TestScene scene) {
        // A shortening and a protector on the entity, both of which would act on anything that passed through the rules again.
        Cow beneath = cow(scene);
        effect().hold(beneath, SOURCE, infinite(MobEffects.HERO_OF_THE_VILLAGE, 0), true);
        beneath.addEffect(of(MobEffects.HERO_OF_THE_VILLAGE, 100, 1));
        beneath.getAttribute(Attributes.MOVEMENT_EFFICIENCY).setBaseValue(0.5D);
        beneath.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE).setBaseValue(0.5D);
        effect().hold(beneath, SOURCE, infinite(MobEffects.HERO_OF_THE_VILLAGE, 0), false);
        scene.check("the finite head the held instance sat under is untouched", () -> {
            MobEffectInstance left = beneath.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
            scene.assertTrue(left != null && left.getAmplifier() == 1 && left.getDuration() == 100 && left.hiddenEffect == null, "remainder");
        });
        scene.check("and the source is forgotten", () -> scene.assertFalse(Effect.OWNED.isSet(beneath), "attachment set"));
        scene.succeed();
    }

    // ------------------------------------------------------------------ clear and cure

    @Scene(maxTicks = 20, isolated = true)
    public void aMilkBucketDrunkByASurvivalPlayerLeavesTheIncurableEffectAndTakesTheOthers(TestScene scene) {
        TestPlayer test = scene.addPlayer("EffectMilk").survival();
        ServerPlayer player = test.player();
        player.addEffect(of(MobEffects.SPEED, 200, 0));
        player.addEffect(of(MobEffects.GLOWING, 200, 0));
        player.addEffect(of(MobEffects.UNLUCK, 200, 0));
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, milk);
        milk.finishUsingItem(player.level(), player);
        scene.check("speed and glowing are cleared, unluck is not", () -> {
            scene.assertFalse(player.hasEffect(MobEffects.SPEED), "speed present");
            scene.assertFalse(player.hasEffect(MobEffects.GLOWING), "glowing present");
            scene.assertTrue(player.hasEffect(MobEffects.UNLUCK), "unluck present");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aTotemsDeathProtectionLeavesTheIncurableEffectAndTakesTheOthers(TestScene scene) {
        Cow cow = cow(scene);
        cow.addEffect(of(MobEffects.SPEED, 200, 0));
        cow.addEffect(of(MobEffects.UNLUCK, 200, 0));
        cow.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        cow.hurtServer(scene.level(), scene.level().damageSources().generic(), 1000.0F);
        scene.check("the totem saved the cow", () -> {
            scene.assertTrue(cow.isAlive(), "alive");
            scene.assertTrue(cow.getMainHandItem().isEmpty(), "totem used");
        });
        scene.check("speed is cleared, unluck is not, and the totem's own effects came after the clear", () -> {
            scene.assertFalse(cow.hasEffect(MobEffects.SPEED), "speed present");
            scene.assertTrue(cow.hasEffect(MobEffects.UNLUCK), "unluck present");
            scene.assertTrue(cow.hasEffect(MobEffects.REGENERATION), "regeneration present");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void effectClearTakesTheIncurableEffectToo(TestScene scene) {
        Cow cow = cow(scene);
        cow.addEffect(of(MobEffects.SPEED, 200, 0));
        cow.addEffect(of(MobEffects.UNLUCK, 200, 0));
        MinecraftServer server = scene.level().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "effect clear " + cow.getStringUUID());
        scene.check("nothing is left", () -> scene.assertTrue(cow.getActiveEffects().isEmpty(), "effects left"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void cureRemovesWhatItsTagListsAndNothingElse(TestScene scene) {
        Cow cow = cow(scene);
        cow.addEffect(of(MobEffects.SPEED, 200, 0));
        cow.addEffect(of(MobEffects.UNLUCK, 200, 0));
        cow.addEffect(of(MobEffects.GLOWING, 200, 0));
        scene.check("the listed effects go, an incurable one included", () -> scene.assertTrue(effect().cure(cow, CURE), "answer"));
        scene.check("the unlisted one stays", () -> {
            scene.assertFalse(cow.hasEffect(MobEffects.SPEED), "speed present");
            scene.assertFalse(cow.hasEffect(MobEffects.UNLUCK), "unluck present");
            scene.assertTrue(cow.hasEffect(MobEffects.GLOWING), "glowing present");
        });
        scene.check("a second cure finds nothing", () -> scene.assertFalse(effect().cure(cow, CURE), "answer"));
        scene.check("the incurable tag is the mod's own", () -> scene.assertEquals("jolcraft:incurable", EffectTags.INCURABLE.location().toString(), "tag"));
        scene.succeed();
    }
}
