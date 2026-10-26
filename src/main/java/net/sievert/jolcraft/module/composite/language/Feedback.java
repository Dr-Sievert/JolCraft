package net.sievert.jolcraft.module.composite.language;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import net.sievert.jolcraft.module.Composite;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.language.Language;
import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.sound.SoundKey;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Telling a player something, on the server: a line in chat or over the hotbar, with a sound for that player alone
 * when the moment has one. Built from {@code language} and {@code log}; the sound comes as a key the caller holds,
 * which is no dependency on the sound primitive, and plays for the player and nobody else. Every feature that
 * answers a player goes through here, so the shape of an answer is one.
 */
@Composite(Dictionary.FEEDBACK)
public final class Feedback implements Module {

    private final Language language;
    private final Log log;

    public Feedback(Language language, Log log) {
        this.language = language;
        this.log = log.tagged(this);
    }

    /** A line in the player's chat. */
    public void tell(ServerPlayer player, LanguageKey key, Object... arguments) {
        player.sendSystemMessage(language.text(key, arguments));
        log.debug("told {} {}", player.getName().getString(), key);
    }

    /** A line in the player's chat, with a sound for the player. */
    public void tell(ServerPlayer player, SoundKey sound, LanguageKey key, Object... arguments) {
        tell(player, key, arguments);
        sound.playFor(player, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** A line over the player's hotbar, which fades; for what should not stay in the chat log. */
    public void show(ServerPlayer player, LanguageKey key, Object... arguments) {
        player.sendOverlayMessage(language.text(key, arguments));
    }

    /** A line over the player's hotbar, with a sound for the player. */
    public void show(ServerPlayer player, SoundKey sound, LanguageKey key, Object... arguments) {
        show(player, key, arguments);
        sound.playFor(player, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
