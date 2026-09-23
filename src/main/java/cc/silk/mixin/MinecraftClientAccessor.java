package cc.silk.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Graph
 */
@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {
    @Accessor("rightClickDelay")
    void setItemUseCooldown(int cooldown);

    @Accessor("mouseHandler")
    MouseHandler getMouse();

    @Invoker("startUseItem")
    void invokeDoItemUse();

    @Invoker("startAttack")
    boolean invokeDoAttack();
}