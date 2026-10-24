package net.sievert.jolcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.registration.ModRegistration;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The composition root. Builds every primitive, composite and system by hand, registers each folder's content,
 * adds the listeners and payload handlers, and on the client calls each module's client bootstrap.
 * No static field holds a module and nothing below this class depends on it; see design/module-map.md.
 */
@Mod(JolCraft.MOD_ID)
public final class JolCraft {

    public static final String MOD_ID = Dictionary.JOLCRAFT;

    public JolCraft(IEventBus modBus, ModContainer container) {
        Log log = new ModLog(LogUtils.getLogger()).tagged(Dictionary.JOLCRAFT);
        ModRegistration registration = new ModRegistration();

        // Modules are built here, declaring their registries as they go.

        registration.register(modBus);
        log.info("JolCraft {} loading with {} registries of its own", container.getModInfo().getVersion(), registration.declared());
    }
}
