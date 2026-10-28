package net.sievert.jolcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import net.sievert.jolcraft.module.composite.attachment.Cooldown;
import net.sievert.jolcraft.module.composite.capability.Inventory;
import net.sievert.jolcraft.module.composite.loot.Result;
import net.sievert.jolcraft.module.composite.menu.Station;
import net.sievert.jolcraft.module.composite.loot.YieldKinds;
import net.sievert.jolcraft.module.composite.stat.Ledger;
import net.sievert.jolcraft.module.composite.language.Feedback;
import net.sievert.jolcraft.module.composite.schedule.Action;
import net.sievert.jolcraft.module.primitive.advancement.ModAdvancement;
import net.sievert.jolcraft.module.primitive.attachment.ModAttachment;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.capability.ModCapability;
import net.sievert.jolcraft.module.primitive.command.ModCommand;
import net.sievert.jolcraft.module.primitive.config.ModConfig;
import net.sievert.jolcraft.module.primitive.config.client.ConfigScreen;
import net.sievert.jolcraft.module.primitive.data.ModData;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.loot.ModLoot;
import net.sievert.jolcraft.module.primitive.menu.ModMenu;
import net.sievert.jolcraft.module.primitive.language.Language;
import net.sievert.jolcraft.module.primitive.language.ModLanguage;
import net.sievert.jolcraft.module.primitive.network.ModNetwork;
import net.sievert.jolcraft.module.primitive.recipe.ModRecipe;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.primitive.registration.ModRegistration;
import net.sievert.jolcraft.module.primitive.schedule.ModSchedule;
import net.sievert.jolcraft.module.primitive.sound.ModSound;
import net.sievert.jolcraft.module.primitive.stat.ModStat;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The composition root. Builds every primitive, composite and system by hand, registers each folder's content,
 * registers the primitives, adds the listeners, and on the client calls each module's client bootstrap.
 * No static field holds a module and nothing below this class depends on it; see design/module-map.md.
 */
@Mod(JolCraft.MOD_ID)
public final class JolCraft {

    public static final String MOD_ID = Dictionary.JOLCRAFT;

    public JolCraft(IEventBus modBus, ModContainer container) {
        Log log = new ModLog(LogUtils.getLogger()).tagged(Dictionary.JOLCRAFT);
        ModRegistration registration = new ModRegistration();
        ModAttachment attachment = new ModAttachment();
        ModData data = new ModData();
        ModSound sound = new ModSound();
        ModRecipe recipe = new ModRecipe();
        ModLoot loot = new ModLoot();
        ModAdvancement advancement = new ModAdvancement();
        ModMenu menu = new ModMenu();
        ModCommand command = new ModCommand();
        ModCapability capability = new ModCapability();
        ModStat stat = new ModStat();
        ModSchedule schedule = new ModSchedule((_, thrown) -> log.error("a scheduled task failed", thrown));
        Language language = new ModLanguage();
        ModConfig config = new ModConfig();
        ModNetwork network = new ModNetwork();

        // Built here so the root owns them (rule 5); no feature holds them yet.
        @SuppressWarnings("unused") Action action = new Action(schedule, log);
        @SuppressWarnings("unused") Feedback feedback = new Feedback(language, log);
        @SuppressWarnings("unused") Cooldown cooldown = new Cooldown(attachment, log);
        Ledger ledger = new Ledger(stat, data, log);
        Result result = new Result(loot, registration, log);
        result.declare(YieldKinds.of(Ids.of(MOD_ID, Dictionary.YIELD)));
        Inventory inventory = new Inventory(capability, log);
        Station station = new Station(menu, log);

        // Systems are built here, taking the primitives and the composites above through their constructors; each folder's content is registered.

        registration.register(modBus);
        attachment.register(modBus);
        sound.register(modBus);
        recipe.register(modBus);
        recipe.sync(NeoForge.EVENT_BUS);
        loot.register(modBus);
        advancement.register(modBus);
        menu.register(modBus);
        capability.register(modBus);
        stat.register(modBus);
        schedule.register(NeoForge.EVENT_BUS);
        command.register(NeoForge.EVENT_BUS);
        ledger.register(NeoForge.EVENT_BUS);
        result.register(modBus);
        config.register(container);
        network.register(modBus, container.getModInfo().getVersion().toString());
        if (config.declared() > 0 && FMLEnvironment.getDist().isClient()) {
            ConfigScreen.register(container);
        }
        log.info("JolCraft {} loading with {} registries of its own, {} attachments, {} saved data, {} sounds, {} recipe kinds, {} loot kinds, {} criteria, {} menus,"
                + " {} command trees, {} capability providers, {} statistics, {} settings, {} payloads, {} ledger books, {} yield kinds and hooks, {} exposed inventories and {} stations",
                container.getModInfo().getVersion(), registration.declared(), attachment.declared(), data.declared(), sound.declared(), recipe.declared(), loot.declared(),
                advancement.declared(), menu.declared(), command.declared(), capability.declared(), stat.declared(), config.declared(), network.declared(), ledger.declared(), result.declared(),
                inventory.declared(), station.declared());
    }
}
