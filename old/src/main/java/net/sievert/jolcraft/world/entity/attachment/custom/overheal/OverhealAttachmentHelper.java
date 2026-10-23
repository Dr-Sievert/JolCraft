package net.sievert.jolcraft.world.entity.attachment.custom.overheal;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.sievert.jolcraft.world.entity.JolCraftAttributes;
import net.sievert.jolcraft.world.entity.attachment.JolCraftAttachments;

public final class OverhealAttachmentHelper {

    private OverhealAttachmentHelper() {}

    /** Reads without creating: getData would materialise, sync and save a zero attachment. */
    public static float getAmount(LivingEntity entity) {
        Float amount = entity.getExistingDataOrNull(JolCraftAttachments.OVERHEAL);
        return amount == null ? 0.0F : amount;
    }

    public static float getMaxAmount(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(JolCraftAttributes.MAX_OVERHEAL);

        if (attribute == null) {
            return 0.0F;
        }

        return entity.getMaxHealth() * (float) attribute.getValue();
    }

    public static void setAmount(LivingEntity entity, float amount) {
        float clamped = Mth.clamp(amount, 0.0F, getMaxAmount(entity));

        if (Float.compare(getAmount(entity), clamped) != 0) {
            entity.setData(JolCraftAttachments.OVERHEAL, clamped);
        }
    }

    public static void addAmount(LivingEntity entity, float amount) {
        setAmount(entity, getAmount(entity) + amount);
    }

    public static void clear(LivingEntity entity) {
        setAmount(entity, 0.0F);
    }
}