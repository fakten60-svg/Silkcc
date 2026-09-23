package cc.silk.event.impl.render;

import cc.silk.event.types.Event;
import lombok.Getter;
import com.mojang.blaze3d.vertex.PoseStack;

@Getter
public class Render3DEvent implements Event {
    PoseStack matrixStack;

    public Render3DEvent(PoseStack matrixStack) {
        this.matrixStack = matrixStack;
    }
}
