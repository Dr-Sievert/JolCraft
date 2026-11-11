package net.sievert.jolcraft.module.system.hand;

import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.sievert.jolcraft.module.primitive.loot.Shape;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The context a hand recipe's results and sounds are validated against and rolled with: the player, where they stand,
 * and the two matched stacks, the tool's under the game's own {@code TOOL}, which {@code match_tool} and the
 * enchantment-level functions read, and the target's under {@link #TARGET}. Both stacks are the match's copies, so a
 * hook reads them and changes nothing in a hand.
 */
@Value
public final class HandContext {

    /** The matched target stack, {@code jolcraft:target}. */
    public static final ContextKey<ItemInstance> TARGET = new ContextKey<>(Ids.of(Dictionary.JOLCRAFT, Dictionary.TARGET));

    /** {@code jolcraft:hand}: the player, their position, the tool's stack and the target's, all required. */
    public static final Shape SHAPE = Shape.of(Ids.of(Dictionary.JOLCRAFT, Dictionary.HAND),
            LootContextParams.THIS_ENTITY, LootContextParams.ORIGIN, LootContextParams.TOOL, TARGET);

    private HandContext() {
    }
}
