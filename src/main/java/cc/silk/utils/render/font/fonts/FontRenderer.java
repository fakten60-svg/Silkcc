package cc.silk.utils.render.font.fonts;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import java.awt.*;
import java.io.Closeable;

/**
 * Text drawing helper.
 * <p>
 * Minecraft 26.1.2 replaced the GUI render-state model completely: {@code DrawContext} became
 * {@code GuiGraphicsExtractor} and the whole immediate-mode glyph-atlas approach used here before
 * (AWT rasterised atlas + {@code Tesselator} + {@code BufferUploader} + {@code RenderSystem.setShaderTexture})
 * no longer exists - there is no way to create a custom {@code RenderType} outside of Fabric's
 * render-pipeline API. Text is therefore drawn through the vanilla {@link Font}, which keeps every
 * existing call site working while removing the custom atlas machinery.
 * <p>
 * The previous API is preserved so callers only need their {@code PoseStack} argument.
 */
public class FontRenderer implements Closeable {
    /** Vanilla glyphs are drawn on a 9px grid, so sizes are expressed relative to that. */
    private static final float VANILLA_GLYPH_HEIGHT = 9f;

    private final float sizePx;
    private final float scale;

    public FontRenderer(float sizePx) {
        this.sizePx = sizePx;
        this.scale = sizePx / VANILLA_GLYPH_HEIGHT;
    }

    /**
     * Strips all characters prefixed with a section sign from the given string.
     *
     * @param text The string to strip
     * @return The stripped string
     */
    public static String stripControlCodes(String text) {
        char[] chars = text.toCharArray();
        StringBuilder f = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\u00a7') {
                i++;
                continue;
            }
            f.append(c);
        }
        return f.toString();
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    /**
     * Draws a string.
     *
     * @param stack The PoseStack to position the text with
     * @param s     The string to draw
     * @param x     X coordinate to draw at
     * @param y     Y coordinate to draw at
     * @param color Text colour
     */
    public void drawString(PoseStack stack, String s, float x, float y, Color color) {
        drawString(stack, s, x, y, color.getRGB(), true);
    }

    public void drawString(PoseStack stack, String s, float x, float y, int argb, boolean shadow) {
        if (s == null || s.isEmpty()) return;
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1536);
        MultiBufferSource.BufferSource source = MultiBufferSource.immediate(byteBuffer);
        try {
            stack.pushPose();
            stack.translate(x, y, 0);
            stack.scale(scale, scale, 1f);
            font().drawInBatch(s, 0f, 0f, argb, shadow, stack.last().pose(), source,
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            stack.popPose();
        } finally {
            source.endBatch();
            byteBuffer.close();
        }
    }

    /**
     * Draws a string centered on the X coordinate
     *
     * @param stack The PoseStack
     * @param s     The string to draw
     * @param x     X center coordinate of the text to draw
     * @param y     Y coordinate of the text to draw
     * @param color Text colour
     */
    public void drawCenteredString(PoseStack stack, String s, float x, float y, Color color) {
        drawString(stack, s, x - getStringWidth(s) / 2f, y, color);
    }

    /**
     * Calculates the width of the string, if it were drawn on the screen.
     */
    public float getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0f;
        float max = 0f;
        for (String line : stripControlCodes(text).split("\n", -1)) {
            max = Math.max(max, font().width(line) * scale);
        }
        return max;
    }

    /**
     * Calculates the height of the string, if it were drawn on the screen.
     */
    public float getStringHeight(String text) {
        if (text == null || text.isEmpty()) return font().lineHeight * scale;
        int lines = stripControlCodes(text).split("\n", -1).length;
        return font().lineHeight * scale * lines;
    }

    public float getSizePx() {
        return sizePx;
    }

    @Override
    public void close() {
        // Nothing to release: the vanilla font owns its atlases.
    }
}
