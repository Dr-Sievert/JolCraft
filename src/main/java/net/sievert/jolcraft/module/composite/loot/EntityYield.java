package net.sievert.jolcraft.module.composite.loot;

import java.util.List;
import java.util.Objects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/** Entities of a type, as many as the count provider rolls; none when it rolls nothing. */
@Value
public record EntityYield(EntityType<?> type, NumberProvider count, List<Identifier> hooks) implements Yield {

    public static final MapCodec<EntityYield> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf(Dictionary.ENTITY).forGetter(EntityYield::type),
            NumberProviders.CODEC.optionalFieldOf(Dictionary.COUNT, ConstantValue.exactly(1.0F)).forGetter(EntityYield::count),
            Identifier.CODEC.listOf().optionalFieldOf(Dictionary.HOOKS, List.of()).forGetter(EntityYield::hooks)).apply(instance, EntityYield::new));

    public EntityYield {
        Objects.requireNonNull(type, "an entity type");
        Objects.requireNonNull(count, "a count");
        hooks = List.copyOf(hooks);
    }

    public static EntityYield of(EntityType<?> type, int count, Identifier... hooks) {
        return new EntityYield(type, ConstantValue.exactly(count), List.of(hooks));
    }

    @Override
    public MapCodec<EntityYield> codec() {
        return CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        Yield.super.validate(context);
        count.validate(context.forField(Dictionary.COUNT));
    }

    @Override
    public List<? extends Yielded> roll(LootContext context) {
        int rolled = count.getInt(context);
        return rolled <= 0 ? List.of() : List.of(new Yielded.Entities(type, rolled));
    }
}
