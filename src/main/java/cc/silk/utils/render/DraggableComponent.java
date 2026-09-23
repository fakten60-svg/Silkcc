package cc.silk.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class DraggableComponent {
    private float x;
    private float y;
    private float width;
    private float height;

    private boolean dragging = false;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    public DraggableComponent(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.mouseHandler == null || mc.getWindow() == null)
            return;

        if (mc.screen == null) {
            dragging = false;
            return;
        }

        double mouseX = mc.mouseHandler.getScaledXPos(mc.getWindow()) * mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.getScaledYPos(mc.getWindow()) * mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getHeight();

        boolean leftClick = GLFW.glfwGetMouseButton(mc.getWindow().handle(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (leftClick) {
            if (!dragging) {
                if (isMouseOver(mouseX, mouseY)) {
                    dragging = true;
                    dragOffsetX = (float) (mouseX - x);
                    dragOffsetY = (float) (mouseY - y);
                }
            }

            if (dragging) {
                x = (float) mouseX - dragOffsetX;
                y = (float) mouseY - dragOffsetY;

                x = Mth.clamp(x, 0, mc.getWindow().getGuiScaledWidth() - width);
                y = Mth.clamp(y, 0, mc.getWindow().getGuiScaledHeight() - height);
            }
        } else {
            dragging = false;
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public boolean isDragging() {
        return dragging;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
