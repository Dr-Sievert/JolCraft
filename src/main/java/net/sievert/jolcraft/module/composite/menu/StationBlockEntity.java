package net.sievert.jolcraft.module.composite.menu;

import java.util.OptionalInt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * The block entity of a station, over the game's own storage base so the lock code, the custom name and the loot table
 * behave as for a vanilla container. Its cells are the kind's layout: {@link #canPlaceItem} is the cell's rule, the one
 * statement the menu slot reads too, a hopper or an exposed capability takes through a face only from output and
 * storage cells, and a null side is unrestricted. It calls no module: a change posts {@link StationEvent.Changed}, a
 * menu button {@link StationEvent.Button}, and the system that owns the station listens. A feature's own mechanics that
 * need no module, a progress counter or a recipe lookup, may live in a subclass. Constructed over a kind that was
 * declared through {@link Station}, which it checks.
 */
@Actor
public abstract class StationBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

    private final StationKind<?> kind;
    private final int[] allSlots;
    private NonNullList<ItemStack> items;
    private NonNullList<ItemStack> snapshot;
    private boolean checking;

    protected StationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, StationKind<?> kind) {
        super(type, pos, state);
        try {
            kind.menu().type();
        } catch (IllegalStateException unbound) {
            throw new IllegalStateException(kind + " has no menu type: declare it through Station in the system's constructor", unbound);
        }
        this.kind = kind;
        int size = kind.layout().size();
        this.allSlots = new int[size];
        for (int index = 0; index < size; index++) {
            allSlots[index] = index;
        }
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.snapshot = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public StationKind<?> kind() {
        return kind;
    }

    public StationLayout layout() {
        return kind.layout();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return layout().cell(slot).accepts(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return allSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return layout().cell(slot).role().extractable();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(layout().size(), ItemStack.EMPTY);
        if (!tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, items);
        }
    }

    @Override
    protected Component getDefaultName() {
        return kind.title().text();
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return kind.create(id, inventory, this, data(inventory.player));
    }

    /** What the menu reads of this station for one viewer: {@code kind.dataCount()} ints, answered by {@link #readData}, never written from the client. */
    protected ContainerData data(Player viewer) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return readData(index, viewer);
            }

            @Override
            public void set(int index, int amount) {
            }

            @Override
            public int getCount() {
                return kind.dataCount();
            }
        };
    }

    /** One int of the menu's data for the viewer; a kind with data overrides it, per viewer. */
    protected int readData(int index, Player viewer) {
        throw new IllegalStateException(kind + " has data but its block entity does not override readData");
    }

    /** Opens this station's menu for the player, as the game's own provider. The window id, or empty when the game did not open it. */
    public OptionalInt open(ServerPlayer player) {
        return player.openMenu(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel && !checking && differsFromSnapshot()) {
            refresh();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            refresh();
        }
    }

    private boolean differsFromSnapshot() {
        for (int index = 0; index < items.size(); index++) {
            if (!ItemStack.matches(items.get(index), snapshot.get(index))) {
                return true;
            }
        }
        return false;
    }

    private void refresh() {
        checking = true;
        try {
            contentsChanged();
        } finally {
            for (int index = 0; index < items.size(); index++) {
                snapshot.set(index, items.get(index).copy());
            }
            checking = false;
        }
    }

    /** Runs on the server when the cells differ from what they were at the last check, and once when loaded; posts {@link StationEvent.Changed}. */
    protected void contentsChanged() {
        NeoForge.EVENT_BUS.post(new StationEvent.Changed(this));
    }

    /** A player pressed menu button {@code id}; posts {@link StationEvent.Button} and answers whether a listener handled it. */
    protected boolean onButton(ServerPlayer player, int id) {
        StationEvent.Button event = new StationEvent.Button(player, this, id);
        NeoForge.EVENT_BUS.post(event);
        return event.handled();
    }

    /**
     * Merges the stack into the cell whatever the cell's rule, since an output refuses everything else: an empty cell takes a
     * copy up to the stack limit, a cell of the same item and components grows to it, any other takes nothing. The given
     * stack is not changed; the remainder that did not fit is a new stack, empty when all of it did.
     */
    public ItemStack deliver(int cell, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack held = getItem(cell);
        int limit = getMaxStackSize(stack);
        int moved;
        if (held.isEmpty()) {
            moved = Math.min(stack.getCount(), limit);
            items.set(cell, stack.copyWithCount(moved));
        } else if (ItemStack.isSameItemSameComponents(held, stack)) {
            moved = Math.max(0, Math.min(stack.getCount(), limit - held.getCount()));
            held.grow(moved);
        } else {
            moved = 0;
        }
        if (moved > 0) {
            setChanged();
        }
        return moved >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    /** What travels to the clients: the cells when the kind says so, nothing by default else, never the lock code. */
    protected void writeSynced(ValueOutput output) {
        if (kind.sendsItems()) {
            ContainerHelper.saveAllItems(output, items, true);
        }
    }

    /** Reads what {@link #writeSynced} wrote. */
    protected void readSynced(ValueInput input) {
        if (kind.sendsItems()) {
            items = NonNullList.withSize(layout().size(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writeSynced(output);
        return output.buildResult();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        if (level == null) {
            return null;
        }
        CompoundTag tag = getUpdateTag(level.registryAccess());
        return tag.isEmpty() ? null : ClientboundBlockEntityDataPacket.create(this, (_, _) -> tag);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        readSynced(input);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        readSynced(input);
    }

    /** Sends this block entity's synced state to the clients that track it. */
    public void sync() {
        Sync.send(this);
    }

    /** Whether the contents drop when the block is removed; a storage whose item carries its contents answers false. */
    protected boolean dropsContents() {
        return true;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (dropsContents()) {
            super.preRemoveSideEffects(pos, state);
        }
    }
}
