package net.sievert.jolcraft.world.item.material;

import com.google.common.base.Suppliers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.data.JolCraftTags;
import net.sievert.jolcraft.data.id.item.JolCraftMaterialIds;
import net.sievert.jolcraft.data.language.JolCraftDictionary;
import net.sievert.jolcraft.util.JolCraftEnumHelper;
import net.sievert.jolcraft.util.JolCraftStrings;
import net.sievert.jolcraft.world.item.JolCraftItems;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public final class JolCraftMaterials {

    private JolCraftMaterials() {}

    /**
     * An equipment material.
     *
     * <p>Every material owns the item that repairs its gear. Its repair tag
     * ({@code jolcraft:repairs_<id>}) is derived from the material, the tool tier and
     * armor material read their repair ingredient from it, and the item tag provider
     * fills it from {@link #repairItem()}. Adding a material therefore cannot leave an
     * empty repair tag behind.
     */
    public enum Material implements JolCraftEnumHelper.StringId {
        DEEPSLATE(JolCraftMaterialIds.DEEPSLATE, () -> JolCraftItems.DEEPSLATE_PLATE.get()),
        MITHRIL(JolCraftMaterialIds.MITHRIL, () -> JolCraftItems.MITHRIL_INGOT.get());

        private final String id;
        private final Supplier<Item> repairItem;
        private final TagKey<Item> repairTag;
        private final Supplier<Ingredient> repairIngredient;

        Material(@NotNull String id, @NotNull Supplier<Item> repairItem) {
            this.id = id;
            this.repairItem = repairItem;
            this.repairTag = JolCraftTags.Items.repairs(id);
            this.repairIngredient = Suppliers.memoize(() -> Ingredient.of(this.repairTag));
        }

        @Override
        public @NotNull String getId() {
            return this.id;
        }

        /** The item that repairs this material's gear. Items register after this enum loads, so it stays a supplier. */
        public @NotNull Supplier<Item> repairItem() {
            return this.repairItem;
        }

        /** {@code jolcraft:repairs_<id>}. The item tag provider fills it with {@link #repairItem()}. */
        public @NotNull TagKey<Item> repairTag() {
            return this.repairTag;
        }

        /** Repair ingredient for the tool tier and armor material, backed by {@link #repairTag()}. */
        public @NotNull Ingredient repairIngredient() {
            return this.repairIngredient.get();
        }

        public @NotNull ResourceKey<TrimMaterial> trimKey() {
            return ResourceKey.create(Registries.TRIM_MATERIAL, JolCraft.location(this.id));
        }

        public @NotNull String trimAssetName() {
            return this.id;
        }

        public @NotNull Map<Holder<ArmorMaterial>, String> overrideArmorMaterials() {
            return Map.of();
        }

        public @NotNull String darkerTrimName() {
            return JolCraftStrings.underscored(this.id, JolCraftDictionary.DARKER);
        }
    }
}
