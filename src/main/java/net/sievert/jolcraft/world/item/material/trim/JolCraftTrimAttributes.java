package net.sievert.jolcraft.world.item.material.trim;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.util.JolCraftEnumHelper;
import net.sievert.jolcraft.world.entity.JolCraftAttributes;
import net.sievert.jolcraft.world.item.component.JolCraftDataComponents;
import net.sievert.jolcraft.world.item.equipment.JolCraftEquipmentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class JolCraftTrimAttributes {

    private JolCraftTrimAttributes() {}

    private static final Map<JolCraftTrimMaterials.Attribute, List<TrimAttribute>> ATTRIBUTES = buildAttributes();

    private static Map<JolCraftTrimMaterials.Attribute, List<TrimAttribute>> buildAttributes() {
        Map<JolCraftTrimMaterials.Attribute, List<TrimAttribute>> out = new EnumMap<>(JolCraftTrimMaterials.Attribute.class);

        out.put(
                JolCraftTrimMaterials.Attribute.AEGISCORE,
                attributes(
                        new TrimAttribute(
                                Attributes.ARMOR,
                                1.25D,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        new TrimAttribute(
                                Attributes.ARMOR_TOUGHNESS,
                                0.5D,
                                AttributeModifier.Operation.ADD_VALUE
                        )
                )
        );

        out.put(JolCraftTrimMaterials.Attribute.ASHFANG,
                attributes(Attributes.ATTACK_DAMAGE, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        out.put(JolCraftTrimMaterials.Attribute.DEEPMARROW,
                attributes(JolCraftAttributes.EXPERIENCE_INCREASE, 0.125D, AttributeModifier.Operation.ADD_VALUE));

        out.put(
                JolCraftTrimMaterials.Attribute.EARTHBLOOD,
                attributes(
                        new TrimAttribute(
                                Attributes.BLOCK_INTERACTION_RANGE,
                                0.5D,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        new TrimAttribute(
                                Attributes.BLOCK_BREAK_SPEED,
                                0.05D,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ),
                        new TrimAttribute(
                                Attributes.MINING_EFFICIENCY,
                                1.0D,
                                AttributeModifier.Operation.ADD_VALUE
                        )
                )
        );

        out.put(
                JolCraftTrimMaterials.Attribute.EMBERGLASS,
                attributes(
                        new TrimAttribute(
                                Attributes.MAX_HEALTH,
                                1.0D,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        new TrimAttribute(
                                JolCraftAttributes.MAX_OVERHEAL,
                                0.05D,
                                AttributeModifier.Operation.ADD_VALUE
                        )
                )
        );

        out.put(JolCraftTrimMaterials.Attribute.FROSTVEIN,
                attributes(JolCraftAttributes.SLOW_RESISTANCE, 0.20D, AttributeModifier.Operation.ADD_VALUE));

        out.put(JolCraftTrimMaterials.Attribute.GRIMSTONE,
                attributes(Attributes.ATTACK_SPEED, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        out.put(JolCraftTrimMaterials.Attribute.IRONHEART,
                attributes(
                        new TrimAttribute(
                                Attributes.ARMOR,
                                0.05D,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        ), new TrimAttribute(
                                Attributes.ARMOR_TOUGHNESS,
                                0.05D,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        )
                )
        );

        out.put(JolCraftTrimMaterials.Attribute.LUMIERE,
                attributes(JolCraftAttributes.LUMINANCE, 1.0D, AttributeModifier.Operation.ADD_VALUE));

        out.put(JolCraftTrimMaterials.Attribute.MOONSHARD,
                attributes(JolCraftAttributes.MOON_SHIELD, 1.0D, AttributeModifier.Operation.ADD_VALUE));

        out.put(JolCraftTrimMaterials.Attribute.RUSTAGATE,
                attributes(JolCraftAttributes.ARMOR_PENETRATION, 0.20D, AttributeModifier.Operation.ADD_VALUE));

        out.put(JolCraftTrimMaterials.Attribute.SKYBURROW,
                attributes(JolCraftAttributes.ITEM_USE_SPEED, 0.20D, AttributeModifier.Operation.ADD_VALUE));

        out.put(JolCraftTrimMaterials.Attribute.SUNGLEAM,
                attributes(JolCraftAttributes.CONTAINER_LOOT_INCREASE, 0.10D, AttributeModifier.Operation.ADD_VALUE));

        out.put(JolCraftTrimMaterials.Attribute.VERDANITE,
                attributes(JolCraftAttributes.CROP_LOOT_INCREASE, 0.125D, AttributeModifier.Operation.ADD_VALUE));

        out.put(JolCraftTrimMaterials.Attribute.WOECRYSTAL,
                attributes(JolCraftAttributes.MAGIC_RESISTANCE, 0.10D, AttributeModifier.Operation.ADD_VALUE));

        return Map.copyOf(out);
    }

    /**
     * Adds the attribute trim's modifiers to the stack's computed modifiers.
     * <p>
     * The ATTRIBUTE_TRIM component is the only source of truth (TRIM stays cosmetic);
     * nothing is written to
     * {@code ATTRIBUTE_MODIFIERS}. A non-empty component replaces the item's defaults
     * (NeoForge only falls back to {@code Item#getDefaultAttributeModifiers} when the
     * component list is empty), so baking trim modifiers in there used to strip the
     * armor's own armor, toughness, knockback resistance and set speed modifiers.
     */
    public static void applyAttribute(@NotNull ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        EquipmentSlot slot = getSlotForArmor(stack);
        if (slot == null) return;

        // Must run before any trim-dependent return: legacy pieces whose trim was
        // replaced or removed still carry the baked component.
        healBakedTrimModifiers(event, stack);

        JolCraftTrimMaterials.Attribute material = getAttributeTrim(stack);
        List<TrimAttribute> attributes = material == null ? null : ATTRIBUTES.get(material);
        if (attributes == null) return;

        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
        for (int i = 0; i < attributes.size(); i++) {
            TrimAttribute attribute = attributes.get(i);

            event.replaceModifier(
                    attribute.attribute(),
                    new AttributeModifier(
                            trimModifierId(slot, i),
                            attribute.amount(),
                            attribute.operation()
                    ),
                    group
            );
        }
    }

    /**
     * Stacks trimmed before the event-based path hold a component that contains only
     * baked trim modifiers, which hides the item's defaults. Strip them and, if nothing
     * else is left, re-add the item's defaults so the piece behaves like an untrimmed one.
     */
    private static void healBakedTrimModifiers(
            @NotNull ItemAttributeModifierEvent event,
            @NotNull ItemStack stack
    ) {
        // Decide from the stack's own component, not the live builder, so entries that
        // higher-priority listeners added do not mask a trim-only component.
        boolean baked = false;
        boolean onlyBaked = true;
        for (ItemAttributeModifiers.Entry entry : event.getDefaultModifiers().modifiers()) {
            if (isTrimModifier(entry.modifier().id())) {
                baked = true;
            } else {
                onlyBaked = false;
            }
        }
        if (!baked) return;

        event.removeIf(entry -> isTrimModifier(entry.modifier().id()));
        if (!onlyBaked) return;

        ItemAttributeModifiers defaults = stack.getItem().getDefaultAttributeModifiers(stack);
        for (ItemAttributeModifiers.Entry entry : defaults.modifiers()) {
            event.addModifier(entry.attribute(), entry.modifier(), entry.slot());
        }
    }

    /**
     * The attribute trim on a stack: the ATTRIBUTE_TRIM component or, for pieces trimmed
     * before that component existed, the TRIM material itself.
     */
    public static @Nullable JolCraftTrimMaterials.Attribute getAttributeTrim(@NotNull ItemStack stack) {
        JolCraftTrimMaterials.Attribute attribute = stack.get(JolCraftDataComponents.ATTRIBUTE_TRIM);
        if (attribute != null) return attribute;

        ArmorTrim trim = stack.get(DataComponents.TRIM);
        return trim == null ? null : getAttributeTrim(trim);
    }

    public static @Nullable JolCraftTrimMaterials.Attribute getAttributeTrim(@NotNull ArmorTrim trim) {
        ResourceLocation id = trim.material().unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);

        if (id == null || !id.getNamespace().equals(JolCraft.MOD_ID)) return null;

        return JolCraftEnumHelper.byStringIdNullable(
                JolCraftTrimMaterials.Attribute.class,
                id.getPath(),
                null
        );
    }

    public static List<TrimAttribute> getTrimAttributes(@NotNull JolCraftTrimMaterials.Attribute trim) {
        List<TrimAttribute> attributes = ATTRIBUTES.get(trim);

        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalStateException("Missing attribute definition for: " + trim);
        }

        return attributes;
    }

    private static boolean isTrimModifier(@NotNull ResourceLocation id) {
        if (!id.getNamespace().equals(JolCraft.MOD_ID)) return false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String prefix = "attribute_trim_" + slot.getName() + "_";
            if (id.getPath().startsWith(prefix)) {
                String index = id.getPath().substring(prefix.length());
                return !index.isEmpty() && index.chars().allMatch(Character::isDigit);
            }
        }

        return false;
    }

    private static @NotNull ResourceLocation trimModifierId(
            @NotNull EquipmentSlot slot,
            int index
    ) {
        return JolCraft.location("attribute_trim_" + slot.getName() + "_" + index);
    }

    public static EquipmentSlot getSlotForArmor(ItemStack stack) {
        ArmorItem.Type type = JolCraftEquipmentHelper.armorType(stack);
        return type == null ? null : type.getSlot();
    }

    private static List<TrimAttribute> attributes(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return List.of(new TrimAttribute(attribute, amount, operation));
    }

    private static List<TrimAttribute> attributes(TrimAttribute... attributes) {
        return List.of(attributes);
    }

    public record TrimAttribute(
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {}
}
