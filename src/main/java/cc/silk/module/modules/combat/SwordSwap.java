package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.mc.InventoryUtil;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SwordSwap extends Module {

    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 10, 100, 30, 1);

    private int originalSlot = -1;
    private boolean shouldSwitchBack = false;
    private long switchTime = 0;
    private boolean attackPressedLastTick = false;

    public SwordSwap() {
        super("Sword Swap", "Switches to sword when attacking with any non-axe/mace item", Category.COMBAT);
        addSettings(switchDelay);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (isNull()) return;

        if (shouldSwitchBack && System.currentTimeMillis() - switchTime >= switchDelay.getValue()) {
            if (originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                originalSlot = -1;
            }
            shouldSwitchBack = false;
        }

        boolean attackPressed = mc.options.keyAttack.isDown();

        if (attackPressed && !attackPressedLastTick) {
            var heldItem = mc.player.getMainHandItem().getItem();
            boolean isAxe = heldItem instanceof AxeItem;
            boolean isMace = heldItem instanceof MaceItem;
            boolean isSword = InventoryUtil.isSword(heldItem);

            if (!isAxe && !isMace && !isSword) {
                HitResult hitResult = mc.hitResult;
                if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                    var entity = ((EntityHitResult) hitResult).getEntity();
                    if (entity != null && !(entity instanceof EndCrystal)) {
                        int swordSlot = findSwordSlot();
                        if (swordSlot != -1) {
                            originalSlot = mc.player.getInventory().getSelectedSlot();
                            mc.player.getInventory().setSelectedSlot(swordSlot);
                            ((MinecraftClientAccessor) mc).invokeDoAttack();
                            switchTime = System.currentTimeMillis();
                            shouldSwitchBack = true;
                        }
                    }
                }
            }
        }

        attackPressedLastTick = attackPressed;
    }

    private int findSwordSlot() {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtil.isSword(mc.player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        if (originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
        shouldSwitchBack = false;
        attackPressedLastTick = false;
    }
}