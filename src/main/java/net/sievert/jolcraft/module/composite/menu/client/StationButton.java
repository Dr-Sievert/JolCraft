package net.sievert.jolcraft.module.composite.menu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.module.composite.menu.StationLayout;
import net.sievert.jolcraft.stereotype.Actor;

/**
 * A button of a station screen: the vanilla button sprite in its 18 by 18 frame with an item centred over it, greyed when
 * inactive by the disabled sprite alone since items take no tint. A press sends the game's container-button click for the
 * menu's container id and the button id and changes nothing on the client; the server's menu decides. The message is the
 * icon's name, read for narration and never drawn.
 */
@Actor
public class StationButton extends Button {

    private final ItemStack icon;

    /** A button whose frame's top left is at the screen pixel {@code x}, {@code y}. Built through {@link StationScreen}. */
    protected StationButton(int x, int y, ItemStack icon, int containerId, int id) {
        super(x, y, StationLayout.FRAME, StationLayout.FRAME, icon.getHoverName(), _ -> press(containerId, id), DEFAULT_NARRATION);
        this.icon = icon.copy();
    }

    private static void press(int containerId, int id) {
        MultiPlayerGameMode game = Minecraft.getInstance().gameMode;
        if (game != null) {
            game.handleInventoryButtonClick(containerId, id);
        }
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractDefaultSprite(graphics);
        graphics.item(icon, getX() + 1, getY() + 1);
    }
}
