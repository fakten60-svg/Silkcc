package cc.silk.utils.render.font;


import cc.silk.utils.render.font.fonts.FontRenderer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches {@link FontRenderer} instances per size and font family.
 * <p>
 * Minecraft 26.1.2's GUI rewrite removed the immediate-mode glyph atlas, so text is drawn with the
 * vanilla font and only the requested pixel size is kept. The bundled {@code .ttf} resources are no
 * longer rasterised.
 */
public class FontManager {

    private final Map<FontKey, FontRenderer> fontCache = new HashMap<>();

    public void initialize() {
        for (Type type : Type.values()) {
            for (int size = 4; size <= 32; size++) {
                fontCache.put(new FontKey(size, type), create(size, type.getType()));
            }
        }
    }

    public FontRenderer create(float size, String name) {
        return new FontRenderer(size);
    }

    public FontRenderer getSize(int size, Type type) {
        return fontCache.computeIfAbsent(new FontKey(size, type), k -> create(size, type.getType()));
    }

    @Getter
    public enum Type {
        Inter("Inter"),
        JetbrainsMono("JetbrainsMono"),
        Poppins("Poppins-Medium");

        private final String type;

        Type(String type) {
            this.type = type;
        }
    }

    private record FontKey(int size, Type type) {

        @Override
        public @NotNull String toString() {
            return "FontKey[" +
                    "size=" + size + ", " +
                    "type=" + type + ']';
        }

    }
}
