package cc.silk.utils.mc;

import cc.silk.utils.IMinecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class CombatUtil implements IMinecraft {
    public static boolean isShieldFacingAway(final LivingEntity en) {

        if (en == null) return true;
        if (!(en instanceof net.minecraft.world.entity.player.Player)) return true;
        if (mc.player == null) return false;

        Vec3 toLocal = mc.player.position().subtract(en.position());
        if (toLocal.lengthSqr() == 0) return true;
        toLocal = toLocal.normalize();

        final double yaw = Math.toRadians(en.getYRot());
        final double pitch = Math.toRadians(en.getXRot());

        Vec3 facing = new Vec3(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)
        ).normalize();

        double dot = facing.dot(toLocal);
        return dot < -0.06;
    }
}
