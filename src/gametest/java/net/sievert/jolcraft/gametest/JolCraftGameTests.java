package net.sievert.jolcraft.gametest;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.gametest.module.composite.attachment.TestCooldowns;
import net.sievert.jolcraft.gametest.module.composite.loot.TestYields;
import net.sievert.jolcraft.gametest.module.composite.stat.TestLedgers;
import net.sievert.jolcraft.gametest.module.primitive.advancement.TestTriggers;
import net.sievert.jolcraft.gametest.module.primitive.attachment.TestAttachments;
import net.sievert.jolcraft.gametest.module.primitive.capability.TestCapabilities;
import net.sievert.jolcraft.gametest.module.primitive.command.TestCommands;
import net.sievert.jolcraft.gametest.module.primitive.data.TestData;
import net.sievert.jolcraft.gametest.module.primitive.loot.TestLoot;
import net.sievert.jolcraft.gametest.module.primitive.menu.TestMenus;
import net.sievert.jolcraft.gametest.module.primitive.network.TestPayloads;
import net.sievert.jolcraft.gametest.module.primitive.network.client.TestPayloadsClient;
import net.sievert.jolcraft.gametest.module.primitive.recipe.TestRecipes;
import net.sievert.jolcraft.gametest.module.primitive.schedule.TestSchedule;
import net.sievert.jolcraft.gametest.module.primitive.sound.TestSounds;
import net.sievert.jolcraft.gametest.module.primitive.stat.TestStats;
import net.sievert.jolcraft.gametest.scene.SceneRegistry;
import net.sievert.jolcraft.module.composite.attachment.Cooldown;
import net.sievert.jolcraft.module.composite.loot.Result;
import net.sievert.jolcraft.module.composite.stat.Ledger;
import net.sievert.jolcraft.module.composite.language.Feedback;
import net.sievert.jolcraft.module.composite.schedule.Action;
import net.sievert.jolcraft.module.primitive.language.ModLanguage;
import net.sievert.jolcraft.module.primitive.advancement.Advancement;
import net.sievert.jolcraft.module.primitive.advancement.ModAdvancement;
import net.sievert.jolcraft.module.primitive.attachment.ModAttachment;
import net.sievert.jolcraft.module.primitive.capability.ModCapability;
import net.sievert.jolcraft.module.primitive.command.ModCommand;
import net.sievert.jolcraft.module.primitive.data.ModData;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.loot.Loot;
import net.sievert.jolcraft.module.primitive.loot.ModLoot;
import net.sievert.jolcraft.module.primitive.registration.ModRegistration;
import net.sievert.jolcraft.module.primitive.menu.ModMenu;
import net.sievert.jolcraft.module.primitive.network.ModNetwork;
import net.sievert.jolcraft.module.primitive.recipe.ModRecipe;
import net.sievert.jolcraft.module.primitive.schedule.ModSchedule;
import net.sievert.jolcraft.module.primitive.sound.ModSound;
import net.sievert.jolcraft.module.primitive.stat.ModStat;
import net.sievert.jolcraft.module.primitive.stat.Stat;
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

    private static Loot loot;
    private static Advancement advancement;
    private static Stat stat;
    private static Action action;
    private static Feedback feedback;
    private static Cooldown cooldown;
    private static Ledger ledger;
    private static Result result;

    public JolCraftGameTests(IEventBus modBus) {
        Log log = new ModLog(LogUtils.getLogger()).tagged(Dictionary.GAMETEST);
        new SceneRegistry(MOD_ID, log).register(modBus);
        ModAttachment attachment = new ModAttachment();
        TestAttachments.declare(attachment);
        Cooldown cooldowns = new Cooldown(attachment, log);
        TestCooldowns.declare(cooldowns);
        attachment.register(modBus);
        cooldown = cooldowns;
        ModData data = new ModData();
        TestData.declare(data);
        ModSound sound = new ModSound();
        TestSounds.declare(sound);
        sound.register(modBus);
        ModSchedule schedule = new ModSchedule((_, thrown) -> TestSchedule.failures().add(thrown));
        schedule.register(NeoForge.EVENT_BUS);
        TestSchedule.attach(schedule);
        action = new Action(schedule, log);
        feedback = new Feedback(new ModLanguage(), log);
        ModLoot built = new ModLoot();
        TestLoot.declare(built);
        built.register(modBus);
        loot = built;
        ModRegistration registration = new ModRegistration();
        Result results = new Result(built, registration, log);
        TestYields.declare(results);
        registration.register(modBus);
        results.register(modBus);
        result = results;
        ModAdvancement criteria = new ModAdvancement();
        TestTriggers.declare(criteria);
        criteria.register(modBus);
        advancement = criteria;
        ModMenu menu = new ModMenu();
        TestMenus.declare(menu);
        menu.register(modBus);
        ModRecipe recipe = new ModRecipe();
        TestRecipes.declare(recipe);
        recipe.register(modBus);
        recipe.sync(NeoForge.EVENT_BUS);
        ModCommand command = new ModCommand();
        TestCommands.declare(command);
        command.register(NeoForge.EVENT_BUS);
        ModCapability capability = new ModCapability();
        TestCapabilities.declare(capability);
        capability.register(modBus);
        ModStat statistics = new ModStat();
        TestStats.declare(statistics);
        statistics.register(modBus);
        stat = statistics;
        Ledger ledgers = new Ledger(statistics, data, log);
        TestLedgers.declare(ledgers);
        ledgers.register(NeoForge.EVENT_BUS);
        ledger = ledgers;
        ModNetwork network = new ModNetwork();
        TestPayloads.declare(network);
        network.register(modBus, Dictionary.GAMETEST);
        if (FMLEnvironment.getDist().isClient()) {
            TestPayloadsClient.bootstrap(modBus);
        }
    }

    /** The game-test mod's loot, as its root built it, for the scenes: the adapter creates scene classes through a no-argument constructor and hands them nothing. */
    public static Loot loot() {
        return loot;
    }

    public static Advancement advancement() {
        return advancement;
    }

    public static Stat stat() {
        return stat;
    }

    public static Action action() {
        return action;
    }

    public static Feedback feedback() {
        return feedback;
    }

    public static Cooldown cooldown() {
        return cooldown;
    }

    public static Ledger ledger() {
        return ledger;
    }

    public static Result result() {
        return result;
    }
}
