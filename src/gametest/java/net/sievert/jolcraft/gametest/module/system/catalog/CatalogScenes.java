package net.sievert.jolcraft.gametest.module.system.catalog;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.catalog.CatalogKeys;
import net.sievert.jolcraft.module.system.flora.FloraContent;

/** The mod's creative tab as the game builds it for a level: what the systems added, in the order they were built. */
public final class CatalogScenes {

    @Scene(maxTicks = 20)
    public void theTabDisplaysTheFloraItemsInOrder(TestScene scene) {
        ServerLevel level = scene.level();
        CreativeModeTab tab = CatalogKeys.TAB.tab();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
        List<Item> shown = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
        List<Item> expected = List.of(FloraContent.BARLEY_SEEDS.get(), FloraContent.BARLEY.get(), FloraContent.BARLEY_BLOCK_ITEM.get(),
                FloraContent.ASGARNIAN_SEEDS.get(), FloraContent.DUSKHOLD_SEEDS.get(), FloraContent.KRANDONIAN_SEEDS.get(), FloraContent.YANILLIAN_SEEDS.get(),
                FloraContent.ASGARNIAN_HOPS.get(), FloraContent.DUSKHOLD_HOPS.get(), FloraContent.KRANDONIAN_HOPS.get(), FloraContent.YANILLIAN_HOPS.get(),
                FloraContent.SKYBELL_ITEM.get(), FloraContent.CYANELLA_ITEM.get(), FloraContent.BLOODROOT_ITEM.get(),
                FloraContent.DUSKCAP_ITEM.get(), FloraContent.DUSKCAP_BLOCK_ITEM.get(), FloraContent.DUSKCAP_STEM_ITEM.get(),
                FloraContent.FESTERLING_ITEM.get(), FloraContent.FESTERLING_BLOCK_ITEM.get(), FloraContent.FESTERLING_STEM_ITEM.get(),
                FloraContent.DEEPSLATE_BULBS.get(), FloraContent.VERDANT_SOIL_ITEM.get(), FloraContent.VERDANT_FARMLAND_ITEM.get());
        scene.check("the tab displays exactly the twenty-three flora items, in the order they were added", () -> scene.assertEquals(expected, shown, "displayed items"));
        scene.succeed();
    }
}
