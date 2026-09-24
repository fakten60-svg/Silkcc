package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.event.impl.network.DisconnectEvent;
import cc.silk.event.impl.player.DoAttackEvent;
import cc.silk.event.impl.player.ItemUseEvent;
import cc.silk.utils.IMinecraft;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.event.impl.level.WorldChangeEvent;
import cc.silk.gui.ClickGui;
import cc.silk.module.modules.client.ClickGUIModule;
import cc.silk.module.modules.client.Client;
import cc.silk.profiles.ProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin implements IMinecraft {

    @Shadow
    public ClientLevel level;
    @Shadow
    public HitResult hitResult;
    @Shadow
    public LocalPlayer player;
    @Shadow
    public MultiPlayerGameMode gameMode;
    @Shadow
    @Final
    private DeltaTracker.Timer deltaTracker;

    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
    public void setTitle(CallbackInfoReturnable<String> cir) {
        if (SilkClient.INSTANCE == null || SilkClient.mc == null) return;

        var optionalClientModule = SilkClient.INSTANCE.getModuleManager().getModule(Client.class);
        if (optionalClientModule.isPresent()) {
            Client client = optionalClientModule.get();
            if (client.isEnabled() && client.getTitle()) {
                cir.setReturnValue("Silk 26.1.2");
            }
        }
    }

    @Inject(method = "run", at = @At("HEAD"))
    public void runInject(CallbackInfo ci) {
        if (SilkClient.INSTANCE != null) {
            ProfileManager profileManager = SilkClient.INSTANCE.getProfileManager();
            profileManager.loadProfile("default");
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (SilkClient.INSTANCE == null || SilkClient.mc == null) return;

        if (level != null) {
            SilkClient.INSTANCE.getSilkEventBus().post(new TickEvent());
        }

        var optionalClickGuiModule = SilkClient.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        if (optionalClickGuiModule.isPresent()) {
            ClickGUIModule clickGuiModule = optionalClickGuiModule.get();
            if (clickGuiModule.isEnabled() && SilkClient.mc.screen == null && level != null) {
                SilkClient.mc.setScreen(new ClickGui());
            }
            else if (!clickGuiModule.isEnabled() && SilkClient.mc.screen instanceof ClickGui) {
                SilkClient.mc.setScreen(null);
            }
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void doAttackInject(CallbackInfoReturnable<Boolean> cir) {
        try {
            var antiMissOpt = SilkClient.INSTANCE.getModuleManager().getModule(cc.silk.module.modules.combat.AntiMiss.class);
            if (antiMissOpt.isPresent() && antiMissOpt.get().isEnabled()) {
                if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }

        DoAttackEvent event = new DoAttackEvent();
        SilkClient.INSTANCE.getSilkEventBus().post(event);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stopInject(CallbackInfo ci) {
        if (SilkClient.INSTANCE != null) {
            ProfileManager profileManager = SilkClient.INSTANCE.getProfileManager();
            profileManager.saveProfile("default", true);
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    public void onWorldChangeInject(ClientLevel newWorld, CallbackInfo ci) {
        if (SilkClient.INSTANCE != null && SilkClient.mc != null) {
            SilkClient.INSTANCE.getSilkEventBus().post(new WorldChangeEvent(newWorld));
        }
    }
    @Inject(method = "disconnectFromWorld", at = @At("HEAD"))
    private void onDisconnected(net.minecraft.network.chat.Component reason, CallbackInfo ci) {
        DisconnectEvent event = new DisconnectEvent();
        if (SilkClient.INSTANCE != null) SilkClient.INSTANCE.getSilkEventBus().post(event);
    }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void doItemUseInject(CallbackInfo ci) {
        ItemUseEvent event = new ItemUseEvent();

        SilkClient.INSTANCE.getSilkEventBus().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
