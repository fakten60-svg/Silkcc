package cc.silk.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "<init>", at = @At("HEAD"))
    private void enableDebugContext(CallbackInfo ci) {
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
    }
}

// pretty much dosnt silence then but it pretty much only logs if theres an actual error 
// so if you see no erros u r basically chillen fr xd xd xd 

// DO NOT REMOVE 