package net.sievert.jolcraft.world.entity.custom.dwarf.interaction.handler.profession;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.sievert.jolcraft.world.entity.custom.dwarf.interaction.DwarfInteractionOutcome;
import net.sievert.jolcraft.world.entity.custom.dwarf.interaction.DwarfInteractions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultProfessionInteractionHandler
        implements DwarfInteractions.ProfessionInteraction {

    /**
     * Nothing profession-specific to offer. Pass, so the dispatcher can try the
     * off hand and play the single refusal grunt when neither hand is recognised.
     */
    @Override
    public DwarfInteractionOutcome handle(
            DwarfInteractions.DwarfInteractionContext ctx
    ) {
        return DwarfInteractionOutcome.pass();
    }
}