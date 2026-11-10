package net.sievert.jolcraft.module.system.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.attachment.Attachment;
import net.sievert.jolcraft.module.primitive.attachment.AttachmentKey;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.system.effect.EffectData.Protection;
import net.sievert.jolcraft.module.system.effect.EffectData.Scaling;
import net.sievert.jolcraft.module.system.effect.EffectData.Stacking;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The effect rules no feature owns, written once and read from data ({@link EffectData}): stacking with a cap,
 * protection by effect or attribute with cleansing, duration scaling and immunity. Every rule acts on the server only,
 * and none acts on a player the game is only restoring (an End-portal return re-adds every effect through
 * {@code addEffect} before the player is in the level, which would roll protections and scale durations a second time).
 * {@link MobEffectEvent.Applicable} only ever answers "do not apply", so another mod's refusal stands; on
 * {@link MobEffectEvent.Added} the order is cleanse, scale, stack. The incoming instance is edited in place, as the
 * game requires a fresh one from every caller, and the game's own merge then raises the instance already there.
 * Design in design/effect.md.
 */
@ModSystem(Dictionary.EFFECT)
public final class Effect implements Module {

    /** The sources holding an infinite effect on an entity; saved with the entity and not kept on death, as the effects are not. Public so a scene can ask whether anything was recorded. */
    public static final AttachmentKey<Owned> OWNED = AttachmentKey.of(Ids.of(Dictionary.JOLCRAFT, Dictionary.EFFECT, Dictionary.OWNED), () -> Owned.NONE).saved(Owned.CODEC);

    private final Log log;

    public Effect(Attachment attachment, Log log) {
        this.log = log.tagged(this);
        attachment.declare(OWNED);
    }

    /** One infinite effect a source holds: the effect, its amplifier and how the game shows it. */
    public record Held(Holder<MobEffect> effect, int amplifier, boolean ambient, boolean visible, boolean icon) {
    }

    /** Adds the system's listeners to the game bus. */
    public void register(IEventBus gameBus) {
        gameBus.addListener(this::onApplicable);
        gameBus.addListener(this::onAdded);
        gameBus.addListener(this::onClear);
    }

    private static boolean skipped(LivingEntity entity) {
        return entity.level().isClientSide() || entity instanceof ServerPlayer player && !player.isAddedToLevel();
    }

    /** The value of the attribute on the entity, 0 when the entity has none; every rule treats 0 as neutral. */
    private static double value(LivingEntity entity, ResourceKey<Attribute> key) {
        Optional<Holder.Reference<Attribute>> attribute = BuiltInRegistries.ATTRIBUTE.get(key);
        return attribute.isPresent() && entity.getAttributes().hasAttribute(attribute.get()) ? entity.getAttributeValue(attribute.get()) : 0.0D;
    }

    private void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (skipped(entity) || event.getResult() == MobEffectEvent.Applicable.Result.DO_NOT_APPLY) {
            return;
        }
        Holder<MobEffect> incoming = event.getEffectInstance().getEffect();
        if (immune(entity, incoming) || protectedFrom(entity, incoming)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** A shortening attribute at 1 or more keeps every effect its entry matches off the entity. */
    private boolean immune(LivingEntity entity, Holder<MobEffect> incoming) {
        for (Map.Entry<ResourceKey<Attribute>, Scaling> entry : BuiltInRegistries.ATTRIBUTE.getDataMap(EffectData.SCALING).entrySet()) {
            Scaling scaling = entry.getValue();
            if (scaling.matches(incoming) && scaling.immune(value(entity, entry.getKey()))) {
                log.debug("{} is immune to {} through {}", entity.getName().getString(), incoming.getRegisteredName(), entry.getKey().identifier());
                return true;
            }
        }
        return false;
    }

    /**
     * The certain protectors first, an effect the entity has and an attribute at 1 or more, so no draw is spent when one
     * holds; then each attribute between 0 and 1 rolls its value as a chance, and the first success refuses.
     */
    private boolean protectedFrom(LivingEntity entity, Holder<MobEffect> incoming) {
        for (Map.Entry<ResourceKey<MobEffect>, Protection> entry : BuiltInRegistries.MOB_EFFECT.getDataMap(EffectData.EFFECT_PROTECTION).entrySet()) {
            if (entry.getValue().blocks().contains(incoming) && has(entity, entry.getKey())) {
                log.debug("{} is protected from {} by the effect {}", entity.getName().getString(), incoming.getRegisteredName(), entry.getKey().identifier());
                return true;
            }
        }
        Map<ResourceKey<Attribute>, Protection> attributes = BuiltInRegistries.ATTRIBUTE.getDataMap(EffectData.ATTRIBUTE_PROTECTION);
        for (Map.Entry<ResourceKey<Attribute>, Protection> entry : attributes.entrySet()) {
            if (entry.getValue().blocks().contains(incoming) && certain(entity, entry.getKey())) {
                log.debug("{} is protected from {} by the attribute {}", entity.getName().getString(), incoming.getRegisteredName(), entry.getKey().identifier());
                return true;
            }
        }
        for (Map.Entry<ResourceKey<Attribute>, Protection> entry : attributes.entrySet()) {
            if (entry.getValue().blocks().contains(incoming) && !certain(entity, entry.getKey()) && Protection.roll(strength(entity, entry.getKey()), entity.getRandom())) {
                log.debug("{} resisted {} through {}", entity.getName().getString(), incoming.getRegisteredName(), entry.getKey().identifier());
                return true;
            }
        }
        return false;
    }

    private static boolean has(LivingEntity entity, ResourceKey<MobEffect> key) {
        Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.get(key);
        return effect.isPresent() && entity.hasEffect(effect.get());
    }

    private static double strength(LivingEntity entity, ResourceKey<Attribute> key) {
        return Math.clamp(value(entity, key), 0.0D, 1.0D);
    }

    private static boolean certain(LivingEntity entity, ResourceKey<Attribute> key) {
        return strength(entity, key) >= 1.0D;
    }

    private void onAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (skipped(entity)) {
            return;
        }
        MobEffectInstance added = event.getEffectInstance();
        Optional<ResourceKey<MobEffect>> key = added.getEffect().unwrapKey();
        if (key.isEmpty()) {
            return;
        }
        cleanse(entity, added, key.get());
        scale(entity, added);
        stack(entity, added, event.getOldEffectInstance(), key.get());
    }

