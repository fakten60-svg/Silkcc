package cc.silk.module.modules.render;

import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.render.W2SUtil;
import cc.silk.utils.render.nanovg.NanoVGRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Trajectories extends Module {

    private final BooleanSetting showLandingPoint = new BooleanSetting("Show Landing Point", true);
    private final BooleanSetting trackThrown = new BooleanSetting("Track Thrown", true);
    private final NumberSetting trackDuration = new NumberSetting("Track Duration (s)", 1.0, 10.0, 3.0, 0.5);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.0, 5.0, 2.0, 0.5);
    private final NumberSetting pointSize = new NumberSetting("Point Size", 2.0, 10.0, 5.0, 0.5);
    private final ColorSetting lineColor = new ColorSetting("Line Color", new Color(255, 255, 255, 200));
    private final ColorSetting hitColor = new ColorSetting("Hit Color", new Color(255, 50, 50, 255));
    private final ColorSetting thrownColor = new ColorSetting("Thrown Color", new Color(100, 200, 255, 200));
    private final NumberSetting maxPoints = new NumberSetting("Max Points", 50, 500, 200, 10);

    private final Map<Entity, ProjectileTracker> trackedProjectiles = new HashMap<>();

    public Trajectories() {
        super("Trajectories", "Shows projectile trajectory path", -1, Category.RENDER);
        addSettings(showLandingPoint, trackThrown, trackDuration, lineWidth, pointSize, lineColor, hitColor,
                thrownColor, maxPoints);
    }

    private static class ProjectileTracker {
        List<Vec3> path = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        boolean isExpired(double durationSeconds) {
            return (System.currentTimeMillis() - startTime) / 1000.0 > durationSeconds;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (isNull() || mc.player == null || mc.level == null)
            return;

        if (trackThrown.getValue()) {
            updateTrackedProjectiles();
        } else {
            trackedProjectiles.clear();
        }

        NanoVGRenderer.beginFrame();

        ItemStack heldItem = mc.player.getMainHandItem();
        if (isProjectile(heldItem) && isUsingItem(heldItem)) {
            List<Vec3> trajectory = calculateTrajectory(heldItem);
            if (!trajectory.isEmpty()) {
                renderTrajectoryLines(trajectory, lineColor.getValue(), false);
            }
        }

        if (trackThrown.getValue()) {
            for (ProjectileTracker tracker : trackedProjectiles.values()) {
                if (!tracker.path.isEmpty()) {
                    renderTrajectoryLines(tracker.path, thrownColor.getValue(), true);
                }
            }
        }

        NanoVGRenderer.endFrame();
    }

    private boolean isUsingItem(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof BowItem) {
            return mc.player.isUsingItem() && mc.player.getUseItemRemainingTicks() > 0;
        }

        if (item instanceof CrossbowItem) {
            return CrossbowItem.isCharged(stack);
        }

        if (item instanceof TridentItem) {
            return mc.player.isUsingItem() && mc.player.getUseItemRemainingTicks() > 0;
        }

        if (item instanceof SnowballItem || item instanceof EggItem ||
                item instanceof EnderpearlItem || item instanceof ExperienceBottleItem ||
                item instanceof PotionItem) {
            return true;
        }

        if (item instanceof FishingRodItem) {
            return mc.player.isUsingItem();
        }

        return false;
    }

    private void updateTrackedProjectiles() {
        trackedProjectiles.entrySet().removeIf(entry -> entry.getValue().isExpired(trackDuration.getValue()) ||
                !entry.getKey().isAlive() ||
                entry.getKey().isRemoved());

        for (Entity entity : mc.level.players()) {
            if (isTrackableProjectile(entity) && !trackedProjectiles.containsKey(entity)) {
                if (entity.tickCount < 5 && entity.distanceToSqr(mc.player) < 100) {
                    trackedProjectiles.put(entity, new ProjectileTracker());
                }
            }
        }

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        for (Map.Entry<Entity, ProjectileTracker> entry : trackedProjectiles.entrySet()) {
            Entity entity = entry.getKey();
            ProjectileTracker tracker = entry.getValue();

            double x = entity.xo + (entity.getX() - entity.xo) * tickDelta;
            double y = entity.yo + (entity.getY() - entity.yo) * tickDelta;
            double z = entity.zo + (entity.getZ() - entity.zo) * tickDelta;

            Vec3 pos = new Vec3(x, y, z);

            if (tracker.path.isEmpty() || tracker.path.get(tracker.path.size() - 1).distanceToSqr(pos) > 0.01) {
                tracker.path.add(pos);

                if (tracker.path.size() > maxPoints.getValueInt()) {
                    tracker.path.remove(0);
                }
            }
        }
    }

    private boolean isTrackableProjectile(Entity entity) {
        return entity instanceof Arrow ||
                entity instanceof SpectralArrow ||
                entity instanceof ThrownTrident ||
                entity instanceof Snowball ||
                entity instanceof ThrownEgg ||
                entity instanceof ThrownEnderpearl ||
                entity instanceof ThrownExperienceBottle ||
                entity instanceof AbstractThrownPotion ||
                entity instanceof FishingHook;
    }

    private boolean isProjectile(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof TridentItem ||
                item instanceof SnowballItem ||
                item instanceof EggItem ||
                item instanceof EnderpearlItem ||
                item instanceof ExperienceBottleItem ||
                item instanceof PotionItem ||
                item instanceof FishingRodItem;
    }

    private List<Vec3> calculateTrajectory(ItemStack stack) {
        List<Vec3> points = new ArrayList<>();
        Item item = stack.getItem();

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        double playerX = mc.player.xo + (mc.player.getX() - mc.player.xo) * tickDelta;
        double playerY = mc.player.yo + (mc.player.getY() - mc.player.yo) * tickDelta;
        double playerZ = mc.player.zo + (mc.player.getZ() - mc.player.zo) * tickDelta;

        float yaw = mc.player.yRotO + (mc.player.getYRot() - mc.player.yRotO) * tickDelta;
        float pitch = mc.player.xRotO + (mc.player.getXRot() - mc.player.xRotO) * tickDelta;

        Vec3 pos = new Vec3(playerX, playerY + mc.player.getEyeHeight(), playerZ);

        Vec3 velocity = getRotationVector(pitch, yaw);

        float power = getProjectilePower(item, stack);
        velocity = velocity.scale(power);

        float gravity = getGravity(item);
        float drag = getDrag(item);

        int maxIterations = maxPoints.getValueInt();

        for (int i = 0; i < maxIterations; i++) {
            points.add(pos);

            Vec3 nextPos = pos.add(velocity);
            HitResult hitResult = raycast(pos, nextPos);

            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                points.add(hitResult.getLocation());
                break;
            }

            pos = nextPos;

            velocity = velocity.scale(drag);
            velocity = velocity.add(0, -gravity, 0);

            if (velocity.lengthSqr() < 0.001)
                break;
        }

        return points;
    }

    private Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = (float) Math.cos(g);
        float i = (float) Math.sin(g);
        float j = (float) Math.cos(f);
        float k = (float) Math.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    private float getProjectilePower(Item item, ItemStack stack) {
        if (item instanceof BowItem) {
            int useTicks = mc.player.getUseItemRemainingTicks();
            float charge = BowItem.getPowerForTime(useTicks);
            return charge * 3.0f;
        } else if (item instanceof CrossbowItem) {
            return 3.15f;
        } else if (item instanceof TridentItem) {
            return 2.5f;
        } else if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem) {
            return 1.5f;
        } else if (item instanceof ExperienceBottleItem || item instanceof PotionItem) {
            return 1.0f;
        } else if (item instanceof FishingRodItem) {
            return 1.5f;
        }
        return 1.5f;
    }

    private float getGravity(Item item) {
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            return 0.05f;
        } else if (item instanceof TridentItem) {
            return 0.05f;
        } else if (item instanceof FishingRodItem) {
            return 0.04f;
        }
        return 0.03f;
    }

    private float getDrag(Item item) {
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) {
            return 0.99f;
        }
        return 0.99f;
    }

    private HitResult raycast(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = mc.level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player));

        AABB box = new AABB(start, end).inflate(1.0);
        var entityHit = ProjectileUtil.getEntityHitResult(
                mc.player, start, end, box,
                entity -> !entity.isSpectator() && entity != mc.player, 0.0);

        if (entityHit != null) {
            return entityHit;
        }

        return blockHit;
    }

    private void renderTrajectoryLines(List<Vec3> points, Color color, boolean isThrown) {
        if (points.size() < 2)
            return;

        float width = lineWidth.getValueFloat();

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 start = points.get(i);
            Vec3 end = points.get(i + 1);

            Vec3 screenStart = W2SUtil.getCoords(start);
            Vec3 screenEnd = W2SUtil.getCoords(end);

            if (screenStart != null && screenEnd != null && screenStart.z >= 0 && screenStart.z < 1 && screenEnd.z >= 0
                    && screenEnd.z < 1) {
                Color drawColor = color;
                if (isThrown && i < points.size() - 1) {
                    float alpha = (float) i / points.size();
                    drawColor = new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            (int) (color.getAlpha() * alpha));
                }

                NanoVGRenderer.drawLine(
                        (float) screenStart.x, (float) screenStart.y,
                        (float) screenEnd.x, (float) screenEnd.y,
                        width, drawColor);
            }
        }

        if (showLandingPoint.getValue() && !isThrown && !points.isEmpty()) {
            Vec3 lastPoint = points.get(points.size() - 1);
            Vec3 screenPos = W2SUtil.getCoords(lastPoint);

            if (screenPos != null && screenPos.z >= 0 && screenPos.z < 1) {
                Color hitCol = hitColor.getValue();
                float size = pointSize.getValueFloat();

                NanoVGRenderer.drawCircle((float) screenPos.x, (float) screenPos.y, size + 2,
                        new Color(0, 0, 0, 150));
                NanoVGRenderer.drawCircle((float) screenPos.x, (float) screenPos.y, size, hitCol);
            }
        }
    }
}
