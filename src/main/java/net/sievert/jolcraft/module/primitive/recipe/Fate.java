package net.sievert.jolcraft.module.primitive.recipe;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * What becomes of a matched input when a recipe fires: consumed by an amount, damaged by an amount, or kept. The
 * amount is a provider of the game's own form, a number or an inclusive range, rolled when the fate is applied; a
 * stack {@link #covers} the fate only when it can pay it in full, a consumed stack by its count reaching the largest
 * amount, a damaged one by being damageable at all, so a matching recipe is never half paid. A creative holder pays
 * nothing.
 */
@Value
public record Fate(Kind kind, IntProvider amount) {

    public enum Kind implements StringRepresentable {
        CONSUME(Dictionary.CONSUME),
        DAMAGE(Dictionary.DAMAGE),
        KEEP(Dictionary.KEEP);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final Codec<Kind> KIND_CODEC = StringRepresentable.fromEnum(Kind::values);
    public static final Codec<Fate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KIND_CODEC.fieldOf(Dictionary.KIND).forGetter(Fate::kind),
            IntProviders.POSITIVE_CODEC.optionalFieldOf(Dictionary.AMOUNT, ConstantInt.of(1)).forGetter(Fate::amount)).apply(instance, Fate::new));

    public Fate {
        Objects.requireNonNull(kind, "a kind");
        Objects.requireNonNull(amount, "an amount");
    }

    public static Fate consume(int amount) {
        return new Fate(Kind.CONSUME, ConstantInt.of(amount));
    }

    public static Fate consume(int least, int most) {
        return new Fate(Kind.CONSUME, UniformInt.of(least, most));
    }

    public static Fate damage(int amount) {
        return new Fate(Kind.DAMAGE, ConstantInt.of(amount));
    }

    public static Fate damage(int least, int most) {
        return new Fate(Kind.DAMAGE, UniformInt.of(least, most));
    }

    public static Fate keep() {
        return new Fate(Kind.KEEP, ConstantInt.of(1));
    }

    /** Whether the stack can take the fate in full: a kept stack always, a consumed one by its count, a damaged one by being damageable. */
    public boolean covers(ItemStack stack) {
        return switch (kind) {
            case KEEP -> true;
            case CONSUME -> stack.getCount() >= amount.maxInclusive();
            case DAMAGE -> stack.isDamageableItem();
        };
    }

    /**
     * Applies the fate to the stack with the level's random and answers the amount applied: nothing for a kept stack
     * or a creative holder. Damage goes through the game's own wear, so an unbreaking enchantment applies, a worn-out
     * stack empties, and the holder hears it break.
     */
    public int apply(ItemStack stack, ServerLevel level, @Nullable ServerPlayer holder) {
        if (kind == Kind.KEEP || (holder != null && holder.getAbilities().instabuild)) {
            return 0;
        }
        int rolled = amount.sample(level.getRandom());
        switch (kind) {
            case CONSUME -> stack.shrink(rolled);
            case DAMAGE -> stack.hurtAndBreak(rolled, level, holder, _ -> {
                if (holder != null) {
                    level.playSound(null, holder.blockPosition(), SoundEvents.ITEM_BREAK.value(), holder.getSoundSource(), 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                }
            });
            case KEEP -> { }
        }
        return rolled;
    }
}
