package net.sievert.jolcraft.module.system.outcome;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The mod's yield registries keep the ids the root gave them when it built them itself, so no data changes. */
class OutcomeKeysTest {

    @Test
    void theYieldsNameTheModsRegistriesOfKindsAndHooks() {
        Assertions.assertEquals(Identifier.fromNamespaceAndPath("jolcraft", "yield"), OutcomeKeys.YIELDS.id());
        Assertions.assertEquals(Identifier.fromNamespaceAndPath("jolcraft", "yield"), OutcomeKeys.YIELDS.kinds().identifier());
        Assertions.assertEquals(Identifier.fromNamespaceAndPath("jolcraft", "yield_hook"), OutcomeKeys.YIELDS.hooks().identifier());
    }
}
