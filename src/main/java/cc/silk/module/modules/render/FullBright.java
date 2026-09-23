package cc.silk.module.modules.render;

import cc.silk.module.Category;
import cc.silk.module.Module;
import net.minecraft.client.OptionInstance;

public class FullBright extends Module {
    private Double previousGamma = null;

    public FullBright() {
        super("Full Bright", "Removes darkness", Category.RENDER);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc != null && mc.options != null) {
            try {
                OptionInstance<Double> gamma = mc.options.gamma();
                previousGamma = gamma.get();
                gamma.set(1.0);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc != null && mc.options != null && previousGamma != null) {
            try {
                mc.options.gamma().set(previousGamma);
            } catch (Throwable ignored) {
            }
        }
    }
}
