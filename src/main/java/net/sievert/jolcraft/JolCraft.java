package net.sievert.jolcraft;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.log.ServerLog;
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
        Log log = new ServerLog(LogUtils.getLogger()).tagged(Dictionary.JOLCRAFT);
        log.info("JolCraft {} loading", container.getModInfo().getVersion());
    }
}
