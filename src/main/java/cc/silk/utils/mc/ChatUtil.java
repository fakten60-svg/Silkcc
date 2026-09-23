package cc.silk.utils.mc;

import cc.silk.SilkClient;
import cc.silk.utils.IMinecraft;
import cc.silk.utils.other.StringUtils;
import cc.silk.utils.render.ColorUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;

import java.awt.*;
import java.util.Objects;

@UtilityClass
public final class ChatUtil implements IMinecraft {

    private static final MutableComponent BRACKET_COLOR = Component.empty().setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY));

    public static void infoChatMessage(final String message) {
        ChatUtil.infoChatMessage(Component.literal(message));
    }

    public static void infoChatMessage(final Component message) {
        ChatUtil.chatMessage(message.copy().withColor(Type.INFO.color()), true);
    }

    public static void warningChatMessage(final String message) {
        ChatUtil.warningChatMessage(Component.literal(message));
    }

    public static void warningChatMessage(final Component message) {
        if (Objects.isNull(mc.gui)) {
            SilkClient.INSTANCE.getLogger().warn(message.getString());
            return;
        }
        ChatUtil.chatMessage(message.copy().withColor(Type.WARNING.color()), true);
    }

    public static void errorChatMessage(final String message) {
        ChatUtil.errorChatMessage(Component.literal(message));
    }

    public static void errorChatMessage(final Component message) {
        if (Objects.isNull(mc.gui)) {
            SilkClient.INSTANCE.getLogger().error(message.getString());
            return;
        }
        ChatUtil.chatMessage(message.copy().withColor(Type.ERROR.color()), true);
    }


    public static void emptyChatMessage(final boolean prefix) {
        ChatUtil.chatMessage(Component.literal(" "), prefix);
    }

    public static void chatMessage(final String message) {
        ChatUtil.chatMessage(Component.literal(message));
    }

    public static void chatMessage(final MutableComponent message) {
        ChatUtil.chatMessage(message, true);
    }

    public static void chatMessage(final String message, final boolean prefix) {
        ChatUtil.chatMessage(Component.literal(message), prefix);
    }

    public static void chatMessage(final MutableComponent message, final boolean prefix) {
        if (Objects.isNull(mc.gui)) {
            SilkClient.INSTANCE.getLogger().info(message.getString());
            return;
        }

        final MutableComponent text = prefix ? ChatUtil.chatPrefix().copy().append(message) : message;
        mc.gui.getChat().addClientSystemMessage(text);
    }

    public static void addChatMessage(String text) {
        if (mc.player == null || mc.level == null || Objects.isNull(mc.gui) || mc.gui.getChat() == null) {
            SilkClient.INSTANCE.getLogger().info("[Silk] " + text);
            return;
        }
        mc.gui.getChat().addClientSystemMessage(Component.nullToEmpty(text));
    }

    public static MutableComponent colorFade(final String text, final Style style, final Color startColor, final Color endColor) {
        final MutableComponent mutableText = Component.empty();

        for (int i = 0; i < text.length(); i++) {
            final float percent = (float) i / (text.length() - 1);
            final Color color = ColorUtils.colorInterpolate(startColor, endColor, percent);

            mutableText.append(Component.literal(String.valueOf(text.charAt(i))).setStyle(style.withColor(TextColor.fromRgb(color.getRGB()))));
        }

        return mutableText;
    }

    public static MutableComponent chatPrefix() {
        return BRACKET_COLOR.copy()
                .append("[")
                .append(ChatUtil.colorFade("Silk", Style.EMPTY, new Color(0, 191, 255), new Color(0, 255, 127)))
                .append("]")
                .append(" ");
    }

    public enum Type {
        INFO(Color.GREEN),
        WARNING(Color.ORANGE),
        ERROR(Color.RED);

        private final int color;
        private final String name;

        Type(final Color color) {
            this.color = color.getRGB();
            this.name = StringUtils.normalizeEnumName(this.name());
        }

        public int color() {
            return this.color;
        }

        public String display() {
            return this.name;
        }
    }
}
