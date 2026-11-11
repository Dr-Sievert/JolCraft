package net.sievert.jolcraft.module.primitive.tab;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping, the title and what a tab built from the entries shows; the game's registries are frozen here, so the registered tab is checked beside the mod's own key. */
class ModTabTest {

    private final TabKey tools = TabKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "tools"));
    private final TabKey food = TabKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "food"));
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void aTabIsDeclaredOncePerId() {
        ModTab tab = new ModTab();
        tab.declare(tools);
        tab.declare(food);
        Assertions.assertEquals(2, tab.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> tab.declare(TabKey.of(tools.id())));
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModTab().declare(TabKey.of(Identifier.withDefaultNamespace("tools"))));
    }

    @Test
    void entriesKeepTheirOrderAcrossCallsAndTheBuiltTabDisplaysThem() {
        ModTab tab = new ModTab();
        tab.declare(tools);
        tab.add(tools, Items.STICK, Items.APPLE);
        tab.add(tools, Items.COAL);
        Assertions.assertEquals(List.of(Items.STICK, Items.APPLE, Items.COAL), displayed(tab, tools));
    }

    @Test
    void theIconIsTheFirstEntryAndTheEmptyStackForAnEmptyTab() {
        ModTab tab = new ModTab();
        tab.declare(tools);
        tab.declare(food);
        tab.add(tools, Items.APPLE, Items.STICK);
        Assertions.assertSame(Items.APPLE, tab.icon(tools).orElseThrow());
        Assertions.assertTrue(tab.icon(food).isEmpty());
        Assertions.assertEquals(List.of(), displayed(tab, food));
    }

    @Test
    void theSameItemTwiceInOneTabIsRefusedButTwoTabsMayShareOne() {
        ModTab tab = new ModTab();
        tab.add(tools, Items.STICK);
        Assertions.assertThrows(IllegalStateException.class, () -> tab.add(tools, Items.STICK));
        Assertions.assertThrows(IllegalStateException.class, () -> tab.add(food, Items.APPLE, Items.APPLE));
        tab.add(food, Items.STICK);
    }

    @Test
    void declaringOrAddingAfterRegisterIsRefusedAndSoIsRegisteringTwice() {
        ModTab tab = new ModTab();
        tab.declare(tools);
        tab.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> tab.declare(food));
        Assertions.assertThrows(IllegalStateException.class, () -> tab.add(tools, Items.STICK));
        Assertions.assertThrows(IllegalStateException.class, () -> tab.register(bus));
    }

    @Test
    void registeringEntriesOfAnUndeclaredTabFailsNamingIt() {
        ModTab tab = new ModTab();
        tab.declare(tools);
        tab.add(food, Items.APPLE);
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, () -> tab.register(bus));
        Assertions.assertTrue(refused.getMessage().contains("jolcraft:food"), refused.getMessage());
    }

    @Test
    void anEntryAddedBeforeItsTabIsDeclaredIsKept() {
        ModTab tab = new ModTab();
        tab.add(tools, Items.STICK);
        tab.declare(tools);
        tab.register(bus);
        Assertions.assertEquals(List.of(Items.STICK), displayed(tab, tools));
    }

    @Test
    void theTitleIsTheModsKeyUnderItsNamespaceAndTheTabIsTitledWithIt() {
        Assertions.assertEquals("jolcraft.tab.tools", tools.title());
        Assertions.assertEquals("jolcraft.tab.tools", Tab.title(tools.id()));
        ModTab tab = new ModTab();
        TranslatableContents title = (TranslatableContents) tab.build(tools).getDisplayName().getContents();
        Assertions.assertEquals("jolcraft.tab.tools", title.getKey());
        Assertions.assertFalse(tools.isRegistered());
        Assertions.assertThrows(IllegalStateException.class, tools::tab);
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.TAB, new ModTab().id());
    }

    /** What the tab's generator hands the game, in order; the game's own build needs the tab registered, and no stack can be made before the items are bound, so the output takes the items as they are. */
    private static List<ItemLike> displayed(ModTab tab, TabKey key) {
        List<ItemLike> shown = new ArrayList<>();
        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false, null);
        tab.display(key).accept(parameters, new CreativeModeTab.Output() {
            @Override
            public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
                throw new AssertionError("the tab hands items, not stacks");
            }

            @Override
            public void accept(ItemLike item) {
                shown.add(item);
            }
        });
        return shown;
    }
}
