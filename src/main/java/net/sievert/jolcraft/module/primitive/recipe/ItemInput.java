package net.sievert.jolcraft.module.primitive.recipe;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.world.item.ItemStack;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * One item a recipe takes: the game's own predicate (items or a tag, components) and the {@link Fate} of the matched
 * stack. The predicate's count must be unbounded, so the fate's amount is the one source of how many; data with a
 * bounded count is refused at load. {@link #matches} is pure and runs on both sides.
 */
@Value
public record ItemInput(ItemPredicate predicate, Fate fate) {

    public static final Codec<ItemInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemPredicate.CODEC.validate(ItemInput::unboundedCount).fieldOf(Dictionary.PREDICATE).forGetter(ItemInput::predicate),
            Fate.CODEC.optionalFieldOf(Dictionary.FATE, Fate.consume(1)).forGetter(ItemInput::fate)).apply(instance, ItemInput::new));

    public ItemInput {
        Objects.requireNonNull(predicate, "a predicate");
        Objects.requireNonNull(fate, "a fate");
        unboundedCount(predicate).getOrThrow(IllegalArgumentException::new);
    }

    public static ItemInput of(ItemPredicate.Builder predicate, Fate fate) {
        return new ItemInput(predicate.build(), fate);
    }

    private static DataResult<ItemPredicate> unboundedCount(ItemPredicate predicate) {
        return predicate.count().equals(MinMaxBounds.Ints.ANY) ? DataResult.success(predicate)
                : DataResult.error(() -> "the predicate bounds its count; the fate's amount is the one source of how many");
    }

    /** Whether the stack is this input in full: the predicate holds and the count covers the fate. */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && predicate.test(stack) && fate.covers(stack);
    }
}
