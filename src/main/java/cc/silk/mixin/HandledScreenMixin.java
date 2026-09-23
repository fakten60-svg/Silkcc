package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.module.modules.render.ContainerSlots;
import cc.silk.utils.render.font.FontManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cc.silk.SilkClient.mc;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {
        @Inject(method = "extractSlot", at = @At("TAIL"))
    public void postDrawSlot(GuiGraphicsExtractor context, Slot slot, int i, int j, CallbackInfo ci) {
        if (!SilkClient.INSTANCE.moduleManager.getModule(ContainerSlots.class).get().isEnabled()) return;

        if (ContainerSlots.highlightTotem.getValue() && slot.hasItem()) {
            if (slot.getItem().getItem() == Items.TOTEM_OF_UNDYING) {
                context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, ContainerSlots.highlightColor.getValue().getRGB());
            }
        }

        if (ContainerSlots.disableText.getValue()) return;

        if (ContainerSlots.fontMode.isMode("Inter")) {
            context.text(
                    mc.font,
                    String.valueOf(slot.getContainerSlot()),
                    slot.x,
                    slot.y,
                    ContainerSlots.color.getValue().getRGB(),
                    false
            );
        } else {
            context.text(
                    mc.font,
                    String.valueOf(slot.getContainerSlot()),
                    slot.x,
                    slot.y,
                    ContainerSlots.color.getValue().getRGB(),
                    false
            );
        }
    }
}
