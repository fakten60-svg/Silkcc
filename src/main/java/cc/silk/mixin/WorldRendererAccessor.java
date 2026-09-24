package cc.silk.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Kept as an empty marker mixin.
 *
 * <p>Minecraft 26.1.2 no longer stores a {@code Frustum} on {@link LevelRenderer} - the frustum is now
 * created per frame and passed down as a method argument, so the previous {@code @Accessor("frustum")}
 * could not be resolved and made the game crash on startup.</p>
 */
@Mixin(LevelRenderer.class)
public interface WorldRendererAccessor {
}
