package cc.silk.utils.render;

/**
 * Minecraft 26.1.2 removed {@code RenderSystem.setShader(...)} and the global shader state entirely:
 * geometry is now drawn through a {@link net.minecraft.client.renderer.rendertype.RenderType}, which
 * carries its own {@link com.mojang.blaze3d.pipeline.RenderPipeline}. Selecting a "current shader" has
 * therefore become a no-op, and this class is kept only so call sites stay readable.
 */
public final class CompatShaders {
    private CompatShaders() {
    }

    public static void usePositionColor() {
        // No-op: the render type passed to BufferUtils.draw(...) selects the pipeline.
    }

    public static void usePositionTexColor() {
        // No-op: the render type passed to BufferUtils.draw(...) selects the pipeline.
    }
}
