package cc.silk.module.modules.client;

import cc.silk.event.impl.network.PacketEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.mc.ChatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;

public class Debugger extends Module {
    public Debugger() {
        super("Debugger", "Debugs inv packets (dev purposes)", -1, Category.CLIENT);
    }

    @EventHandler
    public void onPacketSend(PacketEvent e) {
        if (isNull()) return;
        if (e.getPacket() == null) return;
        if (!(e.getPacket() instanceof ServerboundContainerClickPacket packet))
            return;

        ChatUtil.addChatMessage("""
                ClickSlotPacket
                  containerId: %s
                  revision: %s
                  slot: %s
                  button: %s
                  actionType: %s
                  modifiedItems: %s
                  stack: %s
                """.formatted(
                packet.containerId(),
                packet.stateId(),
                packet.slotNum(),
                packet.buttonNum(),
                packet.containerInput().toString(),
                packet.changedSlots(),
                packet.carriedItem()
        ));
    }
}
