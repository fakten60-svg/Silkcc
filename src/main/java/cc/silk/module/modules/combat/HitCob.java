package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.AttackEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class HitCob extends Module {
    private static final double SPEED_STOPPED = 0.05;
    private static final double SPEED_WALKING = 0.13;
    private static final double SPEED_SPRINTING = 0.26;

    public HitCob() {
        super("Hit Cob", "Places cobweb at player feet when you hit them (DO NOT USE ITS BEING TESTED)", Category.COMBAT);
    }

    @EventHandler
    private void onAttack(AttackEvent event) {
        if (isNull() || !(event.getTarget() instanceof Player target)) return;

        int webSlot = findCobwebInHotbar();
        if (webSlot == -1) return;

        BlockPos feetPos = target.blockPosition();

        if (mc.level.getBlockState(feetPos).getBlock() == Blocks.COBWEB) return;
        if (mc.level.getBlockState(feetPos).getBlock() == Blocks.WATER) return;

        double distance = mc.player.position().distanceTo(Vec3.atCenterOf(feetPos));
        if (distance > 4.5) return;

        double targetSpeed = Math.hypot(target.getDeltaMovement().x, target.getDeltaMovement().z);

        Vec3 knockbackDirection = target.position().subtract(mc.player.position()).normalize();

        double knockbackDistance;
        if (targetSpeed < SPEED_STOPPED) {
            knockbackDistance = mc.player.isSprinting() ? 1.8 : 1.2;
        } else if (targetSpeed < SPEED_WALKING) {
            knockbackDistance = mc.player.isSprinting() ? 1.5 : 1.0;
        } else {
            knockbackDistance = mc.player.isSprinting() ? 1.2 : 0.8;
        }

        Vec3 predictedPos = target.position().add(knockbackDirection.scale(knockbackDistance));
        BlockPos predictedFeet = BlockPos.containing(predictedPos.x, predictedPos.y, predictedPos.z);

        if (mc.level.getBlockState(predictedFeet).getBlock() == Blocks.COBWEB) return;
        if (mc.level.getBlockState(predictedFeet).getBlock() == Blocks.WATER) return;

        if (!mc.level.getBlockState(predictedFeet).isAir()) {
            predictedFeet = feetPos;
        }

        BlockPos groundPos = predictedFeet.below();
        if (mc.level.getBlockState(groundPos).isAir()) return;

        if (mc.player.position().distanceTo(Vec3.atCenterOf(predictedFeet)) > 4.5) return;

        int originalSlot = mc.player.getInventory().getSelectedSlot();
        float originalYaw = mc.player.getYRot();
        float originalPitch = mc.player.getXRot();

        Vec3 hitVec = Vec3.atCenterOf(groundPos).add(0, 0.5, 0);
        float[] rotation = calculateRotation(hitVec);

        mc.player.setYRot(rotation[0]);
        mc.player.setXRot(rotation[1]);
        mc.player.getInventory().setSelectedSlot(webSlot);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                Direction.UP,
                groundPos,
                false
        );

        if (mc.gameMode != null) {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        }

        mc.player.getInventory().setSelectedSlot(originalSlot);
        mc.player.setYRot(originalYaw);
        mc.player.setXRot(originalPitch);
    }

    private int findCobwebInHotbar() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.COBWEB) {
                return i;
            }
        }
        return -1;
    }

    private float[] calculateRotation(Vec3 target) {
        Vec3 diff = target.subtract(mc.player.getEyePosition());
        double distance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distance));
        return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -89.0f, 89.0f)};
    }
}

