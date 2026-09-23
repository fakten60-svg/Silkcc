package cc.silk.utils.render.font.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;

public class BufferUtils {
    private BufferUtils() {
    }

    /**
     * Draws a buffer.
     * <p>
     * Minecraft 26.1.2 removed {@code BufferUploader} and the global shader state: geometry is now
     * drawn through the {@link RenderType} that describes its pipeline.
     *
     * @param type    the render type describing the pipeline to draw with
     * @param builder The buffer
     */
    public static void draw(RenderType type, BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh == null) return;
        type.draw(mesh);
    }
}
