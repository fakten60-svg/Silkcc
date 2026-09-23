package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.event.impl.input.MouseClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"))
    private void onMouseButton(long window, net.minecraft.client.input.MouseButtonInfo button, int action, CallbackInfo ci) {
        if (SilkClient.INSTANCE == null) return;
        if (window != minecraft.getWindow().handle()) return;
        if (minecraft.screen != null) return;

        int btn = button != null ? button.button() : -1;
        int mods = button != null ? button.modifiers() : 0;
        MouseClickEvent event = new MouseClickEvent(btn, action, mods);
        SilkClient.INSTANCE.getSilkEventBus().post(event);
    }
}
