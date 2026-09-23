package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.AttackEvent;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.mc.EnchantmentUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class BreachSwap extends Module {

    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 10, 100, 30, 1);
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only on ground", true);
    private final BooleanSetting silentSwap = new BooleanSetting("Silent Swap", true);

    private int originalSlot = -1;
    private boolean shouldSwitchBack = false;
    private long switchTime = 0;
    private boolean isSwappingAttack = false;

    public BreachSwap() {
        super("Breach Swap", "Switches to a Breach enchanted mace when attacking", Category.COMBAT);
        addSettings(switchDelay, onlyOnGround, silentSwap);
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (isNull() || isSwappingAttack)
            return;
        if (onlyOnGround.getValue() && !mc.player.onGround())
            return;
        if (ShieldBreaker.breakingShield)
            return;
        if (!(event.getTarget() instanceof LivingEntity))
            return;

        int maceSlot = findBreachMaceSlot();
        if (maceSlot == -1)
            return;

        if (originalSlot == -1) {
            originalSlot = mc.player.getInventory().getSelectedSlot();
        }

        if (silentSwap.getValue()) {
            int prevSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(maceSlot);

            isSwappingAttack = true;
            ((MinecraftClientAccessor) mc).invokeDoAttack();
            isSwappingAttack = false;

            mc.player.getInventory().setSelectedSlot(prevSlot);
        } else {
            mc.player.getInventory().setSelectedSlot(maceSlot);

            isSwappingAttack = true;
            ((MinecraftClientAccessor) mc).invokeDoAttack();
            isSwappingAttack = false;

            shouldSwitchBack = true;
            switchTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (isNull())
            return;
        if (ShieldBreaker.breakingShield)
            return;

        if (shouldSwitchBack && System.currentTimeMillis() - switchTime >= switchDelay.getValue()) {
            if (originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                originalSlot = -1;
            }
            shouldSwitchBack = false;
        }

        if (mc.options.keyAttack.isDown()) {
            HitResult hitResult = mc.hitResult;
            if (hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity) {
                int maceSlot = findBreachMaceSlot();
                if (maceSlot != -1) {
                    if (originalSlot == -1) {
                        originalSlot = mc.player.getInventory().getSelectedSlot();
                    }

                    if (silentSwap.getValue()) {
                        int prevSlot = mc.player.getInventory().getSelectedSlot();
                        mc.player.getInventory().setSelectedSlot(maceSlot);

                        ((MinecraftClientAccessor) mc).invokeDoAttack();

                        mc.player.getInventory().setSelectedSlot(prevSlot);
                    } else {
                        mc.player.getInventory().setSelectedSlot(maceSlot);

                        ((MinecraftClientAccessor) mc).invokeDoAttack();

                        switchTime = System.currentTimeMillis();
                        shouldSwitchBack = true;
                    }
                }
            }
        }
    }

    private int findBreachMaceSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            Item item = stack.getItem();
            if (item instanceof MaceItem && hasBreach(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasBreach(ItemStack stack) {
        ResourceKey<net.minecraft.world.item.enchantment.Enchantment> breachKey = ResourceKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath("minecraft", "breach"));
        return EnchantmentUtil.hasEnchantment(stack, mc.level, breachKey);
    }

    @Override
    public void onDisable() {
        if (originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
        shouldSwitchBack = false;
        isSwappingAttack = false;
    }
}
