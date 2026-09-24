package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.event.impl.network.DisconnectEvent;
import cc.silk.event.impl.network.PacketEvent;
import cc.silk.event.types.TransferOrder;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinClientConnection {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void receivePacketEventInject(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        postPacketEvent(packet, TransferOrder.RECEIVE, ci);
    }

    @Unique
    private void postPacketEvent(Packet<?> packet, TransferOrder order, CallbackInfo ci) {
        if (SilkClient.INSTANCE == null) return;
        PacketEvent eventPacket = new PacketEvent(packet, order);
        SilkClient.INSTANCE.getSilkEventBus().post(eventPacket);
        if (eventPacket.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void sendPacketEventInject(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        postPacketEvent(packet, TransferOrder.SEND, ci);
    }

    @Inject(method = "handleDisconnection", at = @At("HEAD"))
    private void handleDisconnectionInject(CallbackInfo ci) {
        if (SilkClient.INSTANCE != null) {
            SilkClient.INSTANCE.getSilkEventBus().post(new DisconnectEvent());
        }
    }
}
