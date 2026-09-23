package cc.silk.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * World-space to screen-space projection.
 * <p>
 * Minecraft 26.1.2 no longer exposes the projection matrix through {@code RenderSystem}, so it is
 * rebuilt from the camera FOV and the window aspect ratio. The world-to-view matrix is still available
 * through {@link RenderSystem#getModelViewMatrix()}.
 */
public class W2SUtil {
    private static final float FAR_PLANE = 1024.0f;

    public static Matrix4f matrixProject = new Matrix4f();
    public static Matrix4f matrixModel = new Matrix4f();
    public static Matrix4f matrixWorldSpace = new Matrix4f();

    private W2SUtil() {
    }

    private static Matrix4f projectionMatrix(Minecraft mc) {
        Camera camera = mc.gameRenderer.getMainCamera();
        int width = Math.max(1, mc.getWindow().getWidth());
        int height = Math.max(1, mc.getWindow().getHeight());
        float aspect = (float) width / (float) height;
        return new Matrix4f().perspective((float) Math.toRadians(camera.getFov()), aspect,
                Camera.PROJECTION_Z_NEAR, FAR_PLANE);
    }

    public static Vec3 getCoords(Vec3 vector) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            return null;
        }
        return getCoords(vector, mc.gameRenderer.getMainCamera().position());
    }

    public static Vec3 getCoords(Vec3 vector, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }

        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        matrixProject.set(projectionMatrix(mc));
        matrixModel.set(RenderSystem.getModelViewMatrix());
        matrixWorldSpace.set(RenderSystem.getModelViewMatrix());

        Matrix4f mvp = new Matrix4f(matrixProject).mul(matrixModel);
        Vector3f transformed3 = new Vector3f((float) vector.x, (float) vector.y, (float) vector.z);
        Vector3f target = new Vector3f();
        mvp.project(transformed3, viewport, target);

        float scale = (float) mc.getWindow().getGuiScale();
        return new Vec3(target.x / scale, (displayHeight - target.y) / scale, target.z);
    }
}
