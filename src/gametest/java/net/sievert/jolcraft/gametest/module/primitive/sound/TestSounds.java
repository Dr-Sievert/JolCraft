package net.sievert.jolcraft.gametest.module.primitive.sound;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.primitive.sound.Sound;
import net.sievert.jolcraft.module.primitive.sound.SoundKey;

/** The sounds the sound scenes play; the game-test mod declares them in its root. No file is needed to send them. */
public final class TestSounds {

    public static final SoundKey PING = SoundKey.of(Ids.of(JolCraftGameTests.MOD_ID, "ping"));
    public static final SoundKey HORN = SoundKey.of(Ids.of(JolCraftGameTests.MOD_ID, "horn")).heardWithin(64);

    private TestSounds() {
    }

    public static void declare(Sound sound) {
        sound.declare(PING);
        sound.declare(HORN);
    }
}
