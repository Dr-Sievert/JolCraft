package net.sievert.jolcraft.gametest.module.system.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.control.Control;
import net.sievert.jolcraft.module.system.control.Stop;

/**
 * The player half of control: the game's attack, interaction and use events against the six stop tags. The tag
 * fixtures under scene/data/jolcraft/tags/mob_effect name vanilla effects that no other scene uses: strength is ATTACK, haste
 * RANGED, night vision USE, jump boost MOVE, water breathing LOOK and health boost TARGET. The events are posted to the
 * game bus as the game posts them, so the real mod's listeners answer.
 */
public final class ControlPlayerScenes {

    private static final Holder<MobEffect> ATTACK = MobEffects.STRENGTH;
    private static final Holder<MobEffect> RANGED = MobEffects.HASTE;
    private static final Holder<MobEffect> USE = MobEffects.NIGHT_VISION;
    private static final BlockPos POS = new BlockPos(5, 5, 5);

    private static boolean cancelled(Event event) {
        return NeoForge.EVENT_BUS.post(event) instanceof ICancellableEvent cancellable && cancellable.isCanceled();
    }

    private static int one(Event event) {
        return cancelled(event) ? 1 : 0;
    }

    /** How many of the three attack events, left click on an entity, left click on a block and a block break, were cancelled. */
    private static int attacks(ServerPlayer player, Cow target) {
        return one(new AttackEntityEvent(player, target))
                + one(new PlayerInteractEvent.LeftClickBlock(player, POS, Direction.UP, PlayerInteractEvent.LeftClickBlock.Action.START))
                + one(new BreakBlockEvent(player.level(), POS, Blocks.STONE.defaultBlockState(), player));
    }

    /** One when a use tick ends the use (the duration zeroed, the event not cancelled), else none. */
    private static int ended(ServerPlayer player, ItemStack held) {
        LivingEntityUseItemEvent.Tick tick = new LivingEntityUseItemEvent.Tick(player, held, 10);
        NeoForge.EVENT_BUS.post(tick);
        return tick.getDuration() == 0 && !tick.isCanceled() ? 1 : 0;
    }

