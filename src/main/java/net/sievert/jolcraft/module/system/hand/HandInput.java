package net.sievert.jolcraft.module.system.hand;

import java.util.Objects;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.stereotype.Value;

/** What a hand recipe is matched against: the main hand's stack, the offhand's, and whether the player sneaks. */
@Value
public record HandInput(ItemStack main, ItemStack off, boolean sneaking) implements RecipeInput {

    public HandInput {
        Objects.requireNonNull(main, "the main hand's stack");
        Objects.requireNonNull(off, "the offhand's stack");
    }

    /** The main hand's stack at 0, the offhand's at 1. */
    @Override
    public @NotNull ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> main;
            case 1 -> off;
            default -> throw new IllegalArgumentException("a hand input holds two stacks, not one at " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }

    /** Empty when either hand is, so the game asks no recipe then. */
    @Override
    public boolean isEmpty() {
        return main.isEmpty() || off.isEmpty();
    }
}
