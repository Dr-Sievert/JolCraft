package net.sievert.jolcraft.module.composite.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * The menu of a station: one {@link StationSlot} per cell in the layout's order, then the player's 27 main slots and
 * hotbar, then the kind's data slots. Built by the kind's factory on the server over the block entity and on the client
 * over an empty container. Its one {@code quickMoveStack} follows the roles: an output or storage cell moves to the
 * player from the end of the strip, an input or tool cell from its start, and a player slot tries the tool run, then the
 * input run, then the storage run, then swaps main and hotbar. {@link #valid}, {@link #clickMenuButton} and
 * {@link #removed} are the ones a menu over its own container overrides.
 */
@Actor
public class StationMenu extends AbstractContainerMenu {

    private static final int MAIN_SLOTS = 27;
    private static final int PLAYER_SLOTS = 36;
    private static final int INVENTORY_GAP = 14;
    private static final int INVENTORY_LEFT = 8;

    protected final StationKind<?> kind;
    protected final Container container;
    protected final ContainerData data;
    private final int stationEnd;
    private final int playerStart;
    private final int hotbarStart;
    private final int playerEnd;

    public StationMenu(StationKind<?> kind, int windowId, Inventory inventory, Container container, ContainerData data) {
        super(kind.menu().type(), windowId);
        StationLayout layout = kind.layout();
        checkContainerSize(container, layout.size());
        checkContainerDataCount(data, kind.dataCount());
        this.kind = kind;
        this.container = container;
        this.data = data;
        for (int index = 0; index < layout.size(); index++) {
            addSlot(new StationSlot(container, index, layout.cell(index), inventory.player));
        }
        this.stationEnd = layout.size();
        this.playerStart = stationEnd;
        this.hotbarStart = playerStart + MAIN_SLOTS;
        this.playerEnd = playerStart + PLAYER_SLOTS;
        addStandardInventorySlots(inventory, INVENTORY_LEFT, layout.panelHeight() + INVENTORY_GAP);
        addDataSlots(data);
        container.startOpen(inventory.player);
    }

    public StationKind<?> kind() {
        return kind;
    }

    public StationLayout layout() {
        return kind.layout();
    }

    /** The slot of the cell at the index. */
    public Slot stationSlot(int cell) {
        return slots.get(cell);
    }

    /** One int of the kind's data, as the server last synced it. */
    public int data(int index) {
        return data.get(index);
    }

    /** The share {@code numerator} is of {@code denominator}, from 0 to 1; 0 when either is not above zero. */
    public static float fraction(int numerator, int denominator) {
        return numerator <= 0 || denominator <= 0 ? 0.0F : Math.min(1.0F, (float) numerator / denominator);
    }

    /** The slot index after the last station cell. */
    public int stationEnd() {
        return stationEnd;
    }

    /** The slot index of the player's first main slot. */
    public int playerStart() {
        return playerStart;
    }

    /** The slot index of the player's first hotbar slot. */
    public int hotbarStart() {
        return hotbarStart;
    }

    /** The slot index after the player's last hotbar slot. */
    public int playerEnd() {
        return playerEnd;
    }

    /** Whether the player may keep this menu open; by default the container's own answer. */
    protected boolean valid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return valid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < stationEnd) {
            StationLayout.Cell cell = layout().cell(index);
            if (!moveItemStackTo(stack, playerStart, playerEnd, cell.role().backwards())) {
                return ItemStack.EMPTY;
            }
            if (cell.role().result()) {
                slot.onQuickCraft(stack, copy);
            }
        } else {
            boolean moved = false;
            for (StationLayout.Run run : layout().runs()) {
                if (moveItemStackTo(stack, run.start(), run.end(), false)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                boolean swapped = index < hotbarStart
                        ? moveItemStackTo(stack, hotbarStart, playerEnd, false)
                        : moveItemStackTo(stack, playerStart, hotbarStart, false);
                if (!swapped) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return !(target instanceof StationSlot station && station.cell().role().result());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return container instanceof StationBlockEntity station && player instanceof ServerPlayer live && station.onButton(live, id);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
