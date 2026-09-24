package cc.silk.utils.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import com.mojang.blaze3d.vertex.*;
import cc.silk.utils.render.CompatShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import cc.silk.utils.render.RenderCompat;
import net.minecraft.resources.Identifier;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class TextureRenderer {
    private TextureRenderer() {
    }

    public static void enableLinearFiltering() {
        
        
    }

    public static void disableLinearFiltering() {
        
        
    }

    public static void enableMask() {
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
        GlStateManager._depthMask(false);
        GlStateManager._colorMask(0);
    }

    public static void applyMask() {
        GlStateManager._depthFunc(GL11.GL_EQUAL);
        GlStateManager._depthMask(false);
        GlStateManager._colorMask(0x4000);
    }

    public static void disableMask() {
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(0x4000);
        GlStateManager._disableDepthTest();
    }

    public static void drawCenteredQuad(PoseStack matrices, Identifier texture, float width, float height,
            int color) {
        drawCenteredQuad(matrices, texture, width, height, color, false);
    }

    public static void drawCenteredQuad(PoseStack matrices, Identifier texture, float width, float height, int color,
            boolean linearFilter) {
        CompatShaders.usePositionTexColor();

        if (linearFilter) {
            enableLinearFiltering();
        }

        Matrix4f matrix = matrices.last().pose();
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        int a = color >> 24 & 255;
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        BufferBuilder builder = RenderCompat.begin(RenderTypes.text(texture));
        builder.addVertex(matrix, -halfWidth, halfHeight, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, a)
                .setLight(RenderCompat.FULL_BRIGHT);
        builder.addVertex(matrix, halfWidth, halfHeight, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, a)
                .setLight(RenderCompat.FULL_BRIGHT);
        builder.addVertex(matrix, halfWidth, -halfHeight, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, a)
                .setLight(RenderCompat.FULL_BRIGHT);
        builder.addVertex(matrix, -halfWidth, -halfHeight, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, a)
                .setLight(RenderCompat.FULL_BRIGHT);
        RenderCompat.draw(builder);

        if (linearFilter) {
            disableLinearFiltering();
        }
    }

    public static void drawCenteredQuad(GuiGraphicsExtractor context, Identifier texture, float x, float y, float width,
            float height, float rotationDeg, int color) {
        drawCenteredQuad(context, texture, x, y, width, height, rotationDeg, color, false);
    }

    public static void drawCenteredQuad(GuiGraphicsExtractor context, Identifier texture, float x, float y, float width,
            float height, float rotationDeg, int color, boolean linearFilter) {
        PoseStack matrices = new PoseStack();
        matrices.pushPose();
        matrices.translate(x, y, 0.0f);
        if (rotationDeg != 0.0f) {
            matrices.mulPose(Axis.ZP.rotationDegrees(rotationDeg));
        }
        drawCenteredQuad(matrices, texture, width, height, color, linearFilter);
        matrices.popPose();
    }

    public static void drawMaskedQuad(GuiGraphicsExtractor context, Identifier texture, float x, float y, float width,
            float height, int color) {
        drawMaskedQuad(context, texture, x, y, width, height, 0.0f, color, false);
    }

    public static void drawMaskedQuad(GuiGraphicsExtractor context, Identifier texture, float x, float y, float width,
            float height, float rotationDeg, int color, boolean linearFilter) {
        PoseStack matrices = new PoseStack();
        matrices.pushPose();
        matrices.translate(x, y, 0.0f);
        if (rotationDeg != 0.0f) {
            matrices.mulPose(Axis.ZP.rotationDegrees(rotationDeg));
        }

        CompatShaders.usePositionTexColor();

        if (linearFilter) {
            enableLinearFiltering();
        }

        Matrix4f matrix = matrices.last().pose();
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        int a = color >> 24 & 255;
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;

        BufferBuilder builder = RenderCompat.begin(RenderTypes.text(texture));
        builder.addVertex(matrix, -halfWidth, halfHeight, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, a)
                .setLight(RenderCompat.FULL_BRIGHT);
        builder.addVertex(matrix, halfWidth, halfHeight, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, a)
                .setLight(RenderCompat.FULL_BRIGHT);
        builder.addVertex(matrix, halfWidth, -halfHeight, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, a)
                .setLight(RenderCompat.FULL_BRIGHT);
        builder.addVertex(matrix, -halfWidth, -halfHeight, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, a)
                .setLight(RenderCompat.FULL_BRIGHT);
        RenderCompat.draw(builder);

        if (linearFilter) {
            disableLinearFiltering();
        }

        matrices.popPose();
    }
}
