package net.sievert.jolcraft.event.game.world.item;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.world.item.material.trim.JolCraftTrimAttributes;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = JolCraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class JolCraftItemEvents {

    private JolCraftItemEvents() {}

    /** HIGH so listeners from other mods see the item's defaults and the trim modifiers together. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        JolCraftTrimAttributes.applyAttribute(event);
    }
}
