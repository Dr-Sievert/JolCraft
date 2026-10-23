package net.sievert.jolcraft.event.client.game.data;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.network.packet.s2c.ClientboundConfigSyncPacket;
import net.sievert.jolcraft.world.loot.custom.reward.client.RewardLootTableClientCache;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = JolCraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class JolCraftClientDataEvents {

    private JolCraftClientDataEvents() {}

    @SubscribeEvent
    public static void onLoggingOut(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        // Minecraft.doWorldLoad starts with disconnect(), which fires this event with no player
        // after the world's datapacks have already filled the config managers. Those managers are
        // the integrated server's own singletons, so clearing them here would start every single
        // player world on empty defaults (no trade pools, no corruption effects). Only a real
        // logout, which has a player, leaves stale synced data behind.
        if (event.getPlayer() == null) {
            return;
        }

        RewardLootTableClientCache.clear();

        // When this client hosts the world, the config managers are the integrated server's own
        // singletons, and that server is still ticking and saving on its thread right now:
        // Minecraft.disconnect fires this event before it waits for the server to shut down.
        // Only a client that was connected to another server holds stale synced copies.
        Connection connection = event.getConnection();
        boolean hosting = connection != null
                ? connection.isMemoryConnection()
                : Minecraft.getInstance().isLocalServer();

        if (!hosting) {
            ClientboundConfigSyncPacket.clear();
        }
    }
}
