package net.sievert.jolcraft.module.system.hand.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.sievert.jolcraft.module.system.hand.HandInput;
import net.sievert.jolcraft.module.system.hand.HandKeys;
import net.sievert.jolcraft.stereotype.Helper;

/**
 * The client bootstrap of {@code hand}: keeps the client from predicting the use of an item a hand recipe takes. It
 * keeps the recipes of the kind the server sends, drops them on leaving a world, and consumes the main hand's use on
 * the client when a kept recipe matches both hands, at the server's priority and with the server's checks. So the
 * client starts no use of its own, does not go on to the offhand, and swings nothing itself: the server's swing is the
 * one it shows. It asks no hook, since a refusal on the server consumes the click too. The root calls it on the client
 * only.
 */
@Helper
public final class HandClient {

    private HandClient() {
    }

    public static void register(IEventBus gameBus) {
        ReceivedRecipes received = new ReceivedRecipes();
        gameBus.addListener((RecipesReceivedEvent event) -> received.keep(event.getRecipeMap().byType(HandKeys.RECIPE.type())));
        gameBus.addListener((ClientPlayerNetworkEvent.LoggingOut _) -> received.drop());
        gameBus.addListener(EventPriority.LOW, (PlayerInteractEvent.RightClickItem event) -> onUse(received, event));
    }

    private static void onUse(ReceivedRecipes received, PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemCooldowns cooldowns = player.getCooldowns();
        if (main.isEmpty() || off.isEmpty() || cooldowns.isOnCooldown(main) || cooldowns.isOnCooldown(off)) {
            return;
        }
        if (received.anyMatches(new HandInput(main, off, player.isSecondaryUseActive()), event.getLevel())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }
}
