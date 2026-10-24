package net.sievert.jolcraft.gametest.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.sievert.jolcraft.gametest.scene.PacketRecorder;

/**
 * Records every packet sent to a player for {@link PacketRecorder}; dev-only, part of the test mod. The target is the
 * two-argument send, which the one-argument send delegates to; named with @Desc so javac checks the parameter types.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {

    // The IDE sees this class, not the target it is merged into, so it calls the instanceof always false.
    @SuppressWarnings({"ConstantValue", "ConstantConditions"})
    @Inject(target = @Desc(value = "send", args = {Packet.class, ChannelFutureListener.class}), at = @At("HEAD"))
    private void jolcraft$recordPacket(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if ((Object) this instanceof ServerGamePacketListenerImpl game) {
            PacketRecorder.record(game.player, packet);
        }
    }
}
