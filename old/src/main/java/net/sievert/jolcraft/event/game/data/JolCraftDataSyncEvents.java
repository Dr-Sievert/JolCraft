package net.sievert.jolcraft.event.game.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.network.JolCraftNetworking;
import net.sievert.jolcraft.network.packet.s2c.ClientboundConfigSyncPacket;
import net.sievert.jolcraft.network.packet.s2c.ClientboundRewardLootTablesPacket;
import net.sievert.jolcraft.util.log.JolCraftLogTags;
import net.sievert.jolcraft.util.log.JolCraftLogs;
import net.sievert.jolcraft.world.loot.custom.reward.RewardLootTableSync;

import java.util.List;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = JolCraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class JolCraftDataSyncEvents {

    private JolCraftDataSyncEvents() {}

    @SubscribeEvent
    public static void onDatapackSync(
            OnDatapackSyncEvent event
    ) {
        MinecraftServer server = event.getPlayerList().getServer();

        var lootTables = RewardLootTableSync.collect(server);

        ClientboundRewardLootTablesPacket lootTablePacket =
                new ClientboundRewardLootTablesPacket(
                        lootTables
                );

        ClientboundConfigSyncPacket configPacket =
                ClientboundConfigSyncPacket.current();

        // Materialised, not peek()'d: Stream#count can skip the pipeline when the source is SIZED.
        List<ServerPlayer> players =
                event.getRelevantPlayers().toList();

        for (ServerPlayer player : players) {
            JolCraftNetworking.sendToClient(
                    player,
                    lootTablePacket
            );

            JolCraftNetworking.sendToClient(
                    player,
                    configPacket
            );
        }

        JolCraftLogs.info(
                JolCraftLogTags.NETWORK,
                "Datapack sync: reward loot tables = {}, dwarf profession configs = {}, players = {}",
                lootTables.size(),
                configPacket.dwarfProfessions().size(),
                players.size()
        );
    }
}
