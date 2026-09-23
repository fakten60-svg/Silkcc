package cc.silk.utils.mc;

import lombok.experimental.UtilityClass;

import static cc.silk.SilkClient.mc;

@UtilityClass
public final class MovementUtil {

    public static boolean isMoving() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }
}
