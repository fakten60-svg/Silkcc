package cc.silk.utils.keybinding.simulation;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

import static cc.silk.SilkClient.shouldUseMouseEvent;

@UtilityClass
public final class ClickSimulator {
    public static void leftClick() {
        if (shouldUseMouseEvent) {
            User32.INSTANCE.mouse_event(User32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0);
            User32.INSTANCE.mouse_event(User32.MOUSEEVENTF_LEFTUP, 0, 0, 0, 0);
        } else {
            Minecraft mc = Minecraft.getInstance();
            KeyMapping attack = mc.options.keyAttack;

            InputConstants.Key key = attack.getDefaultKey();

            KeyMapping.set(key, true);
            KeyMapping.click(key);

            new Thread(() -> {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {
                }
                KeyMapping.set(key, false);
            }).start();

        }
    }
}