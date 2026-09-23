package cc.silk.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import com.mojang.blaze3d.vertex.*;
import cc.silk.utils.render.CompatShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import cc.silk.utils.render.RenderCompat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL40C;

import java.awt.*;

public class DrawUtils {

    public static void drawRect(PoseStack matrices, float x, float y, float x2, float y2, Color c) {
        Matrix4f matrix = matrices.last().pose();
        setupRender();
        CompatShaders.usePositionColor();
        BufferBuilder buffer = RenderCompat.begin(RenderTypes.debugQuads());
        buffer.addVertex(matrix, x, y2, 0.0F).setColor(c.getRGB());
        buffer.addVertex(matrix, x2, y2, 0.0F).setColor(c.getRGB());
        buffer.addVertex(matrix, x2, y, 0.0F).setColor(c.getRGB());
        buffer.addVertex(matrix, x, y, 0.0F).setColor(c.getRGB());
        RenderCompat.draw(buffer);
        endRender();
    }

    public static void drawRectWithOutline(PoseStack matrices, float x, float y, float x2, float y2, Color c, Color c2) {
        Matrix4f matrix = matrices.last().pose();
        setupRender();
        CompatShaders.usePositionColor();
        BufferBuilder buffer = RenderCompat.begin(RenderTypes.debugQuads());
        buffer.addVertex(matrix, x, y2, 0.0F).setColor(c.getRGB());
        buffer.addVertex(matrix, x2, y2, 0.0F).setColor(c.getRGB());
        buffer.addVertex(matrix, x2, y, 0.0F).setColor(c.getRGB());
        buffer.addVertex(matrix, x, y, 0.0F).setColor(c.getRGB());
        RenderCompat.draw(buffer);

        buffer = RenderCompat.begin(RenderTypes.lines());
        buffer.addVertex(matrix, x, y2, 0.0F).setColor(c2.getRGB());
        buffer.addVertex(matrix, x2, y2, 0.0F).setColor(c2.getRGB());
        buffer.addVertex(matrix, x2, y, 0.0F).setColor(c2.getRGB());
        buffer.addVertex(matrix, x, y, 0.0F).setColor(c2.getRGB());
        buffer.addVertex(matrix, x, y2, 0.0F).setColor(c2.getRGB());
        RenderCompat.draw(buffer);
        endRender();
    }

    public static void drawHorizontalGradientRect(PoseStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
        Matrix4f matrix = matrices.last().pose();
        setupRender();
        CompatShaders.usePositionColor();
        BufferBuilder buffer = RenderCompat.begin(RenderTypes.debugQuads());
        buffer.addVertex(matrix, x1, y1, 0.0F).setColor(startColor.getRGB());
        buffer.addVertex(matrix, x1, y2, 0.0F).setColor(startColor.getRGB());
        buffer.addVertex(matrix, x2, y2, 0.0F).setColor(endColor.getRGB());
        buffer.addVertex(matrix, x2, y1, 0.0F).setColor(endColor.getRGB());
        RenderCompat.draw(buffer);
        endRender();
    }

    public static void setupRender() {
        
        
        
    }

    public static void endRender() {
        
        
        
    }

    public static void rectPoint4VerticalGradient(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float x1, float y1, Color topColor, Color bottomColor) {
        rectPoint4(bufferBuilder, matrix, x, y, x1, y1, topColor, topColor, bottomColor, bottomColor);
    }

    public static void rectPoint4HorizontalGradient(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float x1, float y1, Color leftColor, Color rightColor) {
        rectPoint4(bufferBuilder, matrix, x, y, x1, y1, leftColor, rightColor, rightColor, leftColor);
    }

    public static void rectPoint4(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float x1, float y1, Color color) {
        rectPoint4(bufferBuilder, matrix, x, y, x1, y1, color, color, color, color);
    }

    public static void rectPoint4(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float x1, float y1, Color topLeftColor, Color topRightColor, Color bottomRightColor, Color bottomLeftColor) {
        bufferBuilder.addVertex(matrix, x1, y, 0.0F).setColor(topRightColor.getRGB());
        bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(topLeftColor.getRGB());
        bufferBuilder.addVertex(matrix, x, y1, 0.0F).setColor(bottomLeftColor.getRGB());
        bufferBuilder.addVertex(matrix, x1, y1, 0.0F).setColor(bottomRightColor.getRGB());
    }
}
