package net.sievert.jolcraft.integration.jei.custom.dwarf_trade;

import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sievert.jolcraft.data.language.JolCraftDictionary;
import net.sievert.jolcraft.integration.jei.util.recipe.JeiItemOutcome;
import net.sievert.jolcraft.integration.jei.util.recipe.RewardCrateJeiResolver.ResolvedOutcome;
import net.sievert.jolcraft.util.JolCraftStrings;
import net.sievert.jolcraft.world.entity.custom.dwarf.profession.DwarfProfession;
import net.sievert.jolcraft.world.entity.custom.dwarf.trade.DwarfMerchantData;
import net.sievert.jolcraft.world.recipe.custom.dwarf_trade.DwarfTradeRecipe;
import net.sievert.jolcraft.world.recipe.custom.dwarf_trade.DwarfTradeRecipe.TradeCost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record JeiDwarfTradeRecipe(
        @NotNull DwarfTradeRecipe recipe,
        @NotNull DeferredItem<Item> spawnEgg,
        double tradeSelectionChance,
        @NotNull JeiItemOutcome outcome,
        @Nullable ItemStack rewardCrate,
        @NotNull List<ItemStack> inputAExamples,
        @NotNull List<ItemStack> inputBExamples,
        @NotNull AmountRange inputAmountA,
        @Nullable AmountRange inputAmountB
) {

    public JeiDwarfTradeRecipe {
        Objects.requireNonNull(
                recipe,
                JolCraftDictionary.RECIPE
        );

        Objects.requireNonNull(
                spawnEgg,
                JolCraftDictionary.ENTITY
        );

        if (!Double.isFinite(
                tradeSelectionChance
        ) || tradeSelectionChance < 0.0D
                || tradeSelectionChance > 1.0D) {
            throw new IllegalArgumentException(
                    "tradeSelectionChance must be between 0 and 1"
            );
        }

        Objects.requireNonNull(
                outcome,
                JolCraftDictionary.RESULT
        );

        if (rewardCrate != null) {
            if (rewardCrate.isEmpty()) {
                throw new IllegalArgumentException(
                        "rewardCrate must not be empty"
                );
            }

            rewardCrate = rewardCrate.copy();
        }

        inputAExamples =
                copyStacks(
                        inputAExamples,
                        "inputAExamples",
                        true
                );

        inputBExamples =
                copyStacks(
                        inputBExamples,
                        "inputBExamples",
                        false
                );

        Objects.requireNonNull(
                inputAmountA,
                "inputAAmount"
        );

        if (recipe.costB() == null) {
            if (!inputBExamples.isEmpty()
                    || inputAmountB != null) {
                throw new IllegalArgumentException(
                        "Input B preview must be absent when the recipe has no cost B"
                );
            }
        } else {
            if (inputBExamples.isEmpty()
                    || inputAmountB == null) {
                throw new IllegalArgumentException(
                        "Input B preview is required when the recipe has cost B"
                );
            }
        }
    }

    public static @NotNull List<JeiDwarfTradeRecipe> create(
            @NotNull DwarfTradeRecipe recipe,
            @NotNull DeferredItem<Item> spawnEgg,
            double tradeSelectionChance,
            @NotNull List<ResolvedOutcome> outcomes
    ) {
        Objects.requireNonNull(
                recipe,
                JolCraftDictionary.RECIPE
        );

        Objects.requireNonNull(
                spawnEgg,
                JolCraftDictionary.ENTITY
        );

        Objects.requireNonNull(
                outcomes,
                JolCraftStrings.plural(
                        JolCraftDictionary.RESULT
                )
        );

        List<ItemStack> inputAExamples =
                materializeInputs(
                        recipe.costA()
                );

        if (inputAExamples.isEmpty()) {
            throw new IllegalArgumentException(
                    "Dwarf trade cost A produced no JEI input examples"
            );
        }

        AmountRange inputAmountA =
                amountRange(
                        recipe.costA()
                                .count()
                );

        TradeCost costB =
                recipe.costB();

        List<ItemStack> inputBExamples;
        AmountRange inputAmountB;

        if (costB == null) {
            inputBExamples =
                    List.of();

            inputAmountB =
                    null;
        } else {
            inputBExamples =
                    materializeInputs(
                            costB
                    );

            if (inputBExamples.isEmpty()) {
                throw new IllegalArgumentException(
                        "Dwarf trade cost B produced no JEI input examples"
                );
            }

            inputAmountB =
                    amountRange(
                            costB.count()
                    );
        }

        List<JeiDwarfTradeRecipe> result =
                new ArrayList<>(
                        outcomes.size()
                );

        for (ResolvedOutcome resolved : outcomes) {
            result.add(
                    new JeiDwarfTradeRecipe(
                            recipe,
                            spawnEgg,
                            tradeSelectionChance,
                            resolved.outcome(),
                            resolved.rewardCrate(),
                            inputAExamples,
                            inputBExamples,
                            inputAmountA,
                            inputAmountB
                    )
            );
        }

        return List.copyOf(
                result
        );
    }

    public record AmountRange(
            int min,
            int max,
            boolean known
    ) {

        public AmountRange {
            if (known) {
                int normalizedMin =
                        Math.max(
                                1,
                                Math.min(
                                        min,
                                        max
                                )
                        );

                int normalizedMax =
                        Math.max(
                                normalizedMin,
                                Math.max(
                                        min,
                                        max
                                )
                        );

                min =
                        normalizedMin;

                max =
                        normalizedMax;
            } else {
                min =
                        1;

                max =
                        1;
            }
        }

        public static @NotNull AmountRange fixed(
                int value
        ) {
            int normalized =
                    Math.max(
                            1,
                            value
                    );

            return new AmountRange(
                    normalized,
                    normalized,
                    true
            );
        }

        public static @NotNull AmountRange range(
                int min,
                int max
        ) {
            return new AmountRange(
                    min,
                    max,
                    true
            );
        }

        public static @NotNull AmountRange unknown() {
            return new AmountRange(
                    1,
                    1,
                    false
            );
        }
    }

    public @NotNull DwarfProfession profession() {
        return recipe.profession();
    }

    public @Nullable DwarfMerchantData.Level level() {
        return recipe.merchantLevel();
    }

    public boolean hasInputB() {
        return !inputBExamples.isEmpty();
    }

    public boolean costAItemIs(
            @NotNull TagKey<Item> tag
    ) {
        return recipe.costA()
                .contains(tag);
    }

    public boolean costBItemIs(
            @NotNull TagKey<Item> tag
    ) {
        TradeCost costB =
                recipe.costB();

        return costB != null
                && costB.contains(tag);
    }

    public @NotNull ItemStack outputExample() {
        return normalizeForJei(
                outcome.stack()
        );
    }

    public boolean hasRewardCrate() {
        return rewardCrate != null;
    }

    public @NotNull ItemStack rewardCrateExample() {
        if (rewardCrate == null) {
            return ItemStack.EMPTY;
        }

        return normalizeForJei(
                rewardCrate
        );
    }

    public @NotNull AmountRange outputAmount() {
        return AmountRange.range(
                outcome.minCount(),
                outcome.maxCount()
        );
    }

    public double outputChancePerRoll() {
        return outcome.chancePerRoll();
    }

    public int outputMinRolls() {
        return outcome.minRolls();
    }

    public int outputMaxRolls() {
        return outcome.maxRolls();
    }

    public boolean outputHasMultipleRolls() {
        return outcome.hasMultipleRolls();
    }
    public boolean outputGuaranteedPerRoll() {
        return outputChancePerRoll() >= 1.0D;
    }

    public boolean tradeGuaranteed() {
        return tradeSelectionChance >= 1.0D;
    }

    private static @NotNull List<ItemStack> materializeInputs(
            @NotNull TradeCost cost
    ) {
        List<ItemStack> resolved =
                new ArrayList<>();

        for (ItemStack candidate : cost.candidateItems()) {
            if (candidate.isEmpty() || !cost.test(candidate)) {
                continue;
            }

            ItemStack normalized =
                    normalizeForJei(
                            candidate
                    );

            if (containsEquivalent(
                    resolved,
                    normalized
            )) {
                continue;
            }

            resolved.add(
                    normalized
            );
        }

        return List.copyOf(
                resolved
        );
    }

    private static boolean containsEquivalent(
            @NotNull List<ItemStack> stacks,
            @NotNull ItemStack candidate
    ) {
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(
                    stack,
                    candidate
            )) {
                return true;
            }
        }

        return false;
    }

    /*
     * Amounts come from the number providers alone: constant and uniform
     * providers give an exact value or a range, anything else is unknown. JEI
     * runs on the client thread, so no loot context is built here. The old
     * single-player shortcut read the integrated server's player and showed a
     * fixed-seed sample as if it were exact, while a dedicated server showed
     * unknown for the same recipe.
     */
    private static @NotNull AmountRange amountRange(
            @NotNull NumberProvider provider
    ) {
        if (provider instanceof ConstantValue(float value)) {
            return AmountRange.fixed(Mth.floor(value));
        }

        //noinspection DeconstructionCanBeUsed
        if (provider instanceof UniformGenerator uniform) {
            Integer min = providerMinimum(uniform.min());
            Integer max = providerMaximum(uniform.max());

            if (min == null || max == null) {
                return AmountRange.unknown();
            }

            return AmountRange.range(min, max);
        }

        return AmountRange.unknown();
    }

    private static @Nullable Integer providerMinimum(
            @NotNull NumberProvider provider
    ) {
        if (provider instanceof ConstantValue(float value)) {
            return Mth.floor(value);
        }

        if (provider instanceof UniformGenerator uniform) {
            return providerMinimum(uniform.min());
        }

        return null;
    }

    private static @Nullable Integer providerMaximum(
            @NotNull NumberProvider provider
    ) {
        if (provider instanceof ConstantValue(float value)) {
            return Mth.floor(value);
        }

        if (provider instanceof UniformGenerator uniform) {
            return providerMaximum(uniform.max());
        }

        return null;
    }

    private static @NotNull ItemStack normalizeForJei(
            @NotNull ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy =
                stack.copy();

        copy.setCount(
                1
        );

        return copy;
    }

    private static @NotNull List<ItemStack> copyStacks(
            @NotNull List<ItemStack> stacks,
            @NotNull String name,
            boolean required
    ) {
        Objects.requireNonNull(
                stacks,
                name
        );

        List<ItemStack> copies =
                stacks.stream()
                        .map(ItemStack::copy)
                        .toList();

        if (required
                && copies.isEmpty()) {
            throw new IllegalArgumentException(
                    name
                            + " must contain at least one stack"
            );
        }

        return copies;
    }
}
