package net.sievert.jolcraft.gametest.module.primitive.capability;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.capability.Capability;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** Three capabilities of the game-test mod's own, given to armor stands, sticks and chests; the game-test mod declares the providers in its root. */
public final class TestCapabilities {

    public static final EntityCapability<String, Void> GREETING = EntityCapability.createVoid(Ids.of(JolCraftGameTests.MOD_ID, "greeting"), String.class);
    public static final ItemCapability<String, Void> LABEL = ItemCapability.createVoid(Ids.of(JolCraftGameTests.MOD_ID, "label"), String.class);
    public static final BlockCapability<String, Direction> SIGN = BlockCapability.createSided(Ids.of(JolCraftGameTests.MOD_ID, "sign"), String.class);

    private TestCapabilities() {
    }

    public static void declare(Capability capability) {
        capability.entity(GREETING, () -> EntityType.ARMOR_STAND, (stand, _) -> "hello from " + stand.getType().toShortString());
        capability.blockEntity(SIGN, () -> BlockEntityType.CHEST, (_, side) -> "a chest seen from " + (side == null ? "within" : side.getSerializedName()));
        capability.item(LABEL, (_, _) -> "a stick", Items.STICK);
    }
}