    /** How many of the seven use events, four interactions and the start, tick and stop of using an item, were cancelled with the stack in the main hand. */
    private static int uses(ServerPlayer player, Cow target, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return one(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND))
                + one(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, POS, new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false)))
                + one(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, target))
                + one(new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, target, Vec3.ZERO))
                + one(new LivingEntityUseItemEvent.Start(player, held, InteractionHand.MAIN_HAND, 20))
                + ended(player, held)
                + one(new LivingEntityUseItemEvent.Stop(player, held, 10));
    }

    private static MobEffectInstance of(Holder<MobEffect> effect) {
        return new MobEffectInstance(effect, 1000, 0);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void playerAttackIsStoppedByAttackAndNotByUse(TestScene scene) {
        ServerPlayer player = scene.addPlayer("ControlAttack").survival().player();
        Cow free = scene.helper().spawn(EntityType.COW, new BlockPos(0, 1, 0));
        Cow stopped = scene.helper().spawn(EntityType.COW, new BlockPos(0, 1, 0));
        player.addEffect(of(USE));
        scene.check("with an effect of USE alone no attack event is cancelled and the attack hurts", () -> {
            scene.assertEquals(0, attacks(player, free), "cancelled attack events");
            player.attack(free);
            scene.assertTrue(free.getHealth() < free.getMaxHealth(), "the attacked cow was hurt");
        });
        player.removeEffect(USE);
        player.addEffect(of(ATTACK));
        scene.check("with an effect of ATTACK all three attack events are cancelled and the attack does no harm", () -> {
            scene.assertEquals(3, attacks(player, stopped), "cancelled attack events");
            player.attack(stopped);
            scene.assertEquals(stopped.getMaxHealth(), stopped.getHealth(), "health of the attacked cow");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void useEventsFollowTheStopOfTheHeldItem(TestScene scene) {
        ServerPlayer player = scene.addPlayer("ControlUse").survival().player();
        Cow target = scene.helper().spawn(EntityType.COW, new BlockPos(0, 1, 0));
        player.addEffect(of(RANGED));
        scene.check("RANGED stops a bow, a crossbow and a trident and lets a stick and an empty hand through", () -> {
            scene.assertEquals(7, uses(player, target, new ItemStack(Items.BOW)), "events cancelled with a bow");
            scene.assertEquals(7, uses(player, target, new ItemStack(Items.CROSSBOW)), "events cancelled with a crossbow");
            scene.assertEquals(7, uses(player, target, new ItemStack(Items.TRIDENT)), "events cancelled with a trident");
            scene.assertEquals(0, uses(player, target, new ItemStack(Items.STICK)), "events cancelled with a stick");
            scene.assertEquals(0, uses(player, target, ItemStack.EMPTY), "events cancelled with an empty hand");
        });
        player.removeEffect(RANGED);
        player.addEffect(of(USE));
        scene.check("USE stops a stick and an empty hand and lets a bow through", () -> {
            scene.assertEquals(7, uses(player, target, new ItemStack(Items.STICK)), "events cancelled with a stick");
            scene.assertEquals(7, uses(player, target, ItemStack.EMPTY), "events cancelled with an empty hand");
            scene.assertEquals(0, uses(player, target, new ItemStack(Items.BOW)), "events cancelled with a bow");
        });
        player.removeEffect(USE);
        player.addEffect(of(ATTACK));
        scene.check("ATTACK stops no use", () -> {
            scene.assertEquals(0, uses(player, target, new ItemStack(Items.STICK)), "events cancelled with a stick");
            scene.assertEquals(0, uses(player, target, new ItemStack(Items.BOW)), "events cancelled with a bow");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCreativePlayerIsStoppedByNothing(TestScene scene) {
        TestPlayer test = scene.addPlayer("ControlCreative");
        ServerPlayer player = test.player();
        Cow target = scene.helper().spawn(EntityType.COW, new BlockPos(0, 1, 0));
        Control control = new Control();
        player.addEffect(of(ATTACK));
        player.addEffect(of(RANGED));
        player.addEffect(of(USE));
        player.addEffect(of(MobEffects.JUMP_BOOST));
        player.addEffect(of(MobEffects.WATER_BREATHING));
        player.addEffect(of(MobEffects.HEALTH_BOOST));
        scene.check("a creative player has all six effects and none stops anything", () -> {
            scene.assertEquals(GameType.CREATIVE, player.gameMode.getGameModeForPlayer(), "game mode");
            scene.assertEquals(6, player.getActiveEffects().size(), "effects");
            for (Stop stop : Stop.values()) {
                scene.assertFalse(control.stops(player, stop), "stops " + stop);
            }
            scene.assertEquals(0, attacks(player, target), "cancelled attack events");
            scene.assertEquals(0, uses(player, target, new ItemStack(Items.BOW)), "events cancelled with a bow");
            scene.assertEquals(0, uses(player, target, ItemStack.EMPTY), "events cancelled with an empty hand");
        });
        test.survival();
        scene.check("the same player in survival is stopped by every one", () -> {
            for (Stop stop : Stop.values()) {
                scene.assertTrue(control.stops(player, stop), "stops " + stop);
            }
        });
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void anItemInUseThatTheTagsNowForbidIsStopped(TestScene scene) {
        ServerPlayer player = scene.addPlayer("ControlHolding").survival().player();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        scene.assertTrue(player.isUsingItem(), "the apple is being eaten");
        player.addEffect(of(RANGED));
        scene.helper().startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    scene.check("RANGED leaves the apple being eaten", () -> scene.assertTrue(player.isUsingItem(), "using the apple"));
                    player.addEffect(of(USE));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    scene.check("USE stops the apple and it is not eaten", () -> {
                        scene.assertFalse(player.isUsingItem(), "using the apple");
                        scene.assertEquals(1, player.getMainHandItem().getCount(), "apples in hand");
                    });
                    player.removeEffect(USE);
                    player.removeEffect(RANGED);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
                    player.startUsingItem(InteractionHand.MAIN_HAND);
                    scene.assertTrue(player.isUsingItem(), "the bow is drawn");
                    player.addEffect(of(RANGED));
                })
                .thenIdle(3)
                .thenExecute(() -> scene.check("RANGED, applied after the draw, stops the drawn bow", () -> scene.assertFalse(player.isUsingItem(), "using the bow")))
                .thenSucceed();
    }
}
