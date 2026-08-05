package net.sievert.jolcraft.world.item.custom.food.brewing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.sievert.jolcraft.world.block.fluid.util.brewing.BrewingTooltipHelper;

import java.util.List;

/**
 * A capability-backed brew container. It intentionally does not extend
 * BucketItem because brew components cannot survive placement as a world fluid.
 */
public final class DwarvenBrewBucketItem extends Item {

    public DwarvenBrewBucketItem(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        BrewingTooltipHelper.appendItemFluidTooltip(
                stack,
                tooltip::add
        );

        super.appendHoverText(
                stack,
                context,
                tooltip,
                flag
        );
    }
}
