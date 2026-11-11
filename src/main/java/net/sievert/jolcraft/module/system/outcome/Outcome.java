package net.sievert.jolcraft.module.system.outcome;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;

import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.composite.capability.Inventory;
import net.sievert.jolcraft.module.composite.loot.Delivery;
import net.sievert.jolcraft.module.composite.loot.Yielded;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * What results roll, delivered. The mod's one {@code YieldKinds} is {@link OutcomeKeys#YIELDS}, which the root
 * declares to {@code Result} and every host's records embed, and {@link #deliver} turns a {@link Delivery} of the
 * composite's four kinds into the world for a player, so no host does it again. A host keeps what is its own: when
 * to deliver, what a refusal means, its sounds of success and failure, and any kind of its own, read through
 * {@link Delivery#of}. Built from {@code inventory} and {@code log}; design in design/outcome.md.
 */
@ModSystem(Dictionary.OUTCOME)
public final class Outcome implements Module {

    private final Inventory inventory;
    private final Log log;

    public Outcome(Inventory inventory, Log log) {
        this.inventory = inventory;
        this.log = log.tagged(this);
    }

    /**
     * Delivers the composite's four kinds to the player, on the server thread. The stacks are given together through
     * the player's inventory in the game's place-back order, so they stack with each other and the overflow drops
     * once, at the player, as {@code /give} drops it. Each entity of each count is made by the game's spawning create
     * at the player's block, finalised as a mob is, with {@link EntitySpawnReason#EVENT}, moved to the player's own
     * position keeping the yaw it rolled, and added with its riders; one the game refuses is skipped. Each effect is
     * added as a fresh copy, so the effect system's rules apply and the delivery is not edited. Each sound plays at
     * the player for everyone in range. A value of another kind is the caller's and is left alone; a refused or empty
     * delivery does nothing.
     */
    public void deliver(Delivery delivery, ServerPlayer player) {
        ServerLevel level = player.level();
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("an outcome is delivered on the server thread, not on " + Thread.currentThread().getName());
        }
        if (delivery.refused() || delivery.isEmpty()) {
            return;
        }
        List<ItemStack> stacks = delivery.stacks();
        List<ItemStack> dropped = stacks.isEmpty() ? List.of() : inventory.give(new Inventory.Holder.Player(player), stacks);
        int spawned = 0;
        for (Yielded.Entities entities : delivery.entities()) {
            for (int made = 0; made < entities.count(); made++) {
                if (spawn(entities, player)) {
                    spawned++;
                }
            }
        }
        int added = 0;
        for (MobEffectInstance effect : delivery.effects()) {
            if (player.addEffect(new MobEffectInstance(effect))) {
                added++;
            }
        }
        for (Yielded.Sound sound : delivery.sounds()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound.sound(), sound.source(), sound.volume(), sound.pitch());
        }
        log.debug("delivered to {}: {} stacks given, {} dropped, {} entities spawned, {} effects added, {} sounds played",
                player.getName().getString(), stacks.size(), dropped.size(), spawned, added, delivery.sounds().size());
    }

    /** One entity of the type at the player, with its riders; false when the game refused to make or add it. */
    private static boolean spawn(Yielded.Entities entities, ServerPlayer player) {
        ServerLevel level = player.level();
        Entity entity = entities.type().create(level, null, player.blockPosition(), EntitySpawnReason.EVENT, false, false);
        if (entity == null) {
            return false;
        }
        entity.snapTo(player.position(), entity.getYRot(), 0.0F);
        if (!level.addFreshEntity(entity)) {
            return false;
        }
        entity.getIndirectPassengers().forEach(level::addFreshEntity);
        return true;
    }
}
