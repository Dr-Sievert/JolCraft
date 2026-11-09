package net.sievert.jolcraft.module.composite.menu.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.module.composite.menu.StationLayout;
import net.sievert.jolcraft.module.composite.menu.StationMenu;
import net.sievert.jolcraft.stereotype.Actor;

/**
 * The screen of a station, {@value StationLayout#WIDTH} pixels wide and the layout's panel and the player's strip high.
 * It draws the panel, a well under every station slot and the vanilla inventory strip; the panel and the wells are the
 * kind's art sprites when it names them, else the game's own art, the wells being {@code container/slot} and the panel
 * the top of {@code generic_54.png} with one clean row of it stretched down to the strip, so a resource pack that
 * restyles the game's GUI restyles a station along with it and the composite ships no texture. A station with nothing
 * to add registers this class itself; a feature subclasses it and calls {@link #progress} from its
 * {@link #extractBackground} after {@code super} and {@link #button} from its {@link #init} after {@code super}.
 */
@Actor
public class StationScreen<M extends StationMenu> extends AbstractContainerScreen<@NotNull M> {

    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier WELL = Identifier.withDefaultNamespace("container/slot");
    private static final int TEXTURE = 256;
    private static final int PANEL_TOP = 17;
    private static final int STRIP_V = 126;
    private static final int STRIP_HEIGHT = 96;
    private static final int LABEL_GAP = 3;

    public StationScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title, StationLayout.WIDTH, menu.layout().panelHeight() + STRIP_HEIGHT);
        this.inventoryLabelY = menu.layout().panelHeight() + LABEL_GAP;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int panelHeight = menu.layout().panelHeight();
        menu.kind().panel().ifPresentOrElse(
                sprite -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, leftPos, topPos, StationLayout.WIDTH, panelHeight),
                () -> defaultPanel(graphics, panelHeight));
        Identifier well = menu.kind().slot().orElse(WELL);
        for (int cell = 0; cell < menu.stationEnd(); cell++) {
            Slot slot = menu.stationSlot(cell);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, well, leftPos + slot.x - 1, topPos + slot.y - 1, StationLayout.FRAME, StationLayout.FRAME);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + panelHeight, 0.0F, STRIP_V, StationLayout.WIDTH, STRIP_HEIGHT, TEXTURE, TEXTURE);
    }

    /** The chest texture's top rows, then its clean row 16 stretched to the panel's bottom, which joins the strip without a seam. */
    private void defaultPanel(GuiGraphicsExtractor graphics, int panelHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, StationLayout.WIDTH, PANEL_TOP, TEXTURE, TEXTURE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + PANEL_TOP, 0.0F, PANEL_TOP - 1, StationLayout.WIDTH, panelHeight - PANEL_TOP,
                StationLayout.WIDTH, 1, TEXTURE, TEXTURE);
    }

    /**
     * Draws the left {@code fraction} (0 to 1) of the sprite, of {@code width} by {@code height} pixels, at the panel pixel
     * {@code x}, {@code y}, from the left as the furnace's arrow fills; a subclass calls it after {@code super}.
     */
    protected void progress(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height, float fraction) {
        int columns = Mth.ceil(Mth.clamp(fraction, 0.0F, 1.0F) * width);
        if (columns > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, width, height, 0, 0, leftPos + x, topPos + y, columns, height);
        }
    }

    /**
     * Adds a button whose item sits at the panel pixel {@code x}, {@code y} as a cell's would, its 18 by 18 frame one pixel
     * above and left of it, and answers it so a subclass can set its active state later. Refuses a frame that leaves the
     * panel or overlaps a cell's, since widgets draw under slots. A subclass calls it from its {@link #init} after
     * {@code super}.
     */
    protected StationButton button(int x, int y, ItemStack icon, int id, boolean active) {
        menu.layout().checkFrame("button " + id + " of " + menu.kind().id(), x, y);
        StationButton button = new StationButton(leftPos + x - 1, topPos + y - 1, icon, menu.containerId, id);
        button.active = active;
        return addRenderableWidget(button);
    }
}
