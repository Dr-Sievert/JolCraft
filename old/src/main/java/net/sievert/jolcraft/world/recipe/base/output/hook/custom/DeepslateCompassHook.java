package net.sievert.jolcraft.world.recipe.base.output.hook.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.sievert.jolcraft.world.item.JolCraftItems;
import net.sievert.jolcraft.world.item.component.JolCraftDataComponents;
import net.sievert.jolcraft.world.item.component.custom.compass.DeepslateCompassStructureGroup;
import net.sievert.jolcraft.world.entity.attachment.player.custom.compass.DiscoveredStructuresAttachmentHelper;
import net.sievert.jolcraft.world.entity.attachment.player.custom.compass.DiscoveredStructuresAttachmentHelper.LocatedStructure;
import net.sievert.jolcraft.world.recipe.custom.hand.HandInteractionRecipeInput;
import net.sievert.jolcraft.world.recipe.base.output.hook.RecipeHook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DeepslateCompassHook implements RecipeHook {

    private static final int SEARCH_RADIUS = 20;

    /** Everything the hook needs, resolved from the input and context; null when a precondition fails. */
    private record Target(
            ItemStack compass,
            ItemStack dial,
            LocatedStructure located
    ) {}

    @Override
    public boolean canApply(
            @NotNull LootContext context,
            @NotNull RecipeInput input
    ) {
        return resolve(context, input) != null;
    }

    @Override
    public boolean apply(
            @NotNull LootContext context,
            @NotNull Object generatedOutput,
            @NotNull RecipeInput input
    ) {
        if (!(generatedOutput instanceof ItemStack output) || output.isEmpty()) {
            return false;
        }

        Target target = resolve(context, input);

        if (target == null) {
            return false;
        }

        var dyedColor =
                target.compass().get(
                        DataComponents.DYED_COLOR
                );

        if (dyedColor != null) {
            output.set(
                    DataComponents.DYED_COLOR,
                    dyedColor
            );
        } else {
            output.remove(
                    DataComponents.DYED_COLOR
            );
        }

        output.set(
                JolCraftDataComponents
                        .STRUCTURE_GROUP
                        .get(),
                target.located().structureId().toString()
        );

        var dialColor =
                target.dial().get(
                        JolCraftDataComponents
                                .DEEPSLATE_COMPASS_DIAL_COLOR
                                .get()
                );

        if (dialColor != null) {
            output.set(
                    JolCraftDataComponents
                            .DEEPSLATE_COMPASS_DIAL_COLOR
                            .get(),
                    dialColor
            );
        } else {
            output.remove(
                    JolCraftDataComponents
                            .DEEPSLATE_COMPASS_DIAL_COLOR
                            .get()
            );
        }

        output.set(
                JolCraftDataComponents
                        .DEEPSLATE_COMPASS_TARGET
                        .get(),
                target.located().pos()
        );

        return true;
    }

    /**
     * Resolves compass, dial and the nearest undiscovered structure of the dial's group. It runs once
     * for the precondition and once for the output; the structure search is a bounded locate behind
     * the hand-interaction cooldown, so the repeat is acceptable.
     */
    @Nullable
    private static Target resolve(
            @NotNull LootContext context,
            @NotNull RecipeInput input
    ) {
        if (!(input instanceof HandInteractionRecipeInput(
                ItemStack ingredientA,
                ItemStack ingredientB
        ))) {
            return null;
        }

        if (ingredientA.isEmpty() || ingredientB.isEmpty()) {
            return null;
        }

        ItemStack compass;
        ItemStack dial;

        if (ingredientA.is(JolCraftItems.EMPTY_DEEPSLATE_COMPASS.get())
                && ingredientB.is(JolCraftItems.DEEPSLATE_COMPASS_DIAL.get())) {

            compass = ingredientA;
            dial = ingredientB;

        } else if (ingredientB.is(JolCraftItems.EMPTY_DEEPSLATE_COMPASS.get())
                && ingredientA.is(JolCraftItems.DEEPSLATE_COMPASS_DIAL.get())) {

            compass = ingredientB;
            dial = ingredientA;

        } else {
            return null;
        }

        if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) {
            return null;
        }

        String groupId =
                dial.get(
                        JolCraftDataComponents
                                .STRUCTURE_GROUP
                                .get()
                );

        if (groupId == null || groupId.isBlank()) {
            return null;
        }

        TagKey<Structure> structureTag =
                DeepslateCompassStructureGroup
                        .structureTag(groupId);

        if (structureTag == null) {
            return null;
        }

        LocatedStructure located =
                DiscoveredStructuresAttachmentHelper
                        .findNearestUndiscoveredStructure(
                                player,
                                structureTag,
                                SEARCH_RADIUS
                        );

        if (located == null) {
            return null;
        }

        return new Target(compass, dial, located);
    }
}
