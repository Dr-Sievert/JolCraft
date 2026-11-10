package net.sievert.jolcraft.module.system.control.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.entity.player.Input;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

import net.sievert.jolcraft.module.system.control.Control;
import net.sievert.jolcraft.module.system.control.Stop;
import net.sievert.jolcraft.stereotype.Helper;

/**
 * The client bootstrap of {@code control}: keeps the local player's own input from being sent while a {@link Stop}
 * holds it, so the server's refusal is not the first the player hears of it. The decisions are {@link Control#stops},
 * the same the server makes. The root calls it on the client only.
 */
@Helper
public final class ControlClient {

    private ControlClient() {
    }

    public static void register(IEventBus gameBus, Control control) {
        gameBus.addListener((InputEvent.InteractionKeyMappingTriggered event) -> onInteraction(control, event));
        gameBus.addListener((MovementInputUpdateEvent event) -> onMovement(control, event));
        gameBus.addListener((CalculatePlayerTurnEvent event) -> onTurn(control, event));
    }

    private static void onInteraction(Control control, InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean stopped = event.isAttack() && control.stops(player, Stop.ATTACK)
                || (event.isUseItem() || event.isPickBlock()) && control.stops(player, Stop.of(player.getMainHandItem()));
        if (stopped) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static void onMovement(Control control, MovementInputUpdateEvent event) {
        if (control.stops(event.getEntity(), Stop.MOVE)) {
            event.getInput().keyPresses = Input.EMPTY;
            event.getInput().moveVector = Vec2.ZERO;
        }
    }

    private static void onTurn(Control control, CalculatePlayerTurnEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && control.stops(player, Stop.LOOK)) {
            // the game turns by (sensitivity * 0.6 + 0.2)^3: this cancels its 0.2 floor, which 0 would leave
            event.setMouseSensitivity(-0.2D / 0.6D);
        }
    }
}
