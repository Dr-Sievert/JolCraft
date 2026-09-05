package net.sievert.jolcraft.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.sievert.jolcraft.world.entity.JolCraftAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"
            )
    )
    private void jolcraft$modifyRegenerationExhaustion(
            FoodData foodData,
            float exhaustion,
            Player player
    ) {
        double exhaustionModifier = player.getAttributeValue(JolCraftAttributes.EXHAUSTION);

        float modifiedExhaustion = exhaustion * (1.0F + (float) exhaustionModifier);

        foodData.addExhaustion(modifiedExhaustion);
    }
}