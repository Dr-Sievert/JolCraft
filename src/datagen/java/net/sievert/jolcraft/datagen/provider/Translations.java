package net.sievert.jolcraft.datagen.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import net.neoforged.neoforge.common.data.LanguageProvider;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.contribute.LanguageContributor;
import net.sievert.jolcraft.module.primitive.log.Log;

/**
 * The English language file from every module's contributor, then the completeness walk: every item, block,
 * entity type, mob effect, attribute and creative tab of the mod has a name, or the run fails naming the ones that
 * do not. A registry the mod starts registering into is added to the walk when that happens.
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
    public void add(String key, Component value) {
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
        log.info("{} translations, every registered thing named", keys.size());
    }

    private <T> void requireNamed(Registry<T> registry, java.util.function.Function<T, String> key, List<Identifier> missing) {
        Completeness.ours(registry)
                .filter(holder -> !keys.contains(key.apply(holder.value())))
                .map(Completeness::id)
                .forEach(missing::add);
    }

    /** A tab's title is a translatable component; its key is what the language file must hold. */
    private static String tabKey(CreativeModeTab tab) {
        if (tab.getDisplayName().getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        throw new IllegalStateException("creative tab " + tab.getDisplayName().getString() + " has no translatable title");
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
        if (value instanceof Item item) add(item, name);
        else if (value instanceof Block block) add(block, name);
        else if (value instanceof EntityType<?> type) add(type, name);
        else if (value instanceof MobEffect effect) add(effect, name);
        else if (value instanceof Attribute attribute) add(attribute, name);
        else if (value instanceof CreativeModeTab tab) add(tab, name);
        else throw new IllegalArgumentException("no translation key for " + value.getClass().getName());
    }
}
