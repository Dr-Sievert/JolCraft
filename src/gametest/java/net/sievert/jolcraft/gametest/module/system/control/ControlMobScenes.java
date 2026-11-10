package net.sievert.jolcraft.gametest.module.system.control;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.control.GoalEvent;

/**
 * The mob half of control: the vanilla goals refused through {@code GoalEvent} and the server tick's movement and
 * target rules. The mobs stand on a stone platform above the scene's arena, which is a sealed barrier box (W8), and aim
 * at a cow of enormous health without a mind of its own. The tag fixtures: strength is ATTACK, haste RANGED, night vision
 * USE, jump boost MOVE and health boost TARGET.
 */
public final class ControlMobScenes {

    private static final Holder<MobEffect> ATTACK = MobEffects.STRENGTH;
    private static final Holder<MobEffect> RANGED = MobEffects.HASTE;
    private static final Holder<MobEffect> USE = MobEffects.NIGHT_VISION;
    private static final Holder<MobEffect> MOVE = MobEffects.JUMP_BOOST;
    private static final Holder<MobEffect> TARGET = MobEffects.HEALTH_BOOST;

    private static final int FLOOR = 2;

    /**
     * The scene-relative position for a platform coordinate, the platform's centre being 8 blocks into the chunk the scene
     * starts in: only that chunk is sure to tick its entities, so the platform is exactly that chunk's width and stops there.
     */
    private static BlockPos at(TestScene scene, int x, int y, int z) {
        return new BlockPos(x + 8 - (scene.origin().getX() & 15), y, z + 8 - (scene.origin().getZ() & 15));
    }

