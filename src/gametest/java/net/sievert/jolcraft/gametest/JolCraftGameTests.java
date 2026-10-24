package net.sievert.jolcraft.gametest;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.gametest.module.primitive.network.TestPayloads;
import net.sievert.jolcraft.gametest.module.primitive.network.client.TestPayloadsClient;
import net.sievert.jolcraft.gametest.scene.SceneRegistry;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.network.ModNetwork;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Dev-only mod carrying the game tests (src/gametest/resources/META-INF/neoforge.mods.toml). The client,
 * server and game-test runs load it; the jar never does. A test is a method annotated {@link net.sievert.jolcraft.gametest.scene.Scene} on any
 * class of this source set, found through NeoForge's class-scan data and registered by {@link SceneRegistry}:
 * no JSON and no central list. runGameTestServer runs every registered scene and exits non-zero on failure.
 */
@Mod(JolCraftGameTests.MOD_ID)
public final class JolCraftGameTests {

    public static final String MOD_ID = JolCraft.MOD_ID + "_" + Dictionary.GAMETEST;

    public JolCraftGameTests(IEventBus modBus) {
        Log log = new ModLog(LogUtils.getLogger()).tagged(Dictionary.GAMETEST);
        new SceneRegistry(MOD_ID, log).register(modBus);
        ModNetwork network = new ModNetwork();
        TestPayloads.declare(network);
        network.register(modBus, Dictionary.GAMETEST);
        if (FMLEnvironment.getDist().isClient()) {
            TestPayloadsClient.bootstrap(modBus);
        }
    }
}
