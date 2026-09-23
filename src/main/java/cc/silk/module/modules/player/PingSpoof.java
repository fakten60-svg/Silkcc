package cc.silk.module.modules.player;


import cc.silk.event.impl.network.PacketEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class PingSpoof extends Module {
    private final NumberSetting msDelay = new NumberSetting("Ms", 1, 500, 60, 1);

    public PingSpoof() {
        super("Ping Spoof", "Increases your ping", -1, Category.PLAYER);
        this.addSetting(msDelay);
    }

    @EventHandler
    private void onEventPacket(PacketEvent event) {
        if (event.getPacket() instanceof ClientboundKeepAlivePacket packet) {
            if (isNull()) return;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(msDelay.getValueInt());
                    Objects.requireNonNull(mc.getConnection()).getConnection().send(new ServerboundKeepAlivePacket(packet.getId()));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            future.join();
        }
    }
}
