package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.module.Module;
import cc.silk.utils.keybinding.KeyUtils;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
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
    private Minecraft minecraft;


    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onPress(long window, int key, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
        if (window == this.minecraft.getWindow().handle()) {
            if (this.minecraft.screen == null) {
                for (Module module : SilkClient.INSTANCE.moduleManager.getModules()) {
                    if (key == module.getKey()) {
                        boolean isPressed = KeyUtils.isKeyPressed(key);
                        if (module.getKeybindSetting().isHoldMode()) {
                            if (isPressed && !module.isEnabled()) {
                                module.setEnabled(true);
                            } else if (!isPressed && module.isEnabled()) {
                                module.setEnabled(false);
                            }
                        } else {
                            if (isPressed) {
                                // prevent repeat toggle on hold: only toggle once per press handled via keyPress (which fires on press only)
                                module.toggle();
                            }
                        }
                    }
                }
            }
        }
    }
}
