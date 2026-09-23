package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.AttackEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class Criticals extends Module {
    public final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Watchdog Old", "Mospixel");

    public Criticals() {
        super("Criticals", "Makes you hit every crit (BLATANT)", -1, Category.COMBAT);
        this.addSetting(mode);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (isNull()) return;
        boolean willCritLegit = mc.player.fallDistance > 0.0F && !mc.player.onGround() && !mc.player.onClimbable() && !mc.player.isInWater() && !mc.player.hasEffect(MobEffects.BLINDNESS) && !mc.player.isPassenger() && e.getTarget() instanceof LivingEntity;
        if (willCritLegit) return;

        switch (mode.getMode()) {
            case "Vanilla" -> {
                mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.position().add(0, 0.2, 0), mc.player.getYRot(), mc.player.getXRot(), false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.position().add(0, 0.1, 0), mc.player.getYRot(), mc.player.getXRot(), false, false));
            }
            case "Watchdog Old" -> {
                if (mc.player.onGround()) {
                    mc.player.setPos(mc.player.getX(), mc.player.getY() + 0.001D, mc.player.getZ());
                }
            }
            case "Mospixel" -> {
                mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.position().add(0, 0.000000271875, 0), mc.player.getYRot(), mc.player.getXRot(), false, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.position(), mc.player.getYRot(), mc.player.getXRot(), false, false));
            }
        }
    }
}
