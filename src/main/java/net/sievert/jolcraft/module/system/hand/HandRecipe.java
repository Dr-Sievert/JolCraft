package net.sievert.jolcraft.module.system.hand;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.sievert.jolcraft.module.composite.loot.EffectYield;
import net.sievert.jolcraft.module.composite.loot.EntityYield;
import net.sievert.jolcraft.module.composite.loot.ItemYield;
import net.sievert.jolcraft.module.composite.loot.SoundYield;
import net.sievert.jolcraft.module.composite.loot.Yield;
import net.sievert.jolcraft.module.primitive.recipe.ItemInput;
import net.sievert.jolcraft.module.primitive.recipe.Recipe;
import net.sievert.jolcraft.module.primitive.recipe.RecipeKey;
import net.sievert.jolcraft.module.system.outcome.OutcomeKeys;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * A recipe fired by using one item with another in the other hand: the {@code tool} and the {@code target}, each an
 * input with its fate, matched in either orientation; the {@code results}, at least one yield of the four kinds the
 * outcome system delivers, validated against {@link HandContext#SHAPE}; an optional {@code success} and
 * {@code failure} sound, validated the same way; whether the player must be {@code sneaking}; and the
 * {@code cooldown} in ticks both items get once it matched. No stereotype, since it names two handles, its key and the
 * mod's yield kinds; no static of it reads {@link HandKeys}, so initialisation runs one way, the keys, this record,
 * then the outcome keys.
 */
public record HandRecipe(ItemInput tool, ItemInput target, List<Yield> results, Optional<SoundYield> success, Optional<SoundYield> failure,
                         boolean sneaking, int cooldown) implements Recipe.Custom<HandInput> {

    /** The ticks both items wait when a recipe names none, the old interaction's. */
    public static final int COOLDOWN = 10;

    private static final Codec<List<Yield>> RESULTS = OutcomeKeys.YIELDS.listCodec()
            .validate(HandRecipe::deliverable)
            .validate(HandContext.SHAPE.listValidator());
    private static final Codec<SoundYield> SOUND = OutcomeKeys.YIELDS.single(SoundYield.CODEC).validate(HandContext.SHAPE.validator());

    public static final MapCodec<HandRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemInput.CODEC.fieldOf(Dictionary.TOOL).forGetter(HandRecipe::tool),
            ItemInput.CODEC.fieldOf(Dictionary.TARGET).forGetter(HandRecipe::target),
            RESULTS.fieldOf(Dictionary.RESULTS).forGetter(HandRecipe::results),
            SOUND.optionalFieldOf(Dictionary.SUCCESS).forGetter(HandRecipe::success),
            SOUND.optionalFieldOf(Dictionary.FAILURE).forGetter(HandRecipe::failure),
            Codec.BOOL.optionalFieldOf(Dictionary.SNEAKING, false).forGetter(HandRecipe::sneaking),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf(Dictionary.COOLDOWN, COOLDOWN).forGetter(HandRecipe::cooldown)).apply(instance, HandRecipe::new));

    /**
     * The recipe through its data codec under the receiver's registries, as {@code YieldKinds.streamCodec()} is, since
     * an input's predicate has no wire form of its own: a client reads it with the hook check and the predicate's tags
     * resolved against its own registries.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, HandRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public HandRecipe {
        Objects.requireNonNull(tool, "a tool");
        Objects.requireNonNull(target, "a target");
        Objects.requireNonNull(success, "a success sound or none");
        Objects.requireNonNull(failure, "a failure sound or none");
        results = List.copyOf(results);
        deliverable(results).getOrThrow(IllegalArgumentException::new);
        if (cooldown < 1) {
            throw new IllegalArgumentException("a cooldown is at least one tick, not " + cooldown);
        }
    }

    /**
     * At least one result, each of the four kinds the outcome system delivers; another module's kind is refused,
     * naming it, since nothing here could act on it.
     */
    static DataResult<List<Yield>> deliverable(List<Yield> results) {
        if (results.isEmpty()) {
            return DataResult.error(() -> "a hand recipe has at least one result");
        }
        List<Yield> foreign = results.stream().filter(yield -> !(yield instanceof ItemYield || yield instanceof EntityYield
                || yield instanceof SoundYield || yield instanceof EffectYield)).toList();
        return foreign.isEmpty() ? DataResult.success(results)
                : DataResult.error(() -> "a hand recipe delivers item, entity, sound and effect yields alone, not " + foreign);
    }

    @Override
    public RecipeKey<HandInput, HandRecipe> key() {
        return HandKeys.RECIPE;
    }

    /** Whether the hands hold this recipe in either orientation and the sneak rule holds; pure, the same on both sides. */
    @Override
    public boolean matches(HandInput input, Level level) {
        return (!sneaking || input.sneaking()) && orientation(input.main(), input.off()).isPresent();
    }

    /** The tool's hand: the main hand when the tool matches its stack and the target the offhand's, else the offhand for the reverse, else none. */
    public Optional<InteractionHand> orientation(ItemStack main, ItemStack off) {
        if (tool.matches(main) && target.matches(off)) {
            return Optional.of(InteractionHand.MAIN_HAND);
        }
        if (tool.matches(off) && target.matches(main)) {
            return Optional.of(InteractionHand.OFF_HAND);
        }
        return Optional.empty();
    }
}
