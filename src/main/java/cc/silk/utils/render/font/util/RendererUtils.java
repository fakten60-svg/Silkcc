package cc.silk.utils.render.font.util;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>Utils for rendering in minecraft</p>
 */
@SuppressWarnings("unused")
public class RendererUtils {
    @ApiStatus.Internal
    public static final Matrix4f lastProjMat = new Matrix4f();
    @ApiStatus.Internal
    public static final Matrix4f lastModMat = new Matrix4f();
    @ApiStatus.Internal
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    private static final FastMStack empty = new FastMStack();
    private static final Minecraft client = Minecraft.getInstance();
    private static final char RND_START = 'a';
    private static final char RND_END = 'z';
    private static final Random RND = new Random();

    /**
     * <p>Sets up rendering and resets everything that should be reset</p>
     */
    public static void setupRender() {
        
        
        
        
    }

    /**
     * <p>Reverts everything back to normal after rendering</p>
     */
    public static void endRender() {
        
        
        
    }

    /**
     * <p>Linear interpolation between two integers</p>
     *
     * @param from  Range from
     * @param to    Range to
     * @param delta Range delta
     * @return The interpolated value between from and to
     */
    public static int lerp(int from, int to, double delta) {
        return (int) Math.floor(from + (to - from) * Mth.clamp(delta, 0, 1));
    }

    /**
     * <p>Linear interpolation between two doubles</p>
     *
     * @param from  Range from
     * @param to    Range to
     * @param delta Range delta
     * @return The interpolated value between from and to
     */
    public static double lerp(double from, double to, double delta) {
        return from + (to - from) * Mth.clamp(delta, 0, 1);
    }

    /**
     * <p>Linear interpolation between two colors</p>
     *
     * @param a Color range from
     * @param b Color range to
     * @param c Range delta
     * @return The interpolated color
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static Color lerp(@NonNull Color a, @NonNull Color b, double c) {
        return new Color(lerp(a.getRed(), b.getRed(), c), lerp(a.getGreen(), b.getGreen(), c),
                lerp(a.getBlue(), b.getBlue(), c), lerp(a.getAlpha(), b.getAlpha(), c));
    }

    /**
     * <p>Modifies a color</p>
     * <p>Any of the components can be set to -1 to keep them from the original color</p>
     *
     * @param original       The original color
     * @param redOverwrite   The new red component
     * @param greenOverwrite The new green component
     * @param blueOverwrite  The new blue component
     * @param alphaOverwrite The new alpha component
     * @return The new color
     */
    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static Color modify(@NonNull Color original, int redOverwrite, int greenOverwrite, int blueOverwrite, int alphaOverwrite) {
        return new Color(
                redOverwrite == -1 ? original.getRed() : redOverwrite,
                greenOverwrite == -1 ? original.getGreen() : greenOverwrite,
                blueOverwrite == -1 ? original.getBlue() : blueOverwrite,
                alphaOverwrite == -1 ? original.getAlpha() : alphaOverwrite
        );
    }

