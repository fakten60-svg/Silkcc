package cc.silk.utils.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;

public final class CompatRenderDispatcher {
    private CompatRenderDispatcher() {}

    public static void render(EntityRenderDispatcher dispatcher, Entity entity, double x, double y, double z,
                              float yaw, float tickDelta, PoseStack matrices,
                              MultiBufferSource provider, int light) {
        try {
            // Newer signature without tickDelta
            dispatcher.getClass()
                    .getMethod("render", Entity.class, double.class, double.class, double.class,
                            float.class, PoseStack.class, MultiBufferSource.class, int.class)
                    .invoke(dispatcher, entity, x, y, z, yaw, matrices, provider, light);
            return;
        } catch (NoSuchMethodException ignored) {
            // fall through to try older signature
        } catch (Throwable ignored) {
        }

        try {
            // Older signature with tickDelta
            dispatcher.getClass()
                    .getMethod("render", Entity.class, double.class, double.class, double.class,
                            float.class, float.class, PoseStack.class, MultiBufferSource.class, int.class)
                    .invoke(dispatcher, entity, x, y, z, yaw, tickDelta, matrices, provider, light);
        } catch (Throwable ignored) {
            // give up quietly
        }
    }
}


