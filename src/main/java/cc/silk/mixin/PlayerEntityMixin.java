package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.module.modules.movement.KeepSprint;
import cc.silk.module.modules.player.FastMine;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Player.class)
public class PlayerEntityMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void modifyBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        if (SilkClient.INSTANCE == null) return;

        Optional<FastMine> optionalModule = SilkClient.INSTANCE.getModuleManager().getModule(FastMine.class);
        if (optionalModule.isEmpty()) return;

        FastMine fastMine = optionalModule.get();
        if (!fastMine.isEnabled()) return;

        Player player = (Player) (Object) this;
        if (player != Minecraft.getInstance().player) return;

        float modifiedSpeed = cir.getReturnValue() * fastMine.getSpeed();
        cir.setReturnValue(modifiedSpeed);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    private void attackInject(Entity target, CallbackInfo ci) {
        Optional<KeepSprint> keep = SilkClient.INSTANCE.getModuleManager().getModule(KeepSprint.class);
        KeepSprint keepSprint = keep.get();
        if (keepSprint.isEnabled()) {
            ci.cancel();
        }
    }
}