    /** An effect protector that cleanses removes every effect it blocks, bar itself, when it is added. */
    private void cleanse(LivingEntity entity, MobEffectInstance added, ResourceKey<MobEffect> key) {
        Protection protection = BuiltInRegistries.MOB_EFFECT.getData(EffectData.EFFECT_PROTECTION, key);
        if (protection == null || !protection.cleanse()) {
            return;
        }
        for (Holder<MobEffect> blocked : protection.blocks()) {
            if (!blocked.equals(added.getEffect()) && entity.removeEffect(blocked)) {
                log.debug("{} cleansed {} through {}", entity.getName().getString(), blocked.getRegisteredName(), key.identifier());
            }
        }
    }

    /** A finite incoming duration times the factor of every scaling attribute that matches it; infinite and hidden instances are left alone. */
    private void scale(LivingEntity entity, MobEffectInstance added) {
        if (added.getDuration() <= 0) {
            return;
        }
        double factor = 1.0D;
        boolean scaled = false;
        for (Map.Entry<ResourceKey<Attribute>, Scaling> entry : BuiltInRegistries.ATTRIBUTE.getDataMap(EffectData.SCALING).entrySet()) {
            if (entry.getValue().matches(added.getEffect())) {
                factor *= entry.getValue().factor(value(entity, entry.getKey()));
                scaled = true;
            }
        }
        if (scaled) {
            int before = added.duration;
            added.duration = Scaling.scale(before, factor);
            if (before != added.duration) {
                log.debug("{} scaled {} from {} to {} ticks", entity.getName().getString(), added.getEffect().getRegisteredName(), before, added.duration);
            }
        }
    }

    /** A second application of a stacking effect becomes the merged amplifier and duration; the game's update then raises the old instance to it. */
    private void stack(LivingEntity entity, MobEffectInstance added, MobEffectInstance old, ResourceKey<MobEffect> key) {
        Stacking stacking = BuiltInRegistries.MOB_EFFECT.getData(EffectData.STACKING, key);
        if (old == null || stacking == null) {
            return;
        }
        Stacking.Merged merged = stacking.merge(old.getAmplifier(), old.getDuration(), added.getAmplifier(), added.getDuration());
        log.debug("{} stacked {} from amplifier {} and {} to {}", entity.getName().getString(), key.identifier(), old.getAmplifier(), added.getAmplifier(), merged.amplifier());
        added.amplifier = merged.amplifier();
        added.duration = merged.duration();
    }

    // ------------------------------------------------------------------ cure and incurable

