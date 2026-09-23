package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.module.Module;
import cc.silk.utils.keybinding.KeyUtils;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private Minecraft client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (window == this.client.getWindow().handle()) {
            if (this.client.screen == null) {
                for (Module module : SilkClient.INSTANCE.moduleManager.getModules()) {
                    if (key == module.getKey()) {
                        if (module.getKeybindSetting().isHoldMode()) {
                            if (action == GLFW.GLFW_PRESS && !module.isEnabled()) {
                                module.setEnabled(true);
                            } else if (action == GLFW.GLFW_RELEASE && module.isEnabled()) {
                                module.setEnabled(false);
                            }
                        } else {
                            if (action == GLFW.GLFW_PRESS && KeyUtils.isKeyPressed(key)) {
                                module.toggle();
                            }
                        }
                    }
                }
            }
        }
    }
}
