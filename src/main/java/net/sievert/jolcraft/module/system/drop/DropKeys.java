package net.sievert.jolcraft.module.system.drop;

import net.sievert.jolcraft.module.primitive.loot.LootKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Keys;
import net.sievert.jolcraft.util.text.Dictionary;

/** The drop system's handles: its loot modifier kind, which {@link Drop} declares to {@code Loot}. */
@Keys
public final class DropKeys {

    /** The kind {@code jolcraft:replace_table}, beside NeoForge's {@code add_table}. */
    public static final LootKey<ReplaceTableModifier> REPLACE_TABLE = LootKey.modifier(Ids.of(Dictionary.JOLCRAFT, Dictionary.REPLACE, Dictionary.TABLE), ReplaceTableModifier.CODEC);

    private DropKeys() {
    }
}
