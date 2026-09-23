package cc.silk.module.modules.combat;

import cc.silk.event.impl.render.Render3DEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.modules.misc.Teams;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.friend.FriendManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import cc.silk.utils.mc.InventoryUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class AimAssist extends Module {

    private final NumberSetting maxYawSpeed = new NumberSetting("Max Yaw Speed", 0.1, 5.0, 2.0, 0.1);
    private final NumberSetting minYawSpeed = new NumberSetting("Min Yaw Speed", 0.1, 5.0, 2.0, 0.1);

    private final NumberSetting minPitchSpeed = new NumberSetting("Min Pitch Speed", 0.1, 5.0, 2.0, 0.1);
    private final NumberSetting maxPitchSpeed = new NumberSetting("Max Pitch Speed", 0.1, 5.0, 2.0, 0.1);

    private final NumberSetting fov = new NumberSetting("FOV", 10.0, 180.0, 90.0, 1.0);
    private final NumberSetting range = new NumberSetting("Range", 1.0, 10.0, 4.5, 0.1);
    private final NumberSetting smoothing = new NumberSetting("Smoothing", 1.0, 20.0, 10.0, 0.5);
    private final NumberSetting pitchThreshold = new NumberSetting("Pitch Threshold", 0.0, 90.0, 60.0, 1.0);

    private final BooleanSetting targetPlayers = new BooleanSetting("Target Players", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Target Mobs", false);
    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private final BooleanSetting disableOnTarget = new BooleanSetting("Disable on target", false);
    private final BooleanSetting ignoreBlocks = new BooleanSetting("Ignore Blocks", true);

    private Entity currentTarget = null;
    private long lastUpdateTime = 0;
    private float currentBaseSpeed = 10f;
    private float nextBaseSpeed = 10f;
    private long lastSpeedChangeTime = 0;

    public AimAssist() {
        super("Aim Assist", "Gives you assistance on your aim", Category.COMBAT);
        addSettings(
                maxYawSpeed, minYawSpeed, maxPitchSpeed, minPitchSpeed, fov, range, smoothing, pitchThreshold,
                targetPlayers, targetMobs, weaponsOnly, throughWalls, disableOnTarget, ignoreBlocks);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (isNull())
            return;
        if (maxPitchSpeed.getValueFloat() <= minPitchSpeed.getValueFloat()) {
            maxPitchSpeed.setValue(minPitchSpeed.getValue() + 0.1);
        }
        if (maxYawSpeed.getValueFloat() <= minYawSpeed.getValueFloat()) {
            maxYawSpeed.setValue(minYawSpeed.getValue() + 0.1);
        }
        if (weaponsOnly.getValue() && !isHoldingWeapon())
            return;

        if (mc.screen != null)
            return;

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK
                && mc.options.keyAttack.isDown()) {
            return;
        }

        if (mc.player.getXRot() > pitchThreshold.getValueFloat()) {
            return;
        }

        if (ignoreBlocks.getValue() && mc.hitResult != null
                && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }

        currentTarget = findBestTarget();

        if (mc.crosshairPickEntity == currentTarget && disableOnTarget.getValue()) {
            return;
        }

        if (currentTarget != null) {
            if (!throughWalls.getValue() && !mc.player.hasLineOfSight(currentTarget))
                return;

            Vec3 chestPos = getChestPosition(currentTarget);
            float[] rotation = calculateRotation(chestPos);
            applySmoothAiming(rotation[0], rotation[1]);
        }
    }

    private Entity findBestTarget() {
        if (isNull())
            return null;

        Entity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : mc.level.players()) {
            if (!isValidTarget(entity))
                continue;

            double distance = mc.player.distanceTo(entity);
            if (distance > range.getValue())
                continue;

            Vec3 chestPos = getChestPosition(entity);
            float[] rotation = calculateRotation(chestPos);
            double fovDistance = getFOVDistance(rotation[0], rotation[1]);

            if (fovDistance <= fov.getValue() / 2.0) {
                double score = distance + (fovDistance * 2.0);
                if (score < bestScore) {
                    bestScore = score;
                    bestTarget = entity;
                }
            }
        }

        return bestTarget;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !(entity instanceof LivingEntity livingEntity))
            return false;
        if (!livingEntity.isAlive() || livingEntity.isRemoved())
            return false;
        if (Teams.isTeammate(entity))
            return false;
        if (entity instanceof Player player && FriendManager.isFriend(player.getUUID()))
            return false;

        return entity instanceof Player ? targetPlayers.getValue() : targetMobs.getValue();
    }

    private Vec3 getChestPosition(Entity entity) {
        return new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
    }

    private float[] calculateRotation(Vec3 target) {
        Vec3 diff = target.subtract(mc.player.getEyePosition());
        double distance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distance));
        return new float[] { Mth.wrapDegrees(yaw), Mth.clamp(pitch, -89.0f, 89.0f) };
    }

    private double getFOVDistance(float targetYaw, float targetPitch) {
        float yawDiff = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        float pitchDiff = targetPitch - mc.player.getXRot();
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private void applySmoothAiming(float targetYaw, float targetPitch) {
        long currentTime = System.currentTimeMillis();

        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            return;
        }

        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        if (deltaTime < 0.001f || deltaTime > 0.1f)
            return;
        deltaTime *= randomFloat(0.9f, 1.1f);

        updateBaseSpeed();

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float distance = (float) Math.hypot(yawDiff, pitchDiff);
        if (distance < 0.3f)
            return;

        float t = Math.min(distance / 10f, 1f);
        float eased = easeOutCubic(t);

        float lerpFactor = eased * (smoothing.getValueFloat() / 10f) * deltaTime;
        float newYaw = Mth.lerp(lerpFactor, currentYaw, currentYaw + yawDiff);
        float newPitch = Mth.lerp(lerpFactor, currentPitch, currentPitch + pitchDiff);

        mc.player.setYRot(newYaw);
        mc.player.setXRot(Mth.clamp(newPitch, -89f, 89f));
    }

    private float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }

    private void updateBaseSpeed() {
        long now = System.currentTimeMillis();
        if (now - lastSpeedChangeTime > 200) {
            lastSpeedChangeTime = now;
            float change = randomFloat(-5f, 5f);
            nextBaseSpeed += change;
            nextBaseSpeed = Mth.clamp(nextBaseSpeed, 8f, 100f);
        }

        currentBaseSpeed = (currentBaseSpeed * 0.9f) + (nextBaseSpeed * 0.1f);
    }

    private float randomFloat(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    private boolean isHoldingWeapon() {
        if (mc.player == null)
            return false;
        if (mc.player.getMainHandItem().isEmpty())
            return false;
        Item heldItem = mc.player.getMainHandItem().getItem();
        return InventoryUtil.isSword(heldItem) || heldItem instanceof AxeItem;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }
}
