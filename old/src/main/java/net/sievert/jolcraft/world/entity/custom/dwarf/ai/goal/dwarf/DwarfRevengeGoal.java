package net.sievert.jolcraft.world.entity.custom.dwarf.ai.goal.dwarf;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.sievert.jolcraft.world.entity.custom.dwarf.base.AbstractDwarfEntity;
import net.sievert.jolcraft.world.item.equipment.JolCraftEquipmentHelper;

import java.util.EnumSet;
import java.util.Objects;


public class DwarfRevengeGoal extends Goal
{
    private final AbstractDwarfEntity dwarf;

    public DwarfRevengeGoal(AbstractDwarfEntity entity)
    {
        this.dwarf = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Only a living, non-creative player attacker is worth a grudge. Mobs are
     * DwarfAttackGoal's job; holding MOVE and LOOK for one it cannot reach
     * just froze the dwarf.
     */
    @Override
    public boolean canUse()
    {
        return this.dwarf.getLastHurtByMob() instanceof Player player
                && player.isAlive()
                && !player.isCreative()
                && this.dwarf.distanceTo(player) <= 10.0F;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void tick() {
        LivingEntity revengeTarget = this.dwarf.getLastHurtByMob();
        if (revengeTarget != null && this.dwarf.getTradingPlayer() == null && revengeTarget instanceof Player) {
            this.dwarf.getLookControl().setLookAt(revengeTarget, 10.0F, (float) this.dwarf.getHeadRotSpeed());
            if (this.dwarf.distanceTo(revengeTarget) >= 1.5D) {
                this.dwarf.getNavigation().moveTo(revengeTarget, 1.3F);
            } else if (!JolCraftEquipmentHelper.isMeleeBlocked(this.dwarf)) {
                revengeTarget.hurt(this.dwarf.damageSources().mobAttack(this.dwarf), (float) Objects.requireNonNull(this.dwarf.getAttribute(Attributes.ATTACK_DAMAGE)).getValue());
                DwarfAttackGoal.chooseAndSetAttackAction(dwarf, dwarf.getActionHelper());
                this.dwarf.setLastHurtByMob(null);
            }
        }
    }

    @Override
    public boolean canContinueToUse()
    {
        return this.dwarf.getLastHurtByMob() instanceof Player player
                && player.isAlive()
                && this.dwarf.distanceTo(player) <= 10.0F
                && this.dwarf.getTradingPlayer() == null;
    }

    @Override
    public void stop()
    {
        // No stopAction here: it cut the attack animation this goal had just started
        // and rolled back any inspect transaction in progress. Attack actions end on
        // their own after their few ticks.
        this.dwarf.setLastHurtByMob(null);
    }
}