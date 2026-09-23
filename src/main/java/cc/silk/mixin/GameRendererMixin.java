package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.event.impl.render.Render3DEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * Minecraft 26.1.2 removed the global projection/model-view matrix capture
     * ({@code RenderSystem.getProjectionMatrix()} and the shared model-view stack that used to be mutated
     * here), so the matrices are now derived on demand in {@link cc.silk.utils.render.W2SUtil}.
     */
    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorldTail(DeltaTracker tickCounter, CallbackInfo ci) {
        SilkClient.INSTANCE.getSilkEventBus().post(new Render3DEvent(new PoseStack()));
    }
}
