package net.sievert.jolcraft.gametest.module.system.hand;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.composite.loot.ResultHook;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.system.outcome.OutcomeKeys;

/**
 * A hook in the mod's own hook registry, which the hand fixtures name: the main mod declares none, so the game-test mod
 * registers this one there at {@code RegisterEvent}, under its own namespace.
 */
public final class TestHandHooks {

    /** Refuses every delivery it is named in. */
    public static final Identifier REFUSING = Ids.of(JolCraftGameTests.MOD_ID, "refusing");

    private TestHandHooks() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, event -> event.register(OutcomeKeys.YIELDS.hooks(), helper -> helper.register(REFUSING, new ResultHook() {
            @Override
            public boolean canApply(LootContext context) {
                return false;
            }
        })));
    }
}