    /** A stone floor, one chunk across, above the arena and cleared when the scene ends, and a cow of enormous health on it at the given x. */
    private static Cow platform(TestScene scene, int cowX) {
        fill(scene, Blocks.STONE);
        scene.onEnd(() -> fill(scene, Blocks.AIR));
        Cow cow = scene.helper().spawn(EntityType.COW, at(scene, cowX, FLOOR + 1, 0));
        cow.setNoAi(true);
        // not invulnerable: the game refuses an invulnerable entity as a target
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000.0D);
        cow.setHealth(100000.0F);
        return cow;
    }

    private static void fill(TestScene scene, Block block) {
        for (int x = -8; x < 8; x++) {
            for (int z = -8; z < 8; z++) {
                BlockPos pos = at(scene, x, FLOOR, z);
                scene.setBlock(pos.getX(), pos.getY(), pos.getZ(), block);
            }
        }
    }

    private static <T extends Mob> T spawn(TestScene scene, EntityType<T> type, int x, int z, Cow target) {
        T mob = scene.helper().spawn(type, at(scene, x, FLOOR + 1, z));
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        mob.setTarget(target);
        return mob;
    }

    private static boolean running(Mob mob, Predicate<Goal> kind) {
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.isRunning() && kind.test(wrapped.getGoal())) {
                return true;
            }
        }
        return false;
    }

    private static MobEffectInstance of(Holder<MobEffect> effect) {
        return new MobEffectInstance(effect, 1000, 0);
    }

    /**
     * Both mobs run the goal; the effect goes on {@code blocked} once {@code ready} says so; then {@code blocked} stops
     * running it, {@code aftermath} looks at what the game's own stop left, and {@code free} keeps running it throughout,
     * as does the {@code bystander} a scene may add (a mob that never had a reason to be blocked). {@code again} runs
     * before the ten ticks, for a scene whose game stop leaves the goal nothing to restart on (it gives the target back).
     */
    private static void blocks(TestScene scene, Mob blocked, Mob free, Predicate<Goal> kind, Holder<MobEffect> effect, Predicate<Mob> ready, BooleanSupplier bystander, Runnable aftermath, Runnable again) {
        scene.helper().startSequence()
                .thenWaitUntil(() -> {
                    scene.assertTrue(running(blocked, kind) && ready.test(blocked), "the mob to be running its goal");
                    scene.assertTrue(running(free, kind), "the control mob to be running its goal");
                    scene.assertTrue(bystander.getAsBoolean(), "the bystander to be running its goal");
                })
                .thenExecute(() -> {
                    scene.log().info("running before the effect: {}", names(blocked, kind));
                    blocked.addEffect(of(effect));
                })
                .thenWaitUntil(() -> scene.assertFalse(running(blocked, kind), "the mob under the effect to still run its goal"))
                .thenExecute(() -> {
                    scene.check("the goal stopped and the game's own stop left what it leaves", aftermath);
                    scene.check("the control mob without the effect still runs it", () -> scene.assertTrue(running(free, kind), "control mob running"));
                })
                .thenExecute(again)
                .thenIdle(10)
                .thenExecute(() -> {
                    scene.check("ten ticks on, the mob under the effect has not started it again", () -> scene.assertFalse(running(blocked, kind), "blocked mob running"));
                    scene.check("and the bystander is still not blocked", () -> scene.assertTrue(bystander.getAsBoolean(), "bystander running"));
                })
                .thenSucceed();
    }

    private static String names(Mob mob, Predicate<Goal> kind) {
        StringBuilder names = new StringBuilder();
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.isRunning() && kind.test(wrapped.getGoal())) {
                names.append(wrapped.getGoal().getClass().getSimpleName()).append(' ');
            }
        }
        return names.toString().trim();
    }

    @Scene(maxTicks = 200)
    public void aSkeletonDrawsItsBowUntilTheRangedEffectAndASkeletonWithoutItGoesOn(TestScene scene) {
        Cow cow = platform(scene, 4);
        Skeleton blocked = spawn(scene, EntityType.SKELETON, -4, -2, cow);
        Skeleton free = spawn(scene, EntityType.SKELETON, -4, 2, cow);
        blocked.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        free.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        blocks(scene, blocked, free, goal -> goal instanceof RangedBowAttackGoal<?>, RANGED, _ -> true, () -> true, () -> scene.assertFalse(blocked.isUsingItem(), "using the bow"), () -> {
        });
    }

    @Scene(maxTicks = 200)
    public void aSkeletonWithTheUseEffectKeepsDrawingItsBow(TestScene scene) {
        Cow cow = platform(scene, 4);
        Skeleton skeleton = spawn(scene, EntityType.SKELETON, -4, 0, cow);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.addEffect(of(USE));
        skeleton.addEffect(of(ATTACK));
        scene.helper().startSequence()
                .thenWaitUntil(() -> scene.assertTrue(running(skeleton, goal -> goal instanceof RangedBowAttackGoal<?>), "the bow goal to run under USE and ATTACK"))
                .thenIdle(10)
                .thenExecute(() -> scene.check("USE and ATTACK leave a bow goal alone", () -> scene.assertTrue(running(skeleton, goal -> goal instanceof RangedBowAttackGoal<?>), "bow goal running")))
                .thenSucceed();
    }

    @Scene(maxTicks = 200)
    public void aZombieWithASwordPursuesUntilTheAttackEffectAndABareHandedOneIsNotDisarmed(TestScene scene) {
        Cow cow = platform(scene, 5);
        Zombie blocked = spawn(scene, EntityType.ZOMBIE, -4, -3, cow);
        Zombie free = spawn(scene, EntityType.ZOMBIE, -4, 3, cow);
        Zombie bare = spawn(scene, EntityType.ZOMBIE, -4, 0, cow);
        blocked.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        free.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        bare.addEffect(of(ATTACK));
        Predicate<Goal> melee = goal -> goal instanceof MeleeAttackGoal;
        blocks(scene, blocked, free, melee, ATTACK, _ -> true, () -> running(bare, melee), () -> scene.assertTrue(blocked.getTarget() == cow, "the zombie keeps its target"), () -> {
        });
    }

    @Scene(maxTicks = 400)
    public void aPillagerWithACrossbowChargesUntilTheRangedEffectThenTheGameClearsTheTargetAndTheCharge(TestScene scene) {
        Cow cow = platform(scene, 3);
        Pillager blocked = spawn(scene, EntityType.PILLAGER, -3, -2, cow);
        Pillager free = spawn(scene, EntityType.PILLAGER, -3, 2, cow);
        blocked.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        free.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        // mid-charge: the effect lands only while the pillager charges, and the target is given back so only the veto keeps the goal from starting again
        blocks(scene, blocked, free, goal -> goal instanceof RangedCrossbowAttackGoal<?>, RANGED, _ -> blocked.isChargingCrossbow(), () -> true, () -> {
            scene.assertTrue(blocked.getTarget() == null, "the crossbow goal's stop clears the target");
            scene.assertFalse(blocked.isUsingItem(), "using the crossbow");
            scene.assertFalse(blocked.isChargingCrossbow(), "charging the crossbow");
        }, () -> blocked.setTarget(cow));
    }

    /**
     * One evoker, since two rarely cast together: it casts before the effect, which shows a spell goal runs unhindered, and
     * afterwards stops, with the vetoes it was refused counted from the event itself.
     */
    @Scene(maxTicks = 300)
    public void anEvokerCastsFangsOrSummonsUntilTheUseEffect(TestScene scene) {
        Cow cow = platform(scene, 3);
        Evoker evoker = spawn(scene, EntityType.EVOKER, -3, 0, cow);
        // the fangs and summon goals defer to the base canUse; the wololo goal overrides it and is outside
        Predicate<Goal> casting = goal -> goal instanceof SpellcasterIllager.SpellcasterUseSpellGoal && !(goal instanceof Evoker.EvokerWololoSpellGoal);
        AtomicInteger vetoes = new AtomicInteger();
        Consumer<GoalEvent> count = event -> {
            if (event.mob() == evoker && event.isCanceled()) {
                vetoes.incrementAndGet();
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, GoalEvent.class, count);
        scene.onEnd(() -> NeoForge.EVENT_BUS.unregister(count));
        scene.helper().startSequence()
                .thenWaitUntil(() -> scene.assertTrue(running(evoker, casting), "the evoker to cast"))
                .thenExecute(() -> {
                    scene.log().info("running before the effect: {}", names(evoker, casting));
                    scene.assertEquals(0, vetoes.get(), "vetoes before the effect");
                    evoker.addEffect(of(USE));
                })
                .thenWaitUntil(() -> scene.assertFalse(running(evoker, casting), "the evoker under the effect to still cast"))
                .thenExecute(() -> scene.check("the goal stopped on a veto of the event", () -> scene.assertTrue(vetoes.get() > 0, "vetoes " + vetoes.get())))
                .thenIdle(10)
                .thenExecute(() -> scene.check("ten ticks on, the evoker has not cast again", () -> scene.assertFalse(running(evoker, casting), "evoker casting")))
                .thenSucceed();
    }

    @Scene(maxTicks = 40)
    public void aMobUnderMoveLosesItsHorizontalMotionAndKeepsItsVerticalOne(TestScene scene) {
        platform(scene, 0);
        Cow cow = scene.helper().spawn(EntityType.COW, at(scene, 3, FLOOR + 4, 3));
        cow.setNoGravity(true);
        cow.setNoAi(true);
        scene.helper().startSequence()
                .thenWaitUntil(() -> scene.assertTrue(cow.tickCount > 2, "the cow to be ticking"))
                .thenExecute(() -> {
                    cow.setDeltaMovement(new Vec3(0.2D, 0.3D, 0.2D));
                    cow.addEffect(of(MOVE));
                })
                .thenIdle(2)
                .thenExecute(() -> scene.check("the horizontal motion is zero and the vertical one is left", () -> {
                    Vec3 motion = cow.getDeltaMovement();
                    scene.assertEquals(0.0D, motion.x, "horizontal x");
                    scene.assertEquals(0.0D, motion.z, "horizontal z");
                    scene.assertTrue(motion.y > 0.1D, "vertical motion " + motion.y);
                }))
                .thenSucceed();
    }

    @Scene(maxTicks = 120)
    public void aZombieUnderMoveStopsWalkingToItsTarget(TestScene scene) {
        Cow cow = platform(scene, 6);
        Zombie zombie = spawn(scene, EntityType.ZOMBIE, -5, 0, cow);
        scene.helper().startSequence()
                .thenWaitUntil(() -> scene.assertFalse(zombie.getNavigation().isDone(), "the zombie to be walking"))
                .thenExecute(() -> zombie.addEffect(of(MOVE)))
                .thenIdle(3)
                .thenExecute(() -> scene.check("its navigation is stopped and it has no horizontal motion", () -> {
                    scene.assertTrue(zombie.getNavigation().isDone(), "navigation done");
                    scene.assertEquals(0.0D, zombie.getDeltaMovement().x, "horizontal x");
                    scene.assertEquals(0.0D, zombie.getDeltaMovement().z, "horizontal z");
                }))
                .thenSucceed();
    }

    @Scene(maxTicks = 60)
    public void aMobUnderTargetLosesItsTargetAndItsAnger(TestScene scene) {
        Cow cow = platform(scene, 3);
        Zombie zombie = spawn(scene, EntityType.ZOMBIE, -3, -2, cow);
        ZombifiedPiglin piglin = spawn(scene, EntityType.ZOMBIFIED_PIGLIN, -3, 2, cow);
        piglin.setPersistentAngerTarget(EntityReference.of(cow));
        scene.helper().startSequence()
                .thenWaitUntil(() -> scene.assertTrue(zombie.tickCount > 2 && piglin.tickCount > 2, "the mobs to be ticking"))
                .thenExecute(() -> {
                    scene.assertTrue(zombie.getTarget() == cow && piglin.getTarget() == cow, "both mobs aim at the cow before the effect");
                    scene.assertTrue(piglin.getPersistentAngerTarget() != null, "the piglin is angry");
                    zombie.addEffect(of(TARGET));
                    piglin.addEffect(of(TARGET));
                })
                .thenIdle(2)
                .thenExecute(() -> scene.check("the target is cleared and so is the anger", () -> {
                    scene.assertTrue(zombie.getTarget() == null, "the zombie's target");
                    scene.assertTrue(piglin.getTarget() == null, "the piglin's target");
                    scene.assertTrue(piglin.getPersistentAngerTarget() == null, "the piglin's anger");
                    scene.assertFalse(zombie.isAggressive(), "the zombie aggressive");
                }))
                .thenSucceed();
    }
}
