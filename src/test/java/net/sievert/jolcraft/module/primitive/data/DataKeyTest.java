package net.sievert.jolcraft.module.primitive.data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

class DataKeyTest {

    static final class Tally extends SavedData {
        static final Codec<Tally> CODEC = Codec.INT.fieldOf("count").codec().xmap(Tally::new, tally -> tally.count);
        final int count;

        Tally() {
            this(0);
        }

        Tally(int count) {
            this.count = count;
        }
    }

    private final Identifier id = Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "tally");
    private final DataKey<Tally> local = DataKey.of(id, Tally::new, Tally.CODEC);

    @Test
    void aKeyIsPerLevelUntilAskedToBeServerWide() {
        Assertions.assertEquals(id, local.id());
        Assertions.assertFalse(local.isServerWide());
        Assertions.assertTrue(local.serverWide().isServerWide());
        Assertions.assertFalse(local.isServerWide(), "the per-level key is unchanged");
    }

    @Test
    void anUndeclaredKeyRefusesToBeRead() {
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, local::type);
        Assertions.assertTrue(refused.getMessage().contains("declared"), refused.getMessage());
        Assertions.assertThrows(IllegalStateException.class, () -> local.serverWide().get((MinecraftServer) null), "undeclared, whatever the server");
    }

    @Test
    void aDeclaredKeyHasTheGamesTypeUnderItsId() {
        new ModData().declare(local);
        Assertions.assertEquals(id, local.type().id());
        Assertions.assertThrows(IllegalStateException.class, () -> local.get((MinecraftServer) null), "a per-level key needs a level, whatever the server");
    }
}
