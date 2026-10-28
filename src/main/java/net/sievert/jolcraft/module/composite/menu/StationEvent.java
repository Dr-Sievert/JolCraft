package net.sievert.jolcraft.module.composite.menu;

import net.minecraft.server.level.ServerPlayer;

import net.sievert.jolcraft.stereotype.Event;

/**
 * What a station announces on the game bus, on the server only, for the system that owns the station to act on with its
 * modules; {@code station().kind()} tells whose it is.
 */
@Event
public abstract class StationEvent extends net.neoforged.bus.api.Event {

    private final StationBlockEntity station;

    StationEvent(StationBlockEntity station) {
        this.station = station;
    }

    public StationBlockEntity station() {
        return station;
    }

    /** A player pressed menu button {@code id} on a station. A listener that acts on it calls {@link #setHandled}. */
    public static final class Button extends StationEvent {

        private final ServerPlayer player;
        private final int id;
        private boolean handled;

        public Button(ServerPlayer player, StationBlockEntity station, int id) {
            super(station);
            this.player = player;
            this.id = id;
        }

        public ServerPlayer player() {
            return player;
        }

        public int id() {
            return id;
        }

        /** Whether a listener acted on the button. */
        public boolean handled() {
            return handled;
        }

        public void setHandled() {
            handled = true;
        }
    }

    /** The cells of a station differ from what they were at the last check: after a click, a hopper, a transfer or the load. */
    public static final class Changed extends StationEvent {

        public Changed(StationBlockEntity station) {
            super(station);
        }
    }
}
