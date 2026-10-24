package net.sievert.jolcraft.datagen.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.data.LanguageProvider;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.contribute.LanguageContributor;
import net.sievert.jolcraft.module.primitive.config.ConfigKey;
import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.sound.SoundKey;
import org.jetbrains.annotations.NotNull;

/**
 * The English language file from every module's contributor, then the completeness walks: every item, block,
 * entity type, mob effect, attribute and creative tab of the mod has a name, and every setting and section of the
 * mod's config files has a label for the config screen, every sound event a subtitle, or the run fails naming the ones that do not. A registry
 * the mod starts registering into is added to the walk when that happens. The screen's frame, its title and
 * one button and title per file, is written here.
 */
public final class Translations extends LanguageProvider {

    private final List<LanguageContributor> contributors;
    private final Log log;
    private final Set<String> keys = new HashSet<>();

    public Translations(PackOutput output, List<LanguageContributor> contributors, Log log) {
        super(output, JolCraft.MOD_ID, "en_us");
        this.contributors = contributors;
        this.log = log;
    }

    @Override
    public void add(@NotNull String key, @NotNull Component value) {
        keys.add(key);
        super.add(key, value);
    }

    @Override
    protected void addTranslations() {
        contributors.forEach(contributor -> contributor.english(this));
        List<Identifier> missing = new ArrayList<>();
        requireNamed(BuiltInRegistries.ITEM, Item::getDescriptionId, missing);
        requireNamed(BuiltInRegistries.BLOCK, Block::getDescriptionId, missing);
        requireNamed(BuiltInRegistries.ENTITY_TYPE, EntityType::getDescriptionId, missing);
        requireNamed(BuiltInRegistries.MOB_EFFECT, MobEffect::getDescriptionId, missing);
        requireNamed(BuiltInRegistries.ATTRIBUTE, Attribute::getDescriptionId, missing);
        requireNamed(BuiltInRegistries.CREATIVE_MODE_TAB, Translations::tabKey, missing);
        Completeness.require("names", missing);
        List<String> unlabelled = new ArrayList<>();
        for (ModConfig file : ModConfigs.getModConfigs(JolCraft.MOD_ID)) {
            frame(file);
            if (file.getSpec() instanceof ModConfigSpec spec) {
                requireLabelled(file.getFileName(), spec, spec.getSpec(), List.of(), unlabelled);
            }
        }
        Completeness.require("config labels", unlabelled);
        Completeness.require("subtitles", Completeness.ours(BuiltInRegistries.SOUND_EVENT)
                .map(holder -> SoundKey.subtitle(Completeness.id(holder)))
                .filter(subtitle -> !keys.contains(subtitle))
                .toList());
        log.info("{} translations, every registered thing named, every setting labelled, every sound subtitled", keys.size());
    }

    private <T> void requireNamed(Registry<T> registry, java.util.function.Function<T, String> key, List<Identifier> missing) {
        Completeness.ours(registry)
                .filter(holder -> !keys.contains(key.apply(holder.value())))
                .map(Completeness::id)
                .forEach(missing::add);
    }

    /** Every setting and section of a spec has a label in the file, found by the translation keys the spec carries. */
    private void requireLabelled(String file, ModConfigSpec spec, UnmodifiableConfig level, List<String> path, List<String> unlabelled) {
        level.valueMap().forEach((name, entry) -> {
            List<String> at = Stream.concat(path.stream(), Stream.of(name)).toList();
            String key = switch (entry) {
                case ModConfigSpec.ValueSpec setting -> setting.getTranslationKey();
                case UnmodifiableConfig section -> spec.getLevelTranslationKey(at);
                default -> throw new IllegalStateException(file + " holds a " + entry.getClass().getName() + " at " + at);
            };
            if (key == null) {
                throw new IllegalStateException(at + " in " + file + " has no translation key; settings are declared through Config");
            }
            if (!keys.contains(key)) {
                unlabelled.add(key);
            }
            if (entry instanceof UnmodifiableConfig section) {
                requireLabelled(file, spec, section, at, unlabelled);
            }
        });
    }

    /**
     * The screen's own texts for a file, under the keys NeoForge's {@code ConfigurationScreen} looks up before it
     * falls back to its generic ones: the mod's title, and the file's button and title. {@code %s} is the mod's name.
     */
    private void frame(ModConfig file) {
        String kind = switch (file.getType()) {
            case COMMON -> "Common";
            case CLIENT -> "Client";
            case SERVER -> "Server";
            case STARTUP -> "Startup";
        };
        String section = JolCraft.MOD_ID + ".configuration.section."
                + file.getFileName().replaceAll("[^a-zA-Z0-9]+", ".").replaceFirst("^\\.", "").replaceFirst("\\.$", "").toLowerCase(Locale.ENGLISH);
        if (!keys.contains(JolCraft.MOD_ID + ".configuration.title")) {
            add(JolCraft.MOD_ID + ".configuration.title", "%s Configuration");
        }
        add(section, kind + " Options");
        add(section + ".title", "%s " + kind + " Configuration");
    }

    /** A tab's title is a translatable component; its key is what the language file must hold. */
    private static String tabKey(CreativeModeTab tab) {
        if (tab.getDisplayName().getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        throw new IllegalStateException("creative tab " + tab.getDisplayName().getString() + " has no translatable title");
    }

    /** The English text of one of the mod's own keys. */
    public void add(LanguageKey key, String text) {
        add(key.key(), text);
    }

    /** The subtitle of a sound. */
    public void add(SoundKey key, String subtitle) {
        add(key.subtitle(), subtitle);
    }

    /** The label of a setting on the config screen; its comment is the tooltip. */
    public void add(ConfigKey<?> key, String label) {
        add(key.translation(JolCraft.MOD_ID), label);
    }

    /** The label of a config section on the config screen: the words before a setting's name. */
    public void section(String label, String... words) {
        add(ConfigKey.translation(JolCraft.MOD_ID, words), label);
    }

    public void add(CreativeModeTab tab, String name) {
        add(tabKey(tab), name);
    }

    public void add(Attribute attribute, String name) {
        add(attribute.getDescriptionId(), name);
    }

    /** The name of a registered holder, for contributors that hold a {@code DeferredHolder}. */
    public void add(Holder<?> holder, String name) {
        Object value = holder.value();
        switch (value) {
            case Item item -> add(item, name);
            case Block block -> add(block, name);
            case EntityType<?> type -> add(type, name);
            case MobEffect effect -> add(effect, name);
            case Attribute attribute -> add(attribute, name);
            case CreativeModeTab tab -> add(tab, name);
            default -> throw new IllegalArgumentException("no translation key for " + value.getClass().getName());
        }
    }
}
