package cc.silk.utils.render.nanovg;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.nanovg.NanoVG.*;

public class NanoVGFrameManager {
    private static boolean inFrame = false;
    private static int savedVAO = 0;
    private static int savedArrayBuffer = 0;
    private static int savedElementBuffer = 0;
    private static int savedProgram = 0;
    private static int savedTexture = 0;

    public static void beginFrame() {
        

        if (!NanoVGContext.isInitialized() || !NanoVGContext.isValid()) {
            NanoVGContext.init();
            NanoVGFontManager.loadFonts();
        }

        if (inFrame) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }

        int framebufferWidth = mc.getWindow().getWidth();
        int framebufferHeight = mc.getWindow().getHeight();

        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }

        try {
            

            // mc.getMainRenderTarget().beginWrite(true); // removed - no longer needed in 26.1.2

            savedVAO = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            savedArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            savedElementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
            savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            savedTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);

            NanoVGContext.assertValid();
            nvgBeginFrame(NanoVGContext.getHandle(), framebufferWidth, framebufferHeight, 1f);

            int scaledWidth = mc.getWindow().getGuiScaledWidth();
            int scaledHeight = mc.getWindow().getGuiScaledHeight();
            float scaleX = (float) framebufferWidth / (float) scaledWidth;
            float scaleY = (float) framebufferHeight / (float) scaledHeight;
            nvgScale(NanoVGContext.getHandle(), scaleX, scaleY);

            inFrame = true;
        } catch (Exception e) {
            inFrame = false;
            throw e;
        }
    }

    public static void endFrame() {
        if (!inFrame) {
            return;
        }

        

        try {
            NanoVGContext.assertValid();
            nvgEndFrame(NanoVGContext.getHandle());
        } catch (Exception e) {
            inFrame = false;
            throw e;
        }

        inFrame = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getMainRenderTarget() != null) {
            // mc.getMainRenderTarget().beginWrite(true); // removed - no longer needed in 26.1.2
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL30.glBindVertexArray(savedVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedArrayBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, savedElementBuffer);
        GL20.glUseProgram(savedProgram);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedTexture);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public static boolean isInFrame() {
        return inFrame;
    }

    public static void resetInFrame() {
        inFrame = false;
    }
}
