package net.sievert.jolcraft.module.primitive.menu;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping; the game's registries are frozen here, so scenes in the game-test mod open a real menu for a real player. */
class ModMenuTest {

    static final class Empty extends AbstractContainerMenu {
        Empty(MenuType<?> type, int windowId) {
            super(type, windowId);
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

    private final MenuKey<Empty> bench = MenuKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "bench"), (windowId, _, _) -> new Empty(null, windowId));
    private final MenuKey<Empty> chest = MenuKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "chest"), (windowId, _, _) -> new Empty(null, windowId));
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void menusAreCountedOncePerIdAndBuildTheirType() {
        ModMenu menu = new ModMenu();
        menu.declare(bench);
        menu.declare(chest);
        Assertions.assertEquals(2, menu.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> menu.declare(bench));
        Assertions.assertNotNull(bench.build());
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, bench::type);
        Assertions.assertTrue(refused.getMessage().contains("declared"), refused.getMessage());
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ModMenu().declare(MenuKey.of(Identifier.withDefaultNamespace("bench"), (windowId, _, _) -> new Empty(null, windowId))));
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModMenu menu = new ModMenu();
        menu.declare(bench);
        menu.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> menu.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> menu.declare(chest));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.MENU, new ModMenu().id());
    }
}
