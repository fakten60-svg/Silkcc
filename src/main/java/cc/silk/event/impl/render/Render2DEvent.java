package cc.silk.event.impl.render;

import cc.silk.event.types.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEvent implements Event {
    private GuiGraphicsExtractor context;
    private int width;
    private int height;

    public Render2DEvent(GuiGraphicsExtractor context, int width, int height) {
        this.context = context;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public GuiGraphicsExtractor getContext() {
        return context;
    }

    public void setContext(GuiGraphicsExtractor context) {
        this.context = context;
    }
}
