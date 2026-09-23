package cc.silk.utils.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Immediate-mode drawing shim for Minecraft 26.1.2.
 * <p>
 * Older versions let mods start a {@code Tesselator} buffer, select a program with
 * {@code RenderSystem.setShader(...)} and flush it with {@code BufferUploader.drawWithGlobalProgram(...)}.
 * All three are gone: a {@link RenderType} now owns the pipeline (blend state, depth state, shader,
 * vertex format) and draws the finished {@link MeshData} itself.
 * <p>
 * {@link #begin(RenderType)} remembers the render type so that {@link #draw(BufferBuilder)} can flush it,
 * which keeps the old "begin / add vertices / draw" call shape working. Drawing happens on the client
 * render thread, so a single thread-local slot is enough.
 */
public final class RenderCompat {
    private static final int BUFFER_SIZE = 1536;
    private static final ThreadLocal<RenderType> CURRENT = new ThreadLocal<>();

    private RenderCompat() {
    }

    /** Starts a buffer that uses the vertex format of the given render type. */
    public static BufferBuilder begin(RenderType type) {
        CURRENT.set(type);
        return new BufferBuilder(new ByteBufferBuilder(BUFFER_SIZE), type.mode(), type.format());
    }

    /** Uploads and draws the geometry accumulated in the buffer started by {@link #begin(RenderType)}. */
    public static void draw(BufferBuilder builder) {
        RenderType type = CURRENT.get();
        MeshData mesh = builder.build();
        if (mesh == null || type == null) return;
        type.draw(mesh);
    }
}
