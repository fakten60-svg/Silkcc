package cc.silk.mixin;

import cc.silk.gui.ClickGui;
import cc.silk.gui.newgui.NewClickGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow
    @Nullable
    protected Minecraft minecraft;

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void renderBackgroundInject(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (minecraft == null) return;
        Screen screen = minecraft.screen;
        if (screen instanceof ClickGui) {
            ci.cancel();
            return;
        }
        if (screen instanceof NewClickGUI && !cc.silk.module.modules.client.ClientSettingsModule.isGuiBlurEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRenderTail(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (minecraft == null) return;
        Screen screen = minecraft.screen;
        if (screen instanceof NewClickGUI || screen instanceof ClickGui) {
            
            
            
            
            
            
            
            
            
            
        }
    }
}
