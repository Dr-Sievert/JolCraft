package net.sievert.jolcraft.module.system.control;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Crowd control, read from data: an entity with an effect in a {@link Stop}'s tag is kept from what the stop names.
 * Players are stopped through the game's interaction and use events, mobs through each server tick and through
 * {@link GoalEvent} for the vanilla goals; a creative player is exempt everywhere. Every rule acts on the server; the
 * client half, in {@code control/client}, only keeps the player's own input from being sent. Design in design/effect.md.
 */
@ModSystem(Dictionary.CONTROL)
public final class Control implements Module {

    /** Adds the system's listeners to the game bus. */
    public void register(IEventBus gameBus) {
        gameBus.addListener(this::onAttack);
        gameBus.addListener(this::onLeftClickBlock);
        gameBus.addListener(this::onRightClickBlock);
        gameBus.addListener(this::onRightClickItem);
        gameBus.addListener(this::onEntityInteract);
        gameBus.addListener(this::onEntityInteractSpecific);
        gameBus.addListener(this::onBreak);
        gameBus.addListener(this::onUseStart);
        gameBus.addListener(this::onUseTick);
        gameBus.addListener(this::onUseStop);
        gameBus.addListener(this::onEntityTick);
        gameBus.addListener(this::onGoal);
    }

    /** Whether the entity has an effect in the stop's tag and is not a creative player. */
    public boolean stops(LivingEntity entity, Stop stop) {
        if (entity.getActiveEffectsMap().isEmpty() || entity instanceof Player player && player.isCreative()) {
            return false;
        }
        for (MobEffectInstance instance : entity.getActiveEffectsMap().values()) {
            if (instance.getEffect().is(stop.tag())) {
                return true;
            }
        }
        return false;
    }

    private boolean stopsAttack(Player player) {
        return !player.level().isClientSide() && stops(player, Stop.ATTACK);
    }

    private boolean stopsUse(LivingEntity entity, ItemStack stack) {
        return !entity.level().isClientSide() && stops(entity, Stop.of(stack));
    }

    private void onAttack(AttackEntityEvent event) {
        if (stopsAttack(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (stopsAttack(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private void onBreak(BreakBlockEvent event) {
        if (stopsAttack(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (stopsUse(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (stopsUse(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (stopsUse(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (stopsUse(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (stopsUse(event.getEntity(), event.getItem())) {
            event.setCanceled(true);
        }
    }

    /**
     * Not cancelled: a cancelled tick answers -1 remaining, which the game reads as the use being over and finishes the
     * item (a food is eaten). Stopped here and the duration zeroed, the game finds nothing in use to finish.
     */
    private void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (stopsUse(event.getEntity(), event.getItem())) {
            halt(event.getEntity());
            event.setDuration(0);
        }
    }

    /** Cancelled, the item is not told it was released: a drawn bow does not fire. */
    private void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (stopsUse(event.getEntity(), event.getItem())) {
            event.setCanceled(true);
        }
    }

    /** Not releaseUsingItem: the item must not be told it was released. A crossbow mob's charge pose goes with the use. */
    private void halt(LivingEntity entity) {
        entity.stopUsingItem();
        if (entity instanceof CrossbowAttackMob crossbow) {
            crossbow.setChargingCrossbow(false);
        }
    }

    private void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide() || entity.getActiveEffectsMap().isEmpty()) {
            return;
        }
        if (entity.isUsingItem() && stops(entity, Stop.of(entity.getUseItem()))) {
            halt(entity);
        }
        if (stops(entity, Stop.MOVE)) {
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.getMoveControl().strafe(0.0F, 0.0F);
            }
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0D, motion.y, 0.0D);
        }
        if (entity instanceof Mob mob && stops(mob, Stop.TARGET)) {
            if (mob instanceof NeutralMob neutral) {
                neutral.stopBeingAngry();
            }
            mob.setTarget(null);
            mob.setAggressive(false);
        }
    }

    /** The melee goal is refused only while the mob holds a melee or ranged tool: a bare-handed mob is not disarmed. */
    private void onGoal(GoalEvent event) {
        Mob mob = event.mob();
        boolean blocked = stops(mob, event.stop());
        if (blocked && event.stop() == Stop.ATTACK) {
            blocked = Stop.weapon(mob.getMainHandItem());
        }
        if (blocked) {
            event.setCanceled(true);
        }
    }
}
