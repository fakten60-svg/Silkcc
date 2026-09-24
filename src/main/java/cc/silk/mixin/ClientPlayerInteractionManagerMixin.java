package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.event.impl.player.AttackEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void attackEntityInject(Player player, Entity target, CallbackInfo callbackInfo) {
        if (SilkClient.INSTANCE == null) return;
        SilkClient.INSTANCE.getSilkEventBus().post(new AttackEvent(target));
    }
}
