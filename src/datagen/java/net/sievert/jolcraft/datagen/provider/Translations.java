package net.sievert.jolcraft.datagen.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.data.LanguageProvider;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.contribute.LanguageContributor;
import net.sievert.jolcraft.module.primitive.config.ConfigKey;
import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.sound.SoundKey;
import net.sievert.jolcraft.module.primitive.stat.Stat;
import net.sievert.jolcraft.module.primitive.stat.StatKey;
import net.sievert.jolcraft.stereotype.Value;
import org.jetbrains.annotations.NotNull;

/**
 * The English language file from every module's contributor, then the completeness walks: every item, block,
 * entity type, mob effect, attribute and creative tab of the mod has a name, and every setting and section of the
 * mod's config files has a label for the config screen, every sound event a subtitle, every statistic a name, every language key a module
 * declares in a {@code @Value} class its text, or the run fails naming the ones that do not. A registry
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
    public void add(@NotNull String key, @NotNull Component text) {
        keys.add(key);
        super.add(key, text);
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
        Completeness.require("texts of language keys", languageKeysOfTheMod().stream().map(LanguageKey::key).filter(key -> !keys.contains(key)).toList());
        Completeness.require("statistic names", Completeness.ours(BuiltInRegistries.CUSTOM_STAT)
                .map(holder -> Stat.name(Completeness.id(holder)))
                .filter(name -> !keys.contains(name))
                .toList());
        Completeness.require("subtitles", Completeness.ours(BuiltInRegistries.SOUND_EVENT)
                .map(holder -> SoundKey.subtitle(Completeness.id(holder)))
                .filter(subtitle -> !keys.contains(subtitle))
                .toList());
        log.info("{} translations, every registered thing named, every setting labelled, every sound subtitled, every statistic named, every key given its text", keys.size());
    }

    private <T> void requireNamed(Registry<T> registry, java.util.function.Function<T, String> key, List<Identifier> missing) {
        Completeness.ours(registry)
                .filter(holder -> !keys.contains(key.apply(holder.value())))
                .map(Completeness::id)
                .forEach(missing::add);
    }

    /** Every setting and section of a spec has a label in the file, found by the translation keys the spec carries. */
    @SuppressWarnings("deprecation")
    private void requireLabelled(String file, ModConfigSpec spec, UnmodifiableConfig level, List<String> path, List<String> unlabelled) {
        level.valueMap().forEach((name, entry) -> {
            List<String> at = Stream.concat(path.stream(), Stream.of(name)).toList();
            String key = switch (entry) {
                case ModConfigSpec.ValueSpec setting -> setting.getTranslationKey();
                case UnmodifiableConfig _ -> spec.getLevelTranslationKey(at);
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

    /** Every {@code LanguageKey} constant the mod declares in a {@code @Value} class, found through the mod's scan data. */
    private static List<LanguageKey> languageKeysOfTheMod() {
        List<LanguageKey> found = new ArrayList<>();
        ModList.get().getModFileById(JolCraft.MOD_ID).getFile().getScanResult().getAnnotatedBy(Value.class, ElementType.TYPE).forEach(data -> {
            try {
                Class<?> type = Class.forName(data.clazz().getClassName(), true, Translations.class.getClassLoader());
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType() == LanguageKey.class) {
                        field.setAccessible(true);
                        found.add((LanguageKey) field.get(null));
                    }
                }
            } catch (ReflectiveOperationException unreadable) {
                throw new IllegalStateException("the value class " + data.clazz().getClassName() + " could not be read for its language keys", unreadable);
            }
        });
        return found;
    }

    /** Whether the English of a key was written; for the providers that run after this one and need to know. */
    public boolean has(String key) {
        return keys.contains(key);
    }

    /** The name of a statistic. */
    public void add(StatKey key, String name) {
        add(key.name(), name);
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
        Object held = holder.value();
        switch (held) {
            case Item item -> add(item, name);
            case Block block -> add(block, name);
            case EntityType<?> type -> add(type, name);
            case MobEffect effect -> add(effect, name);
            case Attribute attribute -> add(attribute, name);
            case CreativeModeTab tab -> add(tab, name);
            default -> throw new IllegalArgumentException("no translation key for " + held.getClass().getName());
        }
    }
}
