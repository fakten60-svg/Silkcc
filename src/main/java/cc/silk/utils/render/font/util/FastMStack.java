package cc.silk.utils.render.font.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.SneakyThrows;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A reimplementation of {@link PoseStack}, containing a few optimizations.
 */
public class FastMStack extends PoseStack {
    private static final MethodHandle MATRIXSTACK_ENTRY_CTOR;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(PoseStack.Pose.class,
                    MethodHandles.lookup());
            MATRIXSTACK_ENTRY_CTOR = lookup.findConstructor(PoseStack.Pose.class,
                    MethodType.methodType(void.class, Matrix4f.class, Matrix3f.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final ObjectArrayList<Entry> fEntries = new ObjectArrayList<>(8);
    private Entry top;

    public FastMStack() {
        fEntries.add(top = new Entry(new Matrix4f(), new Matrix3f()));
    }

    @Override
    public void translate(double x, double y, double z) {
        top.positionMatrix.translate((float) x, (float) y, (float) z);
    }

    @Override
    public void scale(float x, float y, float z) {
        top.positionMatrix.scale(x, y, z);
        if (x == y && y == z) {
            if (x != 0) {
                top.normalMatrix.scale(Math.signum(x));
            }
            return;
        }
        float inverseX = 1.0f / x;
        float inverseY = 1.0f / y;
        float inverseZ = 1.0f / z;
        float scalar = (float) (1f / Math.cbrt(inverseX * inverseY * inverseZ));
        top.normalMatrix.scale(scalar * inverseX, scalar * inverseY, scalar * inverseZ);
    }

    @Override
    public void mulPose(org.joml.Quaternionfc quaternion) {
        top.positionMatrix.rotate(quaternion);
        top.normalMatrix.rotate((Quaternionf) quaternion);
    }

    @Override
    public void rotateAround(org.joml.Quaternionfc quaternion, float originX, float originY, float originZ) {
        top.positionMatrix.rotateAround(quaternion, originX, originY, originZ);
        top.normalMatrix.rotate((Quaternionf) quaternion);
    }

    @Override
    public void mulPose(org.joml.Matrix4fc matrix) {
        top.positionMatrix.mul(matrix);
    }

    @Override
    public void pushPose() {
        fEntries.add(top = new Entry(new Matrix4f(top.positionMatrix), new Matrix3f(top.normalMatrix)));
    }

    @Override
    public void popPose() {
        if (fEntries.size() == 1) {
            throw new IllegalStateException("Trying to pop an empty stack");
        }
        fEntries.pop();
        top = fEntries.top();
    }

    @SneakyThrows
    @Override
    public PoseStack.Pose last() {
        return (PoseStack.Pose) MATRIXSTACK_ENTRY_CTOR.invoke(top.positionMatrix, top.normalMatrix);
    }

    @Override
    public boolean isEmpty() {
        return fEntries.size() == 1;
    }

    @Override
    public void setIdentity() {
        top.positionMatrix.identity();
        top.normalMatrix.identity();
    }

    record Entry(Matrix4f positionMatrix, Matrix3f normalMatrix) {

        @Override
        public @NotNull String toString() {
            return "Entry[" +
                    "positionMatrix=" + positionMatrix + ", " +
                    "normalMatrix=" + normalMatrix + ']';
        }

    }
}
