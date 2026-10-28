package net.sievert.jolcraft.module.composite.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * One slot of a station, built from its {@link StationLayout.Cell}: what may be placed is the cell's rule and the empty
 * slot shows the cell's hint sprite. A result cell keeps vanilla's result bookkeeping: a take counts the stack as
 * crafted, awards the crafted statistic and posts NeoForge's crafting event with the station as the container; the
 * {@code recipe_crafted} criterion is a feature's own. The items are real, so the slot is not fake.
 */
@Actor
public class StationSlot extends Slot {

    private final StationLayout.Cell cell;
    private final Player player;
    private int removeCount;

    public StationSlot(Container container, int index, StationLayout.Cell cell, Player player) {
        super(container, index, cell.x(), cell.y());
        this.cell = cell;
        this.player = player;
        cell.hint().ifPresent(this::setBackground);
    }

    public StationLayout.Cell cell() {
        return cell;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return cell.accepts(stack);
    }

    @Override
    public ItemStack remove(int amount) {
        if (cell.role().result() && hasItem()) {
            removeCount += Math.min(amount, getItem().getCount());
        }
        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack picked, int count) {
        if (cell.role().result()) {
            removeCount += count;
            checkTakeAchievements(picked);
        }
    }

    @Override
    protected void onSwapCraft(int count) {
        if (cell.role().result()) {
            removeCount += count;
        }
    }

    @Override
    protected void checkTakeAchievements(ItemStack carried) {
        if (cell.role().result()) {
            if (removeCount > 0) {
                carried.onCraftedBy(player, removeCount);
                EventHooks.firePlayerCraftingEvent(player, carried, container);
            }
            removeCount = 0;
        }
    }

    @Override
    public void onTake(Player taker, ItemStack carried) {
        checkTakeAchievements(carried);
        super.onTake(taker, carried);
    }
}
