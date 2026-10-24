package net.sievert.jolcraft.datagen.provider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.contribute.SoundContributor;
import net.sievert.jolcraft.module.primitive.log.Log;

/** sounds.json from every module's contributor, then the walk: every sound event of the mod has a definition. No exemption: the file is generated whole. */
public final class Sounds extends SoundDefinitionsProvider {

    private final List<SoundContributor> contributors;
    private final Log log;
    private final Set<Identifier> defined = new HashSet<>();

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

    /** A sound event's definition; the one way contributors define sounds, so the walk sees every definition. */
    @Override
    public void add(SoundEvent soundEvent, SoundDefinition definition) {
        defined.add(BuiltInRegistries.SOUND_EVENT.getKey(soundEvent));
        super.add(soundEvent, definition);
    }

    public static SoundDefinition define() {
        return definition();
    }

    public static SoundDefinition.Sound file(Identifier name) {
        return sound(name);
    }
}
