package cc.silk.mixin;

import cc.silk.module.modules.render.OutlineESP;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        OutlineESP outlineESP = OutlineESP.getInstance();
        if (outlineESP != null && outlineESP.isEnabled()) {
            Entity self = (Entity) (Object) this;
            if (outlineESP.shouldEntityGlow(self)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"))
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
    }
}