    /**
     * <p>Translates a Vec3's position with a PoseStack</p>
     *
     * @param stack The PoseStack to translate with
     * @param in    The Vec3 to translate
     * @return The translated Vec3
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static Vec3 translateVec3dWithMatrixStack(@NonNull PoseStack stack, @NonNull Vec3 in) {
        Matrix4f matrix = stack.last().pose();
        Vector4f vec = new Vector4f((float) in.x, (float) in.y, (float) in.z, 1);
        vec.mul(matrix);
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    /**
     * <p>Registers a BufferedImage as Identifier, to be used in future render calls</p>
     * <p><strong>WARNING:</strong> This will wait for the main tick thread to register the texture, keep in mind that the texture will not be available instantly</p>
     * <p><strong>WARNING 2:</strong> This will throw an exception when called when the OpenGL context is not yet made</p>
     *
     * @param i  The identifier to register the texture under
     * @param bi The BufferedImage holding the texture
     */
    public static void registerBufferedImageTexture(@NonNull Identifier i, @NonNull BufferedImage bi) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", out);
            byte[] bytes = out.toByteArray();

            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            data.flip();
            DynamicTexture tex = new DynamicTexture(() -> "silk_temp", NativeImage.read(data));
            Minecraft.getInstance()
                    .execute(() -> Minecraft.getInstance().getTextureManager().register(i, tex));
        } catch (Exception e) { // should never happen, but just in case
            e.printStackTrace();
        }
    }

    /**
     * Gets an empty matrix stack without having to initialize the object
     *
     * @return An empty matrix stack
     */
    public static PoseStack getEmptyMatrixStack() {
        if (!empty.isEmpty()) {
            throw new IllegalStateException(
                    "Supposed \"empty\" stack is not actually empty; someone does not clean up after themselves.");
        }
        empty.setIdentity(); // reset top to identity, in case someone modified it
        return empty;
    }

    /**
     * Gets the position of the crosshair of the player, transformed into level space
     *
     * @return The position of the crosshair of the player, transformed into level space
     */
    @Contract("-> new")
    public static Vec3 getCrosshairVector() {
        Camera camera = client.gameRenderer.getMainCamera();

        float pi = (float) Math.PI;
        float yawRad = (float) Math.toRadians(-camera.yRot());
        float pitchRad = (float) Math.toRadians(-camera.xRot());
        float f1 = Mth.cos(yawRad - pi);
        float f2 = Mth.sin(yawRad - pi);
        float f3 = -Mth.cos(pitchRad);
        float f4 = Mth.sin(pitchRad);

        return new Vec3(f2 * f3, f4, f1 * f3).add(camera.position());
    }

    /**
     * Transforms an input position into a (x, y, d) coordinate, transformed to screen space. d specifies the far plane of the position, and can be used to check if the position is on screen. Use {@link #screenSpaceCoordinateIsVisible(Vec3)}.
     * Example:
     * <pre>
     * {@code
     * // Hud render event
     * Vec3 targetPos = new Vec3(100, 64, 100); // world space
     * Vec3 screenSpace = RendererUtils.worldSpaceToScreenSpace(targetPos);
     * if (RendererUtils.screenSpaceCoordinateIsVisible(screenSpace)) {
     *     // do something with screenSpace.x and .y
     * }
     * }
     * </pre>
     *
     * @param pos The level space coordinates to translate
     * @return The (x, y, d) coordinates
     * @throws NullPointerException If {@code pos} is null
     */
    @Contract(value = "_ -> new", pure = true)
    public static Vec3 worldSpaceToScreenSpace(@NonNull Vec3 pos) {
        Camera camera = client.getEntityRenderDispatcher().camera;
        int displayHeight = client.getMainRenderTarget().height;
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.position().x;
        double deltaY = pos.y - camera.position().y;
        double deltaZ = pos.z - camera.position().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.f).mul(
                lastWorldSpaceMatrix);

        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);

        matrixProj.mul(matrixModel)
                .project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport,
                        target);

        return new Vec3(target.x / client.getWindow().getGuiScale(),
                (displayHeight - target.y) / client.getWindow().getGuiScale(), target.z);
    }

    /**
     * Checks if a screen space coordinate (x, y, d) is on screen
     *
     * @param pos The (x, y, d) coordinates to check
     * @return True if the coordinates are visible
     */
    public static boolean screenSpaceCoordinateIsVisible(Vec3 pos) {
        return pos != null && pos.z > -1 && pos.z < 1;
    }

    /**
     * Converts a (x, y, d) screen space coordinate back into a level space coordinate. Example:
     * <pre>
     * {@code
     * // World render event
     * Vec3 near = RendererUtils.screenSpaceToWorldSpace(100, 100, 0);
     * Vec3 far = RendererUtils.screenSpaceToWorldSpace(100, 100, 1);
     * // Ray-cast from near to far to get block or entity at (100, 100) screen space
     * }
     * </pre>
     *
     * @param x x
     * @param y y
     * @param d d
     * @return The level space coordinate
     */
    @Contract(value = "_,_,_ -> new", pure = true)
    public static Vec3 screenSpaceToWorldSpace(double x, double y, double d) {
        Camera camera = client.getEntityRenderDispatcher().camera;
        int displayHeight = client.getWindow().getGuiScaledHeight();
        int displayWidth = client.getWindow().getGuiScaledWidth();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);

        matrixProj.mul(matrixModel)
                .mul(lastWorldSpaceMatrix)
                .unproject((float) x / displayWidth * viewport[2],
                        (float) (displayHeight - y) / displayHeight * viewport[3], (float) d, viewport, target);

        return new Vec3(target.x, target.y, target.z).add(camera.position());
    }

    /**
     * Returns the GUI scale of the current window
     *
     * @return The GUI scale of the current window
     */
    public static int getGuiScale() {
        return (int) Minecraft.getInstance().getWindow().getGuiScale();
    }

    private static String randomString() {
        return IntStream.range(0, 32)
                .mapToObj(operand -> String.valueOf((char) RND.nextInt(RND_START, RND_END + 1)))
                .collect(Collectors.joining());
    }

    /**
     * Returns an identifier in the renderer namespace, with a random id
     *
     * @return The identifier
     */
    @Contract(value = "-> new", pure = true)
    public static Identifier randomIdentifier() {
        return Identifier.fromNamespaceAndPath("renderer", "temp/" + randomString());
    }
}