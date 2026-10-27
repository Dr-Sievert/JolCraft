package net.sievert.jolcraft.module.composite.capability;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.BusBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.primitive.capability.ModCapability;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.recipe.Fate;
import net.sievert.jolcraft.util.text.Dictionary;

/** The composite's shape and guards; counting and giving need stacks, which the test JVM cannot make, and are scenes in the game-test mod. */
class InventoryTest {

    @Test
    void exposingGoesThroughTheCapabilityPrimitiveAndTheGuardsHold() {
        ModCapability capability = new ModCapability();
        Inventory inventory = new Inventory(capability, new ModLog(LogUtils.getLogger()));
        Assertions.assertEquals(Dictionary.INVENTORY, inventory.id());
        Assertions.assertEquals(0, inventory.declared());
        inventory.exposeBlockEntity(() -> BlockEntityType.CHEST);
        Assertions.assertEquals(1, capability.declared(), "one provider declared to the primitive");
        Assertions.assertEquals(1, inventory.declared());
        capability.register(BusBuilder.builder().build());
        Assertions.assertThrows(IllegalStateException.class, () -> inventory.exposeBlockEntity(() -> BlockEntityType.CHEST), "exposing after registration");
        Assertions.assertEquals(4, Inventory.Holder.class.getPermittedSubclasses().length, "a player, a hand, an entity and a block");
        Assertions.assertThrows(IllegalArgumentException.class, () -> inventory.has(new Inventory.Holder.Block(null, BlockPos.ZERO, null), _ -> true, 0), "an amount below one");
    }

    @Test
    void anInputClaimsItsLargestAmountOrTheOneItemItUses() {
        Assertions.assertEquals(5, Inventory.claimed(Fate.consume(2, 5)), "a consume claims its largest amount");
        Assertions.assertEquals(3, Inventory.claimed(Fate.consume(3)));
        Assertions.assertEquals(1, Inventory.claimed(Fate.damage(2, 9)), "a damage claims the one item it wears");
        Assertions.assertEquals(1, Inventory.claimed(Fate.keep()), "a keep claims the one item it needs present");
    }
}
