package net.sievert.jolcraft.world.item.custom.armor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.data.language.JolCraftDictionary;
import net.sievert.jolcraft.util.JolCraftStrings;
import net.sievert.jolcraft.world.entity.effect.JolCraftOwnedEffectHelper;
import net.sievert.jolcraft.world.item.equipment.JolCraftArmorItemSet;
import net.sievert.jolcraft.world.item.equipment.JolCraftEquipmentHelper;
import net.sievert.jolcraft.world.item.material.JolCraftMaterials;
import net.sievert.jolcraft.world.item.material.armor.JolCraftArmorMaterials;
import net.sievert.jolcraft.world.item.registry.JolCraftArmorItems;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ArmorSetItem extends ArmorItem {

    private static final String NBT_ARMOR_SET_EFFECTS =
            JolCraftStrings.underscored(
                    JolCraft.MOD_ID,
                    JolCraftDictionary.ARMOR,
                    JolCraftDictionary.SET,
                    JolCraftStrings.plural(JolCraftDictionary.EFFECT)
            );

    private static final boolean EFFECT_AMBIENT = false;
    private static final boolean EFFECT_PARTICLES = false;
    private static final boolean EFFECT_ICON = true;

    private static final List<JolCraftArmorItemSet> SETS =
            List.of(
                    JolCraftArmorItems.DEEPSLATE,
                    JolCraftArmorItems.MITHRIL
            );

    protected ArmorSetItem(
            Holder<ArmorMaterial> material,
            Type type,
            Properties properties
    ) {
        super(material, type, properties);
    }

    protected abstract @NotNull JolCraftMaterials.Material material();
    protected abstract @NotNull List<ArmorSetEffect> effects();

    /** The ownership id is cached: it is read for every effect on every player tick. */
    protected record ArmorSetEffect(
            Holder<MobEffect> effect,
            int amplifier,
            String id
    ) {
        protected ArmorSetEffect(
                Holder<MobEffect> effect,
                int amplifier
        ) {
            this(
                    effect,
                    amplifier,
                    effect.unwrapKey()
                            .orElseThrow()
                            .location()
                            .toString()
            );
        }
    }

    private boolean hasFullSet(Player player) {
        for (Type type : JolCraftEquipmentHelper.PLAYER_ARMOR_TYPES) {
            ItemStack stack =
                    player.getItemBySlot(type.getSlot());

            if (stack.isEmpty()) return false;

            var armorMaterial =
                    JolCraftEquipmentHelper.armorMaterial(stack);

            if (armorMaterial == null
                    || armorMaterial
                    != JolCraftArmorMaterials.armorMaterial(material())) {
                return false;
            }
        }

        return true;
    }

    /**
     * The only sync path, run from PlayerTickEvent.Post. A player tick still
     * happens when every piece of a set has left the inventory, so no
     * inventoryTick override is needed.
     */
    public static void maintainSetEffects(Player player) {
        if (player.level().isClientSide) return;
        for (JolCraftArmorItemSet set : SETS) {
            ArmorSetItem armor = (ArmorSetItem) set.helmet().get();
            armor.updateSetEffects(player, armor.hasFullSet(player));
        }
    }

    private void updateSetEffects(
            Player player,
            boolean hasFullSet
    ) {
        for (ArmorSetEffect effect : effects()) {
            JolCraftOwnedEffectHelper.syncInfinite(
                    player,
                    effect.effect(),
                    effect.amplifier(),
                    NBT_ARMOR_SET_EFFECTS,
                    effect.id(),
                    hasFullSet,
                    EFFECT_AMBIENT,
                    EFFECT_PARTICLES,
                    EFFECT_ICON
            );
        }
    }
}
