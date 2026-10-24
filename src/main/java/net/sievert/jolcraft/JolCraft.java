package net.sievert.jolcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import net.sievert.jolcraft.module.primitive.attachment.ModAttachment;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.config.ModConfig;
import net.sievert.jolcraft.module.primitive.config.client.ConfigScreen;
import net.sievert.jolcraft.module.primitive.data.ModData;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.language.Language;
import net.sievert.jolcraft.module.primitive.language.ModLanguage;
import net.sievert.jolcraft.module.primitive.network.ModNetwork;
import net.sievert.jolcraft.module.primitive.registration.ModRegistration;
import net.sievert.jolcraft.module.primitive.sound.ModSound;
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
        Language language = new ModLanguage();
        ModConfig config = new ModConfig();
        ModNetwork network = new ModNetwork();

        // Modules are built here, taking log, language, config, network, attachment, data, sound and registration through their constructors.

        registration.register(modBus);
        attachment.register(modBus);
        sound.register(modBus);
        config.register(container);
        network.register(modBus, container.getModInfo().getVersion().toString());
        if (config.declared() > 0 && FMLEnvironment.getDist().isClient()) {
            ConfigScreen.register(container);
        }
        log.info("JolCraft {} loading with {} registries of its own, {} attachments, {} saved data, {} sounds, {} settings and {} payloads",
                container.getModInfo().getVersion(), registration.declared(), attachment.declared(), data.declared(), sound.declared(), config.declared(), network.declared());
    }
}