    /** The game's clear-all removes every active effect not in {@link EffectTags#INCURABLE}, and answers true when there was one to clear. */
    private void onClear(ClearEvent event) {
        LivingEntity entity = event.entity();
        if (entity.level().isClientSide()) {
            return;
        }
        List<Holder<MobEffect>> clearing = active(entity, effect -> !effect.is(EffectTags.INCURABLE));
        for (Holder<MobEffect> effect : clearing) {
            entity.removeEffect(effect);
        }
        event.setCleared(!clearing.isEmpty());
        log.debug("{} was cleared of {} effects through {}", entity.getName().getString(), clearing.size(), event.stack().getHoverName().getString());
    }

    /** Removes every active effect the tag lists, as a feature's named cure; answers whether there was one. Server side. */
    public boolean cure(LivingEntity entity, TagKey<MobEffect> cure) {
        List<Holder<MobEffect>> curing = active(entity, effect -> effect.is(cure));
        for (Holder<MobEffect> effect : curing) {
            entity.removeEffect(effect);
        }
        if (!curing.isEmpty()) {
            log.debug("{} was cured of {} effects through {}", entity.getName().getString(), curing.size(), cure.location());
        }
        return !curing.isEmpty();
    }

    private static List<Holder<MobEffect>> active(LivingEntity entity, Predicate<Holder<MobEffect>> matching) {
        List<Holder<MobEffect>> found = new ArrayList<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (matching.test(instance.getEffect())) {
                found.add(instance.getEffect());
            }
        }
        return found;
    }

    // ------------------------------------------------------------------ owned infinite effects

    /**
     * A source's infinite effect on an entity, called each tick or when its condition changes; one {@code source} per
     * held effect. Active: with no instance of the effect there, an infinite one is added and, if the game took it,
     * the source recorded; an instance already there is never replaced, and a recorded source whose infinite instance
     * has vanished from the chain is forgotten. Inactive: a recorded source's infinite instance of that amplifier is
     * unlinked from the chain, wherever a stronger finite effect has pushed it, and the source forgotten; a source
     * not recorded returns before anything is touched, so almost no entity carries the attachment. Server side.
     */
    public void hold(LivingEntity entity, Identifier source, Held held, boolean active) {
        if (active) {
            start(entity, source, held);
        } else if (OWNED.isSet(entity) && OWNED.get(entity).has(source)) {
            if (unlink(entity, held)) {
                forget(entity, source);
                log.debug("{} stopped holding {} for {}", source, held.effect().getRegisteredName(), entity.getName().getString());
            }
        }
    }

    private void start(LivingEntity entity, Identifier source, Held held) {
        MobEffectInstance present = entity.getEffect(held.effect());
        if (present == null) {
            MobEffectInstance infinite = new MobEffectInstance(held.effect(), MobEffectInstance.INFINITE_DURATION, held.amplifier(), held.ambient(), held.visible(), held.icon());
            if (entity.addEffect(infinite)) {
                OWNED.update(entity, owned -> owned.with(source));
                log.debug("{} holds {} for {}", source, held.effect().getRegisteredName(), entity.getName().getString());
            }
        } else if (OWNED.isSet(entity) && OWNED.get(entity).has(source) && !holds(present, held)) {
            forget(entity, source);
        }
    }

    private static void forget(LivingEntity entity, Identifier source) {
        Owned remaining = OWNED.get(entity).without(source);
        if (remaining.sources().isEmpty()) {
            OWNED.reset(entity);
        } else {
            OWNED.set(entity, remaining);
        }
    }

    private static boolean isHeld(MobEffectInstance instance, Held held) {
        return instance.isInfiniteDuration() && instance.getAmplifier() == held.amplifier();
    }

    /** Whether the chain starting at {@code head} holds the infinite instance of the held amplifier. */
    private static boolean holds(MobEffectInstance head, Held held) {
        for (MobEffectInstance node = head; node != null; node = node.hiddenEffect) {
            if (isHeld(node, held)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Takes the held instance out of the chain. The game's own merge gives a hidden chain only to a finite head, so an
     * infinite instance never has anything under it: held as the head it is removed alone, held below a finite head it
     * is spliced out in place and nothing passes through the rules again. Answers false only when the head is the held
     * instance and its removal was cancelled, so the source stays recorded and the next call tries again.
     */
    private static boolean unlink(LivingEntity entity, Held held) {
        MobEffectInstance head = entity.getEffect(held.effect());
        if (head == null) {
            return true;
        }
        if (isHeld(head, held)) {
            return entity.removeEffect(held.effect());
        }
        for (MobEffectInstance previous = head; previous.hiddenEffect != null; previous = previous.hiddenEffect) {
            MobEffectInstance node = previous.hiddenEffect;
            if (isHeld(node, held)) {
                previous.hiddenEffect = node.hiddenEffect;
                return true;
            }
        }
        return true;
    }
}
