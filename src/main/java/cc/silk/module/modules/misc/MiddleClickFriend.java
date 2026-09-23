package cc.silk.module.modules.misc;

import cc.silk.event.impl.input.MouseClickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.friend.FriendManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public final class MiddleClickFriend extends Module {

    public MiddleClickFriend() {
        super("Middle Click Friend", "Middle click on players to add/remove them from friends list", -1, Category.MISC);
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && event.action() == GLFW.GLFW_PRESS) {
            if (isNull()) return;

            HitResult hitResult = mc.hitResult;
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHitResult = (EntityHitResult) hitResult;
                if (entityHitResult.getEntity() instanceof Player player) {
                    if (player == mc.player) return;

                    FriendManager.toggleFriend(player.getUUID());

                    if (FriendManager.isFriend(player.getUUID())) {
                        mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a" + player.getName().getString() + " added to friends"));
                    } else {
                        mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c" + player.getName().getString() + " removed from friends"));
                    }
                }
            }
        }
    }
}