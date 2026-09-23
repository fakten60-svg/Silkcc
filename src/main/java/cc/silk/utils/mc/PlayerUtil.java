package cc.silk.utils.mc;

import cc.silk.utils.IMinecraft;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@UtilityClass
@Getter
public final class PlayerUtil implements IMinecraft {
    private int offGroundTicks = 0;
    private int groundTicks = 0;

    public int getOffGroundTicks() {
        assert mc.player != null;
        if (mc.player.onGround()) {
            groundTicks++;
            offGroundTicks = 0;
        } else {
            groundTicks = 0;
            offGroundTicks++;
        }
        return offGroundTicks;
    }

    public static boolean isLookingAt(BlockPos pos, double maxDistance) {
        if (mc.player == null || mc.level == null) return false;

        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 reachVec = eyePos.add(lookVec.scale(maxDistance));

        BlockHitResult result = mc.level.clip(new net.minecraft.world.level.ClipContext(
                eyePos,
                reachVec,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                mc.player
        ));

        return result != null && result.getBlockPos().equals(pos);
    }
}
