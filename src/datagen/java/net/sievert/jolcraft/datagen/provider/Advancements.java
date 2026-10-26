package net.sievert.jolcraft.datagen.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.contribute.AdvancementContributor;
import net.sievert.jolcraft.module.primitive.advancement.Advancement;
import org.jetbrains.annotations.NotNull;

/**
 * The mod's advancements from every module's contributor, then the walk: every advancement shown has the English of
 * its title and description in the language file, or the run fails naming the keys. The language file is written
 * before this provider runs, so it asks the {@link Translations} it was given.
 */
public final class Advancements extends AdvancementProvider {

    public Advancements(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, List<AdvancementContributor> contributors, Translations translations) {
        super(output, lookup, List.of(new Generator(contributors, translations)));
    }

    /** Runs the contributors through one {@link Writer} and then the walk. */
    private record Generator(List<AdvancementContributor> contributors, Translations translations) implements AdvancementSubProvider {
        @Override
        public void generate(HolderLookup.@NotNull Provider registries, @NotNull Consumer<AdvancementHolder> output) {
            Writer writer = new Writer(output, translations, registries);
            contributors.forEach(contributor -> contributor.advancements(writer));
            Completeness.require("advancement texts", writer.missing);
        }
    }

    /** What a contributor writes with: a display under the mod's text keys, and the advancement under its id. */
    public static final class Writer {
        private final Consumer<AdvancementHolder> output;
        private final List<String> missing = new ArrayList<>();
        private final Translations translations;
        private final HolderLookup.Provider registries;

        private Writer(Consumer<AdvancementHolder> output, Translations translations, HolderLookup.Provider registries) {
            this.output = output;
            this.translations = translations;
            this.registries = registries;
        }

        /** The registries at generation time, for criteria that look things up. */
        public HolderLookup.Provider registries() {
            return registries;
        }

        /** A builder showing the advancement with the icon and frame, titled and described under the keys the language contributor fills. */
        public net.minecraft.advancements.Advancement.Builder shown(Identifier id, ItemLike icon, AdvancementType frame, boolean toast, boolean announce, boolean hidden) {
            return shownRoot(id, icon, null, frame, toast, announce, hidden);
        }

        /** A builder showing a root advancement, whose tab has the background texture, titled and described like the others. */
        public net.minecraft.advancements.Advancement.Builder shownRoot(Identifier id, ItemLike icon, Identifier background, AdvancementType frame, boolean toast, boolean announce, boolean hidden) {
            return net.minecraft.advancements.Advancement.Builder.advancement()
                    .display(icon, Component.translatable(Advancement.title(id)), Component.translatable(Advancement.description(id)), background, frame, toast, announce, hidden);
        }

        /** A builder for an advancement that is never shown: a root, or a bookkeeping one. */
        public net.minecraft.advancements.Advancement.Builder hidden() {
            return net.minecraft.advancements.Advancement.Builder.advancement();
        }

        /** Writes the advancement under its id; one that is shown must be titled and described under the mod's keys, and the English must exist. */
        public AdvancementHolder add(Identifier id, net.minecraft.advancements.Advancement.Builder builder) {
            AdvancementHolder holder = builder.save(output, id.toString());
            holder.value().display().ifPresent(display -> {
                for (String key : List.of(Advancement.title(id), Advancement.description(id))) {
                    Component text = key.equals(Advancement.title(id)) ? display.getTitle() : display.getDescription();
                    if (!(text.getContents() instanceof TranslatableContents contents) || !contents.getKey().equals(key)) {
                        throw new IllegalStateException(id + " is shown under text that is not the mod's key " + key + "; build it with shown or shownRoot");
                    }
                    if (!translations.has(key)) {
                        missing.add(key);
                    }
                }
            });
            return holder;
        }
    }
}
