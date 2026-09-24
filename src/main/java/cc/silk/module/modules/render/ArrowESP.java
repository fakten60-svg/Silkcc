package cc.silk.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.render.TextureRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import cc.silk.utils.render.CompatShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public final class ArrowESP extends Module {
    private static final Identifier ARROW_TEXTURE = Identifier.fromNamespaceAndPath("silk", "visuals/triangle.png");
    private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
    private final NumberSetting range = new NumberSetting("Range", 10.0D, 200.0D, 120.0D, 5.0D);
    private final NumberSetting size = new NumberSetting("Size", 8.0D, 64.0D, 24.0D, 1.0D);
    private final NumberSetting offset = new NumberSetting("Offset", 0.0D, 64.0D, 28.0D, 1.0D);
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255, 200));

    public ArrowESP() {
        super("Arrow ESP", "Displays arrows pointing to players", -1, Category.RENDER);
        addSettings(showSelf, range, size, offset, color);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (isNull()) return;

        GuiGraphicsExtractor context = event.getContext();
        PoseStack matrices = new PoseStack();
        int width = event.getWidth();
        int height = event.getHeight();
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        float cameraYaw = mc.gameRenderer.getMainCamera().yRot();

        
        
        
        
        CompatShaders.usePositionTexColor();

        Color arrowColor = color.getValue();
        int baseR = arrowColor.getRed();
        int baseG = arrowColor.getGreen();
        int baseB = arrowColor.getBlue();
        int baseA = arrowColor.getAlpha();
        float sizeValue = size.getValueFloat();
        float offsetValue = offset.getValueFloat();
        double maxRange = range.getValue();

        for (Player player : mc.level.players()) {
            if (!shouldRender(player)) continue;
            double distanceSq = mc.player.distanceToSqr(player);
            if (distanceSq > maxRange * maxRange) continue;

            double interpolatedX = player.xo + (player.getX() - player.xo) * tickDelta;
            double interpolatedZ = player.zo + (player.getZ() - player.zo) * tickDelta;

            double dx = interpolatedX - cameraPos.x;
            double dz = interpolatedZ - cameraPos.z;

            double planar = Math.sqrt(dx * dx + dz * dz);
            if (planar < 1.0E-6) continue;

            float yawToPlayer = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0F - cameraYaw);

            double distance = Math.sqrt(distanceSq);
            float distanceFactor = (float) Math.min(distance / maxRange, 1.0D);
            float alphaScale = 1.0f - distanceFactor * 0.6f;
            int arrowAlpha = Math.max(24, Math.min(255, (int) (baseA * alphaScale)));

            matrices.pushPose();
            matrices.translate(centerX, centerY, 0.0);
            matrices.mulPose(Axis.ZP.rotationDegrees(yawToPlayer));
            matrices.translate(0.0, -(offsetValue + distanceFactor * offsetValue), 0.0);

            int baseColorPacked = (baseA << 24) | (baseR << 16) | (baseG << 8) | baseB;
            int packed = (arrowAlpha << 24) | (baseColorPacked & 0x00FFFFFF);
            TextureRenderer.drawCenteredQuad(matrices, ARROW_TEXTURE, sizeValue, sizeValue, packed);
            matrices.popPose();
        }

        
        
        
    }

    private boolean shouldRender(Player player) {
        if (player == mc.player && !showSelf.getValue()) return false;
        return !player.isSpectator();
    }
}
