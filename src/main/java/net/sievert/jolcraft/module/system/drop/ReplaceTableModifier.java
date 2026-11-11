package net.sievert.jolcraft.module.system.drop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The loot modifier {@code jolcraft:replace_table}: when its conditions hold, what the table rolled is replaced by a roll
 * of another table. NeoForge's {@code add_table} with a clear first; the data names the table by key, as there.
 * <ul>
 *   <li>A table the data pack lacks leaves the drop as rolled: nothing is cleared and nothing added.</li>
 *   <li>The table is rolled raw, through the game's stack splitter, with the same context, as {@code add_table} rolls its
 *   own. The queried table id is set once per roll, so a roll that applied the modifiers would meet this one again, its
 *   conditions still holding, without end.</li>
 *   <li>The roll goes into a list of its own before the drop is cleared. NeoForge catches a modifier's exception and goes
 *   on with the list as the modifier left it, so a roll that throws leaves the drop as it was.</li>
 *   <li>The priority is NeoForge's default unless the data states one; the modifiers after this one see the replacement
 *   as the drop.</li>
 * </ul>
 * Design in design/drop.md.
 */
@Actor
public final class ReplaceTableModifier extends LootModifier {

    public static final MapCodec<ReplaceTableModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(ResourceKey.codec(Registries.LOOT_TABLE).fieldOf(Dictionary.TABLE).forGetter(ReplaceTableModifier::table))
            .apply(instance, ReplaceTableModifier::new));

    private final ResourceKey<LootTable> table;

    /** A replace at NeoForge's default priority. */
    public ReplaceTableModifier(LootItemCondition[] conditions, ResourceKey<LootTable> table) {
        this(conditions, IGlobalLootModifier.DEFAULT_PRIORITY, table);
    }

    /** A replace at a priority of its own; a higher one runs earlier. */
    public ReplaceTableModifier(LootItemCondition[] conditions, int priority, ResourceKey<LootTable> table) {
        super(conditions, priority);
        this.table = table;
    }

    /** The table rolled in place of the drop. */
    public ResourceKey<LootTable> table() {
        return table;
    }

    @SuppressWarnings("deprecation") // the raw roll is the point: the table's own modifiers must not run, see the class doc
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        return context.getResolver().lookupOrThrow(Registries.LOOT_TABLE).get(table).map(found -> {
            ObjectArrayList<ItemStack> rolled = new ObjectArrayList<>();
            found.value().getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), rolled::add));
            generatedLoot.clear();
            generatedLoot.addAll(rolled);
            return generatedLoot;
        }).orElse(generatedLoot);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return DropKeys.REPLACE_TABLE.codec();
    }
}
