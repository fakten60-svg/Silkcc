package cc.silk.utils.mc;

import cc.silk.utils.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

@UtilityClass
public final class InventoryUtil implements IMinecraft {
    public static void swapToSlot(Item item) {
        for (byte i = 0; i < 9; i++) {
            assert mc.player != null;
            var stack = mc.player.getInventory().getItem(i);

            if (stack.isEmpty()) continue;
            if (stack.getItem().equals(item)) {
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    public static boolean hasItem(Item item) {
        if (mc.player == null) return false;
        for (byte i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasWeapon(Class<? extends Item> weaponClass) {
        if (mc.player == null) return false;
        for (byte i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (weaponClass.isInstance(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    public static void swapToWeapon(Class<? extends Item> weaponClass) {
        if (mc.player == null) return;
        for (byte i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (weaponClass.isInstance(stack.getItem())) {
                mc.player.getInventory().setSelectedSlot(i);
                break;
            }
        }
    }

    /**
     * Minecraft 26.1.2 removed {@code SwordItem}/{@code DiggerItem} and made weapon behaviour data
     * driven, so tools must be classified through their item tags instead of their Java class.
     */
    public static boolean isSword(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.typeHolder().is(ItemTags.SWORDS);
    }

    public static boolean isSword(Item item) {
        return item != null && new ItemStack(item).typeHolder().is(ItemTags.SWORDS);
    }

    public static boolean isMiningTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.typeHolder().is(ItemTags.PICKAXES)
                || stack.typeHolder().is(ItemTags.SHOVELS)
                || stack.typeHolder().is(ItemTags.AXES)
                || stack.typeHolder().is(ItemTags.HOES);
    }

    public static void swapToSword() {
        if (mc.player == null) return;
        for (byte i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            if (isSword(mc.player.getInventory().getItem(i))) {
                mc.player.getInventory().setSelectedSlot(i);
                break;
            }
        }
    }

}

