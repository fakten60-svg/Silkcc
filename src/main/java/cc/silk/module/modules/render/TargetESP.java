package cc.silk.module.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.render.Render3DEvent;
import cc.silk.utils.render.RenderCompat;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.render.RenderCompat;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;

public final class TargetESP extends Module {

    private static final Identifier FIREFLY_TEXTURE = Identifier.fromNamespaceAndPath("silk", "imgs/firefly.png");

    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Living");
    private final NumberSetting layers = new NumberSetting("Layers", 1, 5, 3, 1);
    private final NumberSetting orbsPerLayer = new NumberSetting("Orbs Per Layer", 5, 20, 14, 1);
    private final NumberSetting orbSize = new NumberSetting("Orb Size", 0.1, 0.8, 0.3, 0.05);
    private final NumberSetting speed = new NumberSetting("Speed", 0.5, 5.0, 2.5, 0.1);
    private final NumberSetting heightOffset = new NumberSetting("Height Offset", 0.0, 2.0, 1.0, 0.1);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Single", "Single", "Gradient");
    private final ColorSetting color = new ColorSetting("Color", new Color(120, 240, 255, 220));
    private final ColorSetting gradientColor1 = new ColorSetting("Gradient Color 1", new Color(255, 0, 0, 220));
    private final ColorSetting gradientColor2 = new ColorSetting("Gradient Color 2", new Color(0, 0, 255, 220));

    public TargetESP() {
        super("Target ESP", "Ghost-like spiral orbs around players", -1, Category.RENDER);
        addSettings(targets, layers, orbsPerLayer, orbSize, speed, heightOffset, colorMode, color,
                gradientColor1, gradientColor2);
    }

    @EventHandler
    private void onRender3D(Render3DEvent e) {
        if (isNull())
            return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        float size = orbSize.getValueFloat();
        float yOff = heightOffset.getValueFloat();
        Color c = color.getValue();
        int numLayers = layers.getValueInt();
        int orbs = orbsPerLayer.getValueInt();
        boolean useGradient = colorMode.isMode("Gradient");

        float iAge = (System.currentTimeMillis() % 100000) / 50.0f;

        for (var entity : mc.level.players()) {
            if (!shouldRender(entity))
                continue;

            double tPosX = entity.xo + (entity.getX() - entity.xo) * tickDelta - cam.x;
            double tPosY = entity.yo + (entity.getY() - entity.yo) * tickDelta - cam.y + yOff;
            double tPosZ = entity.zo + (entity.getZ() - entity.zo) * tickDelta - cam.z;

            if (mc.player.hasLineOfSight(entity)) {
                
                
            } else {
                
            }

            BufferBuilder buffer = RenderCompat.begin(RenderTypes.text(FIREFLY_TEXTURE));

            float ageMultiplier = iAge * speed.getValueFloat();

            for (int j = 0; j < numLayers; j++) {
                float jOffset = j * 120;
                float jMultiplier = j + 1;

                for (int i = 0; i <= orbs; i++) {
                    float iFloat = (float) i;
                    double radians = Math
                            .toRadians(((iFloat / 1.5f + iAge * speed.getValueFloat()) * 8 + jOffset) % 2880);
                    double sinQuad = Math.sin(Math.toRadians(ageMultiplier + i * jMultiplier) * 3f) / 1.8f;
                    float offset = iFloat / orbs;

                    int orbColor;
                    if (useGradient) {
                        Color blendedColor = blendColors(gradientColor1.getValue(), gradientColor2.getValue(), offset);
                        orbColor = applyOpacity(blendedColor.getRGB(), offset);
                    } else {
                        orbColor = applyOpacity(c.getRGB(), offset);
                    }

                    double ghostX = Math.cos(radians) * entity.getBbWidth();
                    double ghostY = sinQuad;
                    double ghostZ = Math.sin(radians) * entity.getBbWidth();

                    PoseStack matrices = new PoseStack();
                    matrices.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().xRot()));
                    matrices.mulPose(Axis.YP.rotationDegrees(mc.gameRenderer.getMainCamera().yRot() + 180.0F));
                    matrices.translate(tPosX + ghostX, tPosY + ghostY, tPosZ + ghostZ);
                    matrices.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().yRot()));
                    matrices.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().xRot()));

                    Matrix4f matrix = matrices.last().pose();

                    buffer.addVertex(matrix, -size, size, 0).setUv(0f, 1f).setColor(orbColor);
                    buffer.addVertex(matrix, size, size, 0).setUv(1f, 1f).setColor(orbColor);
                    buffer.addVertex(matrix, size, -size, 0).setUv(1f, 0).setColor(orbColor);
                    buffer.addVertex(matrix, -size, -size, 0).setUv(0, 0).setColor(orbColor);
                }
            }

            RenderCompat.draw(buffer);

            if (mc.player.hasLineOfSight(entity)) {
                
            }
        }

        
        
    }

    private Color blendColors(Color color1, Color color2, float ratio) {
        ratio = Math.min(1, Math.max(0, ratio));
        int r = (int) (color1.getRed() * (1 - ratio) + color2.getRed() * ratio);
        int g = (int) (color1.getGreen() * (1 - ratio) + color2.getGreen() * ratio);
        int b = (int) (color1.getBlue() * (1 - ratio) + color2.getBlue() * ratio);
        int a = (int) (color1.getAlpha() * (1 - ratio) + color2.getAlpha() * ratio);
        return new Color(r, g, b, a);
    }

    private int applyOpacity(int colorInt, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        Color color = new Color(colorInt, true);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity))
                .getRGB();
    }

    private boolean shouldRender(net.minecraft.world.entity.Entity entity) {
        if (entity == mc.player)
            return false;
        if (targets.isMode("Players"))
            return entity instanceof Player;
        return entity instanceof LivingEntity;
    }

}


