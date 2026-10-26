package net.sievert.jolcraft.datagen.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.contribute.SoundContributor;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.sound.SoundKey;

/**
 * sounds.json from every module's contributor, then the walk: every sound event of the mod has a definition. No exemption: the
 * file is generated whole. Every definition carries the subtitle of its sound, whichever way it is added; the English of the
 * subtitle is the module's language contributor's, and {@code Translations} demands it.
 */
public final class Sounds extends SoundDefinitionsProvider {

    private final List<SoundContributor> contributors;
    private final Log log;
    private final Set<Identifier> defined = new HashSet<>();
    private final Set<SoundDefinition> definitions = Collections.newSetFromMap(new IdentityHashMap<>());

    public Sounds(PackOutput output, List<SoundContributor> contributors, Log log) {
        super(output, JolCraft.MOD_ID);
        this.contributors = contributors;
        this.log = log;
    }

    @Override
    public void registerSounds() {
        contributors.forEach(contributor -> contributor.sounds(this));
        List<Identifier> missing = Completeness.ours(BuiltInRegistries.SOUND_EVENT)
                .map(Completeness::id)
                .filter(id -> !defined.contains(id))
                .toList();
        Completeness.require("sound definitions", missing);
        log.info("{} sound definitions, every sound event defined", defined.size());
    }

    /** A sound event's definition, given its subtitle; both ways of adding end here, so the walk sees every definition and every one is subtitled. */
    @Override
    public void add(@NotNull SoundEvent soundEvent, @NotNull SoundDefinition definition) {
        Identifier id = BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);
        if (!definitions.add(definition)) {
            throw new IllegalStateException(id + " was given a definition already used for another sound; every sound needs a definition of its own");
        }
        defined.add(id);
        super.add(soundEvent, definition.subtitle(SoundKey.subtitle(id)));
    }

    /** A key's definition. */
    public void add(SoundKey key, SoundDefinition definition) {
        add(key.event(), definition);
    }

    public static SoundDefinition define() {
        return definition();
    }

    public static SoundDefinition.Sound file(Identifier name) {
        return sound(name);
    }
}
