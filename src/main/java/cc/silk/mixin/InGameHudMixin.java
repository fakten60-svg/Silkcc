package cc.silk.mixin;

import cc.silk.SilkClient;
import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Inject(method = "extractRenderState", at = @At(value = "TAIL"))
    private void onRender(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (SilkClient.INSTANCE == null) return;

        RenderUtils.unscaledProjection();
        RenderUtils.scaledProjection();
        
        
        
        
        
        
        SilkClient.INSTANCE.getSilkEventBus()
                .post(new Render2DEvent(context, context.guiWidth(), context.guiHeight()));
        
        
        
    }
}
