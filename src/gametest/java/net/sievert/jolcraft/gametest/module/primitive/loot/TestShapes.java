package net.sievert.jolcraft.gametest.module.primitive.loot;

import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.loot.Shape;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** The shape the shape scenes build contexts under: a place, and an entity when there is one. */
public final class TestShapes {

    public static final Shape AT_A_PLACE = Shape.of(Ids.of(JolCraftGameTests.MOD_ID, "at_a_place"), LootContextParams.ORIGIN).allowing(LootContextParams.THIS_ENTITY);

    private TestShapes() {
    }
}
