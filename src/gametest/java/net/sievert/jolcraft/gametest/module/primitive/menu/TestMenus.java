package net.sievert.jolcraft.gametest.module.primitive.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.menu.Menu;
import net.sievert.jolcraft.module.primitive.menu.MenuKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** The menu the menu scenes open: a counter the server sets and the client is sent; the game-test mod declares it in its root. */
public final class TestMenus {

    /** A menu with one number, kept in a data slot so the game syncs it to the client's copy. */
    public static final class Counter extends AbstractContainerMenu {
        private final DataSlot count = DataSlot.standalone();

        public Counter(int windowId, int count) {
            super(COUNTER.type(), windowId);
            addDataSlot(this.count);
            this.count.set(count);
        }

        public int count() {
            return count.get();
        }

        public void count(int amount) {
            count.set(amount);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final MenuKey<Counter> COUNTER = MenuKey.of(Ids.of(JolCraftGameTests.MOD_ID, "counter"), (windowId, _, data) -> new Counter(windowId, data.readVarInt()));

    private TestMenus() {
    }

    public static void declare(Menu menu) {
        menu.declare(COUNTER);
    }
}
