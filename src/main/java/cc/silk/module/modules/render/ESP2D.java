package cc.silk.module.modules.render;

import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.mixin.WorldRendererAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.render.W2SUtil;
import cc.silk.utils.render.nanovg.NanoVGRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4d;

import java.awt.*;

public class ESP2D extends Module {

        private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Passives", "Hostiles",
                        "All");
        private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
        private final NumberSetting range = new NumberSetting("Range", 10, 200, 64, 5);
        private final ModeSetting boxMode = new ModeSetting("Box Mode", "Full", "Full", "Corners", "Rounded");
        private final BooleanSetting boxSetting = new BooleanSetting("Box", true);
        private final NumberSetting roundRadius = new NumberSetting("Round Radius", 0, 10, 3, 0.5);
        private final BooleanSetting boxFill = new BooleanSetting("Box Fill", false);
        private final NumberSetting fillOpacity = new NumberSetting("Fill Opacity", 0, 255, 80, 5);
        private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
        private final ColorSetting boxColor = new ColorSetting("Box Color", new Color(255, 255, 255));
        private final ColorSetting playerColor = new ColorSetting("Player Color", new Color(255, 255, 255));
        private final ColorSetting passiveColor = new ColorSetting("Passive Color", new Color(0, 255, 0));
        private final ColorSetting hostileColor = new ColorSetting("Hostile Color", new Color(255, 0, 0));

        public ESP2D() {
                super("2D ESP", "Draws 2D boxes around entities", -1, Category.RENDER);
                addSettings(targets, showSelf, range, boxMode, boxSetting, roundRadius, boxFill, fillOpacity, healthBar,
                                boxColor, playerColor, passiveColor, hostileColor);
        }

        @EventHandler
        private void onRender2D(Render2DEvent event) {
                if (isNull() || mc.level == null || mc.player == null)
                        return;

                

                NanoVGRenderer.beginFrame();

                for (Entity entity : mc.level.players()) {
                        if (!(entity instanceof LivingEntity))
                                continue;

                        if (!shouldRender(entity))
                                continue;

                        AABB box = entity.getBoundingBox();

                        if (!((WorldRendererAccessor) mc.levelRenderer).getFrustum().isVisible(box))
                                continue;

                        double x = entity.xo + (entity.getX() - entity.xo)
                                        * mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                        double y = entity.yo + (entity.getY() - entity.yo)
                                        * mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                        double z = entity.zo + (entity.getZ() - entity.zo)
                                        * mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

                        AABB expandedBox = new AABB(
                                        box.minX - entity.getX() + x - 0.05,
                                        box.minY - entity.getY() + y,
                                        box.minZ - entity.getZ() + z - 0.05,
                                        box.maxX - entity.getX() + x + 0.05,
                                        box.maxY - entity.getY() + y + 0.1,
                                        box.maxZ - entity.getZ() + z + 0.05);

                        Vec3[] vectors = new Vec3[] {
                                        new Vec3(expandedBox.minX, expandedBox.minY, expandedBox.minZ),
                                        new Vec3(expandedBox.minX, expandedBox.maxY, expandedBox.minZ),
                                        new Vec3(expandedBox.maxX, expandedBox.minY, expandedBox.minZ),
                                        new Vec3(expandedBox.maxX, expandedBox.maxY, expandedBox.minZ),
                                        new Vec3(expandedBox.minX, expandedBox.minY, expandedBox.maxZ),
                                        new Vec3(expandedBox.minX, expandedBox.maxY, expandedBox.maxZ),
                                        new Vec3(expandedBox.maxX, expandedBox.minY, expandedBox.maxZ),
                                        new Vec3(expandedBox.maxX, expandedBox.maxY, expandedBox.maxZ),
                        };

                        Vector4d position = null;

                        for (Vec3 vector : vectors) {
                                Vec3 vectorToScreen = W2SUtil.getCoords(vector);

                                if (vectorToScreen.z > 0 && vectorToScreen.z < 1) {
                                        if (position == null) {
                                                position = new Vector4d(vectorToScreen.x, vectorToScreen.y,
                                                                vectorToScreen.z, 0);
                                        }

                                        position.x = Math.min(vectorToScreen.x, position.x);
                                        position.y = Math.min(vectorToScreen.y, position.y);
                                        position.z = Math.max(vectorToScreen.x, position.z);
                                        position.w = Math.max(vectorToScreen.y, position.w);
                                }
                        }

                        if (position != null) {
                                float posX = (float) position.x;
                                float posY = (float) position.y;
                                float endPosX = (float) position.z;
                                float endPosY = (float) position.w;

                                Color accentColor = boxColor.getValue();

                                if (boxFill.getValue()) {
                                        int opacity = fillOpacity.getValueInt();
                                        Color fillColor = new Color(0, 0, 0, opacity);
                                        NanoVGRenderer.drawRect(posX, posY, endPosX - posX,
                                                        endPosY - posY, fillColor);
                                }

                                if (boxSetting.getValue()) {
                                        float shadowOffset = 1f;
                                        float lineWidth = 0.5f;

                                        if (boxMode.getMode().equals("Full")) {
                                                NanoVGRenderer.drawRect(posX - shadowOffset, posY,
                                                                lineWidth + shadowOffset, endPosY - posY + shadowOffset,
                                                                Color.BLACK);
                                                NanoVGRenderer.drawRect(posX - shadowOffset,
                                                                posY - shadowOffset, endPosX - posX + shadowOffset,
                                                                lineWidth + shadowOffset, Color.BLACK);
                                                NanoVGRenderer.drawRect(endPosX - lineWidth, posY,
                                                                lineWidth + shadowOffset, endPosY - posY + shadowOffset,
                                                                Color.BLACK);
                                                NanoVGRenderer.drawRect(posX - shadowOffset,
                                                                endPosY - lineWidth, endPosX - posX + shadowOffset,
                                                                lineWidth + shadowOffset, Color.BLACK);

                                                NanoVGRenderer.drawRect(posX - lineWidth, posY,
                                                                lineWidth, endPosY - posY, accentColor);
                                                NanoVGRenderer.drawRect(posX, endPosY - lineWidth,
                                                                endPosX - posX, lineWidth, accentColor);
                                                NanoVGRenderer.drawRect(posX, posY - lineWidth,
                                                                endPosX - posX, lineWidth, accentColor);
                                                NanoVGRenderer.drawRect(endPosX - lineWidth, posY,
                                                                lineWidth, endPosY - posY, accentColor);
                                        } else if (boxMode.getMode().equals("Rounded")) {
                                                float radius = (float) roundRadius.getValue();
                                                float boxWidth = endPosX - posX;
                                                float boxHeight = endPosY - posY;
                                                
                                                NanoVGRenderer.drawRoundedRectOutline(
                                                                posX - 1, posY - 1, boxWidth + 2, boxHeight + 2, radius, 1.5f, Color.BLACK);
                                                NanoVGRenderer.drawRoundedRectOutline(
                                                                posX, posY, boxWidth, boxHeight, radius, 1f, accentColor);
                                        } else {
                                                float boxWidth = endPosX - posX;
                                                float cornerLength = Math.min(boxWidth * 0.25f, 15f);

                                                NanoVGRenderer.drawRect(posX - shadowOffset, posY,
                                                                lineWidth + shadowOffset, cornerLength + shadowOffset,
                                                                Color.BLACK);
                                                NanoVGRenderer.drawRect(posX - shadowOffset,
                                                                posY - shadowOffset, cornerLength + shadowOffset,
                                                                lineWidth + shadowOffset, Color.BLACK);

                                                NanoVGRenderer.drawRect(endPosX - lineWidth, posY,
                                                                lineWidth + shadowOffset, cornerLength + shadowOffset,
                                                                Color.BLACK);
                                                NanoVGRenderer.drawRect(
                                                                endPosX - cornerLength - shadowOffset,
                                                                posY - shadowOffset, cornerLength + shadowOffset,
                                                                lineWidth + shadowOffset, Color.BLACK);

                                                NanoVGRenderer.drawRect(posX - shadowOffset,
                                                                endPosY - cornerLength, lineWidth + shadowOffset,
                                                                cornerLength + shadowOffset, Color.BLACK);
                                                NanoVGRenderer.drawRect(posX - shadowOffset,
                                                                endPosY - lineWidth, cornerLength + shadowOffset,
                                                                lineWidth + shadowOffset, Color.BLACK);

                                                NanoVGRenderer.drawRect(endPosX - lineWidth,
                                                                endPosY - cornerLength, lineWidth + shadowOffset,
                                                                cornerLength + shadowOffset, Color.BLACK);
                                                NanoVGRenderer.drawRect(
                                                                endPosX - cornerLength - shadowOffset,
                                                                endPosY - lineWidth, cornerLength + shadowOffset,
                                                                lineWidth + shadowOffset, Color.BLACK);

                                                NanoVGRenderer.drawRect(posX - lineWidth, posY,
                                                                lineWidth, cornerLength, accentColor);
                                                NanoVGRenderer.drawRect(posX, posY - lineWidth,
                                                                cornerLength, lineWidth, accentColor);

                                                NanoVGRenderer.drawRect(endPosX - lineWidth, posY,
                                                                lineWidth, cornerLength, accentColor);
                                                NanoVGRenderer.drawRect(endPosX - cornerLength,
                                                                posY - lineWidth, cornerLength, lineWidth, accentColor);

                                                NanoVGRenderer.drawRect(posX - lineWidth,
                                                                endPosY - cornerLength, lineWidth, cornerLength,
                                                                accentColor);
                                                NanoVGRenderer.drawRect(posX, endPosY - lineWidth,
                                                                cornerLength, lineWidth, accentColor);

                                                NanoVGRenderer.drawRect(endPosX - lineWidth,
                                                                endPosY - cornerLength, lineWidth, cornerLength,
                                                                accentColor);
                                                NanoVGRenderer.drawRect(endPosX - cornerLength,
                                                                endPosY - lineWidth, cornerLength, lineWidth,
                                                                accentColor);
                                        }
                                }

                                if (healthBar.getValue() && entity instanceof LivingEntity livingEntity) {
                                        float health = livingEntity.getHealth();
                                        float maxHealth = livingEntity.getMaxHealth();
                                        float healthPercent = Math.min(health / maxHealth, 1f);

                                        float boxWidth = endPosX - posX;
                                        float maxBarWidth = 3.5f;
                                        float barWidth = Math.min(2.5f, boxWidth * 0.08f);
                                        barWidth = Math.min(barWidth, maxBarWidth);

                                        float barHeight = endPosY - posY;
                                        float barX = posX - barWidth - 3f;
                                        float barY = posY;
                                        float yOffset = 0.3f;

                                        NanoVGRenderer.drawRect(barX, barY, barWidth + 0.5f,
                                                        barHeight, Color.BLACK);
                                        NanoVGRenderer.drawRect(barX, barY - yOffset,
                                                        barWidth, yOffset, Color.BLACK);
                                        NanoVGRenderer.drawRect(barX, barY + barHeight,
                                                        barWidth, yOffset, Color.BLACK);

                                        float healthBarHeight = barHeight * healthPercent;
                                        float healthBarY = barY + (barHeight - healthBarHeight);

                                        Color accentDark = new Color(
                                                        Math.max(0, accentColor.getRed() - 60),
                                                        Math.max(0, accentColor.getGreen() - 60),
                                                        Math.max(0, accentColor.getBlue() - 60));

                                        NanoVGRenderer.drawRoundedRectGradient(barX, healthBarY,
                                                        barWidth, healthBarHeight, 0f, accentColor, accentDark);
                                }
                        }
                }

                NanoVGRenderer.endFrame();
        }

        private Color getColorForEntity(Entity entity) {
                if (entity instanceof Player)
                        return playerColor.getValue();
                if (entity instanceof Animal)
                        return passiveColor.getValue();
                if (entity instanceof Enemy)
                        return hostileColor.getValue();
                return Color.WHITE;
        }

        private boolean shouldRender(Entity entity) {
                if (entity == mc.player && !showSelf.getValue())
                        return false;

                if (mc.player.distanceTo(entity) > range.getValue())
                        return false;

                return switch (targets.getMode()) {
                        case "Players" -> entity instanceof Player;
                        case "Passives" -> entity instanceof Animal;
                        case "Hostiles" -> entity instanceof Enemy;
                        case "All" ->
                                entity instanceof Player || entity instanceof Animal
                                                || entity instanceof Enemy;
                        default -> false;
                };
        }
}
