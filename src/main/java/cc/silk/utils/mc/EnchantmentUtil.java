package cc.silk.utils.mc;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public final class EnchantmentUtil {

    private EnchantmentUtil() {
    }

    private static ItemEnchantments enchantments(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return stack.get(DataComponents.ENCHANTMENTS);
    }

    public static boolean hasEnchantment(ItemStack stack, Level level, Holder<Enchantment> entry) {
        if (level == null || entry == null) return false;
        ItemEnchantments ench = enchantments(stack);
        if (ench == null) return false;
        return ench.getLevel(entry) > 0;
    }

    public static boolean hasEnchantment(ItemStack stack, Level level, Enchantment enchantment) {
        if (level == null || enchantment == null) return false;
        ItemEnchantments ench = enchantments(stack);
        if (ench == null) return false;
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return registry.getResourceKey(enchantment)
                .flatMap(registry::get)
                .map(holder -> ench.getLevel(holder) > 0)
                .orElse(false);
    }

    public static boolean hasEnchantment(ItemStack stack, Level level, ResourceKey<Enchantment> enchantmentKey) {
        if (level == null || enchantmentKey == null) return false;
        ItemEnchantments ench = enchantments(stack);
        if (ench == null) return false;
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return registry.get(enchantmentKey)
                .map(holder -> ench.getLevel(holder) > 0)
                .orElse(false);
    }
}
