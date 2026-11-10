package net.sievert.jolcraft.module.system.damage;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.primitive.registration.Registration;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Damage affinity, read from the {@code jolcraft:affinity} datapack registry ({@link Affinity}): at
 * {@code LivingDamageEvent.Pre}, at {@link Stage#AFFINITY}, the damage is multiplied by every row that governs its
 * type, and when an entity joins a level the innate shares of its kind are written as transient attribute modifiers.
 * The registry is not synced, only the server reads it, and the data pack that fills it is the feature's. Everything
 * acts on the server. Design in design/effect.md.
 */
@ModSystem(Dictionary.DAMAGE)
public final class Damage implements Module {

    /** The registry of affinity rows. */
    public static final ResourceKey<Registry<Affinity>> AFFINITY = Ids.registry(Dictionary.JOLCRAFT, Dictionary.AFFINITY);

    public Damage(Registration registration) {
        registration.datapack(AFFINITY, Affinity.CODEC, null);
    }

    /** Adds the system's listeners to the game bus. */
    public void register(IEventBus gameBus) {
        gameBus.addListener(Stage.AFFINITY, this::onPre);
        gameBus.addListener(this::onJoin);
    }

    private static List<Holder.Reference<Affinity>> rows(Level level) {
        return level.registryAccess().lookup(AFFINITY).map(lookup -> lookup.listElements().toList()).orElse(List.of());
    }

    private static double value(LivingEntity entity, Holder<Attribute> attribute) {
        return entity.getAttributes().hasAttribute(attribute) ? entity.getAttributeValue(attribute) : 0.0D;
    }

    private void onPre(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        double multiplier = Affinity.multiplier(rows(entity.level()).stream().map(Holder.Reference::value).toList(), event.getSource().typeHolder(), attribute -> value(entity, attribute));
        if (multiplier != 1.0D) {
            event.setNewDamage((float) Math.max(0.0D, event.getNewDamage() * multiplier));
        }
    }

    private void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        Holder<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType());
        for (Holder.Reference<Affinity> row : rows(event.getLevel())) {
            Affinity affinity = row.value();
            Affinity.Strength strength = affinity.innate(type);
            Identifier id = row.key().identifier();
            write(entity, affinity.resistance(), Affinity.resistanceModifier(id), strength.resistance());
            write(entity, affinity.vulnerability(), Affinity.vulnerabilityModifier(id), strength.vulnerability());
        }
    }

    /** Idempotent: a player changing dimension stays the same object and joins again, where adding the same modifier twice would throw. */
    private static void write(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance instance = entity.getAttributes().getInstance(attribute);
        if (instance == null) {
            return;
        }
        if (amount != 0.0D) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        } else {
            instance.removeModifier(id);
        }
    }
}
