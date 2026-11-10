package net.sievert.jolcraft.module.system.control;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.neoforge.common.Tags;

import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * One thing an entity can be kept from doing, each held by a mob effect tag of the mod's namespace: an entity with any
 * effect in the tag is stopped. The tags are empty until a feature fills them; "stunned" is all six, "rooted" is
 * {@link #MOVE}, as data.
 */
@Value
public enum Stop {
    /** A player's attack, left click and block break; a mob's melee goal while it holds a weapon. */
    ATTACK(Dictionary.ATTACK),
    /** Use of a ranged item; a mob's bow and crossbow goals. */
    RANGED(Dictionary.RANGED),
    /** Use of any other item or an empty hand; a mob's spell goal. */
    USE(Dictionary.USE),
    /**
     * Walking: a mob's navigation, strafing and carried horizontal motion, cleared each tick; a player's movement input.
     * A goal that steers a mob in the same tick (a bow goal strafing, a melee goal re-pathing) still moves it that tick;
     * a full root is the effect's own movement speed modifier, as data.
     */
    MOVE(Dictionary.MOVE),
    /** A player's mouse turning. */
    LOOK(Dictionary.LOOK),
    /** A mob's target and anger. */
    TARGET(Dictionary.TARGET);

    private final TagKey<MobEffect> tag;

    Stop(String word) {
        this.tag = Ids.tag(Registries.MOB_EFFECT, Dictionary.JOLCRAFT, word);
    }

    /** The mob effect tag whose effects stop this. */
    public TagKey<MobEffect> tag() {
        return tag;
    }

    /** What using the stack is: {@link #RANGED} for a ranged weapon tool or any projectile weapon, else {@link #USE}. */
    public static Stop of(ItemStack stack) {
        return ranged(stack) ? RANGED : USE;
    }

    static boolean ranged(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem || stack.is(Tags.Items.RANGED_WEAPON_TOOLS);
    }

    /** Whether the stack is a melee or ranged tool: what a mob needs in hand to be disarmed of its melee goal. */
    static boolean weapon(ItemStack stack) {
        return ranged(stack) || stack.is(Tags.Items.MELEE_WEAPON_TOOLS);
    }
}
