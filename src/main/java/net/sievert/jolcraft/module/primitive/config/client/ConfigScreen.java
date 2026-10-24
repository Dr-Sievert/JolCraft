package net.sievert.jolcraft.module.primitive.config.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import net.sievert.jolcraft.stereotype.Helper;

/**
 * The client bootstrap of {@code config}: gives the mod NeoForge's configuration screen, reached from the mod
 * list, which shows every declared setting under its label and comment. The root calls it on the client only.
 */
@Helper
public final class ConfigScreen {

    private ConfigScreen() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
