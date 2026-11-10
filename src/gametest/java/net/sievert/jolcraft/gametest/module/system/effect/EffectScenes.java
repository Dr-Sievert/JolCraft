package net.sievert.jolcraft.gametest.module.system.effect;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * The effect rules on real entities, against the fixture data maps in {@code scene/data/jolcraft/data_maps} (the scene pack): luck stacks to
 * amplifier 2, protects (and cleanses) against unluck, and is shortened by water movement efficiency; slow falling
 * protects against glowing; explosion knockback resistance protects against conduit power; oxygen bonus lengthens
 * dolphin's grace and movement efficiency shortens hero of the village. Every attribute involved defaults to 0 and
 * every effect is met in a dev run only by command, and the pack is off in a dev run unless enabled by hand.
 */
public final class EffectScenes {

    private static Cow cow(TestScene scene) {
        return scene.helper().spawn(EntityType.COW, new BlockPos(0, 1, 0));
    }

    private static MobEffectInstance of(Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier);
    }

    private static void base(LivingEntity entity, Holder<Attribute> attribute, double value) {
        entity.getAttribute(attribute).setBaseValue(value);
    }

    // ------------------------------------------------------------------ stacking

    @Scene(maxTicks = 20)
    public void stackingGoesToItsCapKeepsTheLongerDurationAndStopsThere(TestScene scene) {
        Cow cow = cow(scene);
        cow.addEffect(of(MobEffects.LUCK, 200, 0));
        scene.check("the first application is untouched", () -> scene.assertEquals(0, cow.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier"));
        cow.addEffect(of(MobEffects.LUCK, 100, 0));
        scene.check("a second adds one and keeps the longer duration", () -> {
            scene.assertEquals(1, cow.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier");
            scene.assertEquals(200, cow.getEffect(MobEffects.LUCK).getDuration(), "duration");
        });
        cow.addEffect(of(MobEffects.LUCK, 300, 0));
        scene.check("a third reaches the cap of 2 with the longer duration", () -> {
            scene.assertEquals(2, cow.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier");
            scene.assertEquals(300, cow.getEffect(MobEffects.LUCK).getDuration(), "duration");
        });
        cow.addEffect(of(MobEffects.LUCK, 50, 0));
        cow.addEffect(of(MobEffects.LUCK, 500, 1));
        scene.check("it stays at 2, a stronger incoming one included, and the longest duration wins", () -> {
            scene.assertEquals(2, cow.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier");
            scene.assertEquals(500, cow.getEffect(MobEffects.LUCK).getDuration(), "duration");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void anInfiniteApplicationKeepsTheInstanceInfinite(TestScene scene) {
        Cow onFinite = cow(scene);
        onFinite.addEffect(of(MobEffects.LUCK, 100, 0));
        onFinite.addEffect(of(MobEffects.LUCK, MobEffectInstance.INFINITE_DURATION, 0));
        scene.check("infinite on finite", () -> {
            scene.assertEquals(1, onFinite.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier");
            scene.assertTrue(onFinite.getEffect(MobEffects.LUCK).isInfiniteDuration(), "infinite");
        });
        Cow onInfinite = cow(scene);
        onInfinite.addEffect(of(MobEffects.LUCK, MobEffectInstance.INFINITE_DURATION, 0));
        onInfinite.addEffect(of(MobEffects.LUCK, MobEffectInstance.INFINITE_DURATION, 0));
        scene.check("infinite on infinite", () -> {
            scene.assertEquals(1, onInfinite.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier");
            scene.assertTrue(onInfinite.getEffect(MobEffects.LUCK).isInfiniteDuration(), "infinite");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 40)
    public void aFiniteStackOnAnInfiniteInstanceStaysFiniteAndTheInfiniteOneComesBack(TestScene scene) {
        Cow cow = cow(scene);
        cow.addEffect(of(MobEffects.LUCK, MobEffectInstance.INFINITE_DURATION, 0));
        cow.addEffect(of(MobEffects.LUCK, 5, 0));
        scene.check("the stack is finite and the infinite instance sits in its chain", () -> {
            MobEffectInstance active = cow.getEffect(MobEffects.LUCK);
            scene.assertEquals(1, active.getAmplifier(), "amplifier");
            scene.assertEquals(5, active.getDuration(), "duration");
            scene.assertTrue(active.hiddenEffect != null && active.hiddenEffect.isInfiniteDuration() && active.hiddenEffect.getAmplifier() == 0, "the held instance is hidden");
        });
        scene.runAtTick(15, () -> {
            scene.check("it expires and the infinite instance returns", () -> {
                MobEffectInstance active = cow.getEffect(MobEffects.LUCK);
                scene.assertTrue(active != null && active.isInfiniteDuration() && active.getAmplifier() == 0, "the held instance is back");
            });
            scene.succeed();
        });
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theMergedAmplifierRefreshesTheAttribute(TestScene scene) {
        TestPlayer test = scene.addPlayer("EffectLuck");
        ServerPlayer player = test.player();
        player.addEffect(of(MobEffects.LUCK, 200, 0));
        scene.check("one luck is +1", () -> scene.assertEquals(1.0D, player.getAttributeValue(Attributes.LUCK), "luck"));
        player.addEffect(of(MobEffects.LUCK, 100, 0));
        scene.check("the stack is amplifier 1 and the attribute follows to +2", () -> {
            scene.assertEquals(1, player.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier");
            scene.assertEquals(2.0D, player.getAttributeValue(Attributes.LUCK), "luck");
        });
        scene.succeed();
    }

    // ------------------------------------------------------------------ protection

    @Scene(maxTicks = 20)
    public void anAttributeProtectorBlocksAtOneAndNotAtZero(TestScene scene) {
        Cow cow = cow(scene);
        base(cow, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0D);
        scene.check("at 1 the listed effect is refused", () -> {
            scene.assertFalse(cow.addEffect(of(MobEffects.CONDUIT_POWER, 200, 0)), "addEffect answer");
            scene.assertFalse(cow.hasEffect(MobEffects.CONDUIT_POWER), "present");
        });
        scene.check("an effect it does not list gets through", () -> {
            cow.addEffect(of(MobEffects.SPEED, 200, 0));
            scene.assertTrue(cow.hasEffect(MobEffects.SPEED), "present");
        });
        base(cow, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.0D);
        scene.check("at 0 it is applied", () -> {
            cow.addEffect(of(MobEffects.CONDUIT_POWER, 200, 0));
            scene.assertTrue(cow.hasEffect(MobEffects.CONDUIT_POWER), "present");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void anEffectProtectorBlocksWhatItListsAndLetsTheRestThrough(TestScene scene) {
        Cow cow = cow(scene);
        cow.addEffect(of(MobEffects.LUCK, 200, 0));
        scene.check("luck refuses unluck", () -> {
            cow.addEffect(of(MobEffects.UNLUCK, 200, 0));
            scene.assertFalse(cow.hasEffect(MobEffects.UNLUCK), "unluck present");
        });
        scene.check("and lets an unlisted effect through", () -> {
            cow.addEffect(of(MobEffects.INVISIBILITY, 200, 0));
            scene.assertTrue(cow.hasEffect(MobEffects.INVISIBILITY), "invisibility present");
        });
        cow.addEffect(of(MobEffects.SLOW_FALLING, 200, 0));
        scene.check("slow falling refuses glowing", () -> {
            cow.addEffect(of(MobEffects.GLOWING, 200, 0));
            scene.assertFalse(cow.hasEffect(MobEffects.GLOWING), "glowing present");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aCleansingProtectorRemovesWhatItBlocksAndAPlainOneDoesNot(TestScene scene) {
        Cow cow = cow(scene);
        cow.addEffect(of(MobEffects.UNLUCK, 200, 0));
        cow.addEffect(of(MobEffects.LUCK, 200, 0));
        scene.check("luck added over unluck removes it", () -> {
            scene.assertTrue(cow.hasEffect(MobEffects.LUCK), "luck present");
            scene.assertFalse(cow.hasEffect(MobEffects.UNLUCK), "unluck present");
        });
        cow.addEffect(of(MobEffects.GLOWING, 200, 0));
        cow.addEffect(of(MobEffects.SLOW_FALLING, 200, 0));
        scene.check("slow falling, which does not cleanse, leaves glowing", () -> {
            scene.assertTrue(cow.hasEffect(MobEffects.SLOW_FALLING), "slow falling present");
            scene.assertTrue(cow.hasEffect(MobEffects.GLOWING), "glowing present");
        });
        scene.succeed();
    }

    // ------------------------------------------------------------------ scaling and immunity

    @Scene(maxTicks = 20)
    public void anAttributeLengthensAndAnotherShortensTheDurationOfWhatTheyListAndNothingElse(TestScene scene) {
        Cow cow = cow(scene);
        base(cow, Attributes.OXYGEN_BONUS, 0.5D);
        base(cow, Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5D);
        cow.addEffect(of(MobEffects.DOLPHINS_GRACE, 200, 0));
        scene.check("a lengthening of 0.5 gives one and a half times", () -> scene.assertEquals(300, cow.getEffect(MobEffects.DOLPHINS_GRACE).getDuration(), "duration"));
        cow.addEffect(of(MobEffects.LUCK, 200, 0));
        scene.check("a shortening of 0.5 gives half", () -> scene.assertEquals(100, cow.getEffect(MobEffects.LUCK).getDuration(), "duration"));
        cow.addEffect(of(MobEffects.SPEED, 200, 0));
        scene.check("an unlisted effect is untouched", () -> scene.assertEquals(200, cow.getEffect(MobEffects.SPEED).getDuration(), "duration"));
        cow.removeEffect(MobEffects.DOLPHINS_GRACE);
        cow.addEffect(of(MobEffects.DOLPHINS_GRACE, MobEffectInstance.INFINITE_DURATION, 0));
        scene.check("an infinite instance is not scaled", () -> scene.assertTrue(cow.getEffect(MobEffects.DOLPHINS_GRACE).isInfiniteDuration(), "infinite"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aShorteningOfOneRefusesTheEffectAndLessOnlyShortensIt(TestScene scene) {
        Cow cow = cow(scene);
        base(cow, Attributes.MOVEMENT_EFFICIENCY, 1.0D);
        scene.check("at 1 the effect is not applied", () -> {
            scene.assertFalse(cow.addEffect(of(MobEffects.HERO_OF_THE_VILLAGE, 200, 0)), "addEffect answer");
            scene.assertFalse(cow.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "present");
        });
        base(cow, Attributes.MOVEMENT_EFFICIENCY, 0.5D);
        scene.check("at 0.5 it is applied for half the time", () -> {
            cow.addEffect(of(MobEffects.HERO_OF_THE_VILLAGE, 200, 0));
            scene.assertEquals(100, cow.getEffect(MobEffects.HERO_OF_THE_VILLAGE).getDuration(), "duration");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void scalingHappensBeforeStacking(TestScene scene) {
        Cow cow = cow(scene);
        base(cow, Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5D);
        cow.addEffect(of(MobEffects.LUCK, 200, 0));
        cow.addEffect(of(MobEffects.LUCK, 120, 0));
        scene.check("the second is halved to 60 first, then merged with the 100 held: amplifier 1 for 100", () -> {
            scene.assertEquals(1, cow.getEffect(MobEffects.LUCK).getAmplifier(), "amplifier");
            scene.assertEquals(100, cow.getEffect(MobEffects.LUCK).getDuration(), "duration");
        });
        scene.succeed();
    }

    // ------------------------------------------------------------------ the End-portal return

    @Scene(maxTicks = 20, isolated = true)
    public void anEndPortalReturnReAddsEffectsWithoutRollingProtectionsOrRescaling(TestScene scene) {
        TestPlayer test = scene.addPlayer("EffectRestore");
        ServerPlayer old = test.player();
        old.addEffect(of(MobEffects.CONDUIT_POWER, 200, 0));
        old.addEffect(of(MobEffects.DOLPHINS_GRACE, 200, 0));
        base(old, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0D);
        base(old, Attributes.OXYGEN_BONUS, 0.5D);
        // The game's respawn builds the new player, gives it the old connection, and restores before the level has it.
        ServerPlayer restored = new ServerPlayer(old.level().getServer(), old.level(), new GameProfile(UUID.randomUUID(), "Restored"), ClientInformation.createDefault()) {
        };
        restored.connection = old.connection;
        scene.check("the new player is not yet in the level", () -> scene.assertFalse(restored.isAddedToLevel(), "added"));
        restored.restoreFrom(old, true);
        scene.check("the attributes came across, so a protection would have refused the re-added effect", () ->
                scene.assertEquals(1.0D, restored.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), "protection"));
        scene.check("the protected effect was re-added", () -> scene.assertTrue(restored.hasEffect(MobEffects.CONDUIT_POWER), "conduit power present"));
        scene.check("the lengthened effect was not rescaled", () -> scene.assertEquals(200, restored.getEffect(MobEffects.DOLPHINS_GRACE).getDuration(), "duration"));
        old.removeEffect(MobEffects.CONDUIT_POWER);
        scene.check("control: the same protection refuses a fresh application on a player in the level", () ->
                scene.assertFalse(old.addEffect(of(MobEffects.CONDUIT_POWER, 200, 0)), "addEffect answer"));
        scene.succeed();
    }
}
