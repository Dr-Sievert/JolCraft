package net.sievert.jolcraft.module.primitive.sound;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping; the game's registries are already frozen when these run, so scenes in the game-test mod play real sounds to real players. */
class ModSoundTest {

    private final SoundKey yes = SoundKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "dwarf_yes"));
    private final SoundKey no = SoundKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "dwarf_no"));
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void keysAreCountedOncePerId() {
        ModSound sound = new ModSound();
        sound.declare(yes);
        sound.declare(no);
        Assertions.assertEquals(2, sound.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> sound.declare(yes.heardWithin(16)), "same id, another shape");
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModSound().declare(SoundKey.of(Identifier.withDefaultNamespace("dwarf_yes"))));
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModSound sound = new ModSound();
        sound.declare(yes);
        sound.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> sound.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> sound.declare(no));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.SOUND, new ModSound().id());
    }
}
