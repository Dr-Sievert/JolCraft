package net.sievert.jolcraft.world.entity.effect;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sievert.jolcraft.mixin.MobEffectInstanceAccessor;

import javax.annotation.Nullable;

public final class JolCraftOwnedEffectHelper {

    private JolCraftOwnedEffectHelper() {}

    public static void syncInfinite(
            LivingEntity entity,
            Holder<MobEffect> effect,
            int amplifier,
            String ownershipRoot,
            String ownershipId,
            boolean active,
            boolean ambient,
            boolean visible,
            boolean showIcon
    ) {
        MobEffectInstance current = entity.getEffect(effect);

        /*
         * The common case for every mob: nothing to apply, and nothing applied
         * that this helper would remove (no instance, or no infinite instance
         * with our amplifier anywhere in the hidden chain). Return before
         * isOwned, whose getPersistentData() would create (and then save) an
         * empty NeoForgeData compound on every living entity. Trade-off: a
         * flag left behind when the effect and its trigger vanish in the same
         * tick (milk, totem) survives until an infinite instance with this
         * amplifier is present again; that instance is then removed once, even
         * if another source added it (the armor set re-adds its own next tick;
         * vanilla has no infinite Glowing source).
         */
        if (!active
                && (current == null || !containsOwnedEffect(current, amplifier))) {
            return;
        }

        boolean owned = isOwned(
                entity,
                ownershipRoot,
                ownershipId
        );

        if (active) {
            if (current == null) {
                boolean applied = entity.addEffect(
                        new MobEffectInstance(
                                effect,
                                MobEffectInstance.INFINITE_DURATION,
                                amplifier,
                                ambient,
                                visible,
                                showIcon
                        )
                );

                if (applied) {
                    setOwned(
                            entity,
                            ownershipRoot,
                            ownershipId
                    );
                }

                return;
            }

            if (owned && !containsOwnedEffect(current, amplifier)) {
                clearOwned(
                        entity,
                        ownershipRoot,
                        ownershipId
                );
            }

            return;
        }

        if (owned && current != null) {
            MobEffectInstance remaining = removeOwnedEffect(current, amplifier);
            if (remaining != current) {
                entity.removeEffect(effect);
                if (remaining != null) {
                    entity.addEffect(remaining);
                }
            }
        }

        clearOwned(
                entity,
                ownershipRoot,
                ownershipId
        );
    }

    private static boolean containsOwnedEffect(MobEffectInstance current, int amplifier) {
        for (MobEffectInstance candidate = current; candidate != null;
             candidate = ((MobEffectInstanceAccessor) candidate).jolcraft$getHiddenEffect()) {
            if (matches(candidate, amplifier)) return true;
        }
        return false;
    }

    /** Unlink an owned hidden bonus without replacing the active potion effect. */
    private static @Nullable MobEffectInstance removeOwnedEffect(MobEffectInstance current, int amplifier) {
        MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) current;
        MobEffectInstance hidden = accessor.jolcraft$getHiddenEffect();
        if (hidden != null) {
            hidden = removeOwnedEffect(hidden, amplifier);
            accessor.jolcraft$setHiddenEffect(hidden);
        }
        return matches(current, amplifier) ? hidden : current;
    }

    private static boolean matches(
            MobEffectInstance effect,
            int amplifier
    ) {
        return effect.isInfiniteDuration()
                && effect.getAmplifier() == amplifier;
    }

    /**
     * Where the ownership flags live. For players that is the PlayerPersisted
     * compound, the only part of persistent data ServerPlayer.restoreFrom copies
     * to the clone (End-credits return, death), so owned infinite effects stay
     * owned across it. Other living entities keep the flags at the root.
     */
    private static CompoundTag ownershipData(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();

        if (!(entity instanceof Player)) {
            return data;
        }

        if (!data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }

        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static boolean isOwned(
            LivingEntity entity,
            String root,
            String id
    ) {
        CompoundTag data = ownershipData(entity);

        if (!data.contains(root, Tag.TAG_COMPOUND)) {
            return false;
        }

        return data.getCompound(root).getBoolean(id);
    }

    private static void setOwned(
            LivingEntity entity,
            String root,
            String id
    ) {
        CompoundTag data = ownershipData(entity);

        if (!data.contains(root, Tag.TAG_COMPOUND)) {
            data.put(root, new CompoundTag());
        }

        data.getCompound(root).putBoolean(id, true);
    }

    private static void clearOwned(
            LivingEntity entity,
            String root,
            String id
    ) {
        CompoundTag data = ownershipData(entity);

        if (!data.contains(root, Tag.TAG_COMPOUND)) {
            data.remove(root);
            return;
        }

        CompoundTag ownedEffects = data.getCompound(root);
        ownedEffects.remove(id);

        if (ownedEffects.isEmpty()) {
            data.remove(root);
        }
    }
}
