package net.sievert.jolcraft.world.entity.custom.dwarf.action.type.reputation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.sievert.jolcraft.world.entity.player.advancement.JolCraftCriteriaTriggers;
import net.sievert.jolcraft.world.entity.attachment.JolCraftAttachments;
import net.sievert.jolcraft.world.item.component.JolCraftDataComponents;
import net.sievert.jolcraft.world.entity.attachment.player.custom.reputation.DwarvenReputationAttachmentHelper;
import net.sievert.jolcraft.util.log.JolCraftLogTags;
import net.sievert.jolcraft.util.log.JolCraftLogs;
import net.sievert.jolcraft.world.entity.custom.dwarf.base.AbstractDwarfEntity;
import net.sievert.jolcraft.world.entity.custom.dwarf.action.DwarfActionType;
import net.sievert.jolcraft.world.entity.custom.dwarf.action.type.InspectDwarfAction;
import net.sievert.jolcraft.world.entity.custom.dwarf.profession.DwarfProfession;
import net.sievert.jolcraft.world.sound.util.JolCraftSoundHelper;

public class EndorseDwarfAction extends InspectDwarfAction {

    public int ticksRemaining = 0;
    protected ItemStack tablet = ItemStack.EMPTY;

    public EndorseDwarfAction(AbstractDwarfEntity dwarf, Player player, InteractionHand hand, ItemStack itemstack) {
        super(dwarf, player, hand, itemstack);
    }

    @Override
    public DwarfActionType.Subtype getSubtype() {return DwarfActionType.Subtype.ENDORSE;}

    @Override
    public void start() {
        this.ticksRemaining = 40;
        tablet = itemstack.copy();
        startInspect(dwarf, player, hand, itemstack);
    }

    @Override
    public void tick() {
        if (ticksRemaining > 0) ticksRemaining--;
        if (ticksRemaining == 25 || ticksRemaining == 15) {
            JolCraftSoundHelper.entity(dwarf, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.2F, 0.6F);
        }
    }

    @Override
    public boolean isStopped() {
        return ticksRemaining <= 0;
    }

    @Override
    public void stop() {
        if (dwarf.level().isClientSide) return;

        // The player may have logged out or respawned since start(); never reward a stale instance.
        ServerPlayer target = resolvePlayer();
        if (target == null) {
            dwarf.getActionHelper().rollback(dwarf, this);
            tablet = ItemStack.EMPTY;
            return;
        }

        // Charge only a completed endorsement; an interrupted one keeps its payment.
        dwarf.resetPaid();

        DwarfProfession profession = dwarf.getProfession();

        DwarvenReputationAttachmentHelper.addEndorsement(target, profession);

        int total = target.getData(JolCraftAttachments.DWARVEN_REPUTATION.get()).getEndorsementCount();

        JolCraftLogs.info(
                JolCraftLogTags.PLAYER,
                "{} at {} in {} endorsed {}, they now have {} {}",
                DwarfProfession.getDisplayName(dwarf).getString(),
                JolCraftLogs.roundedPos(dwarf),
                dwarf.level().dimension().location(),
                target.getDisplayName().getString(),
                total,
                total <= 1 ? "endorsement" : "endorsements"
        );

        JolCraftCriteriaTriggers.ENDORSEMENT_GAIN.trigger(target, profession);

        ItemStack updatedTablet = tablet;

        int endorsements = DwarvenReputationAttachmentHelper.getEndorsementCount(target);
        int tier = DwarvenReputationAttachmentHelper.getTier(target);

        updatedTablet.set(JolCraftDataComponents.REPUTATION_ENDORSEMENTS.get(), endorsements);
        updatedTablet.set(JolCraftDataComponents.REPUTATION_TIER.get(), tier);
        updatedTablet.set(JolCraftDataComponents.REPUTATION_OWNER.get(), target.getName().getString());

        throwItem(dwarf, target, updatedTablet);
        tablet = ItemStack.EMPTY;
    }

}
