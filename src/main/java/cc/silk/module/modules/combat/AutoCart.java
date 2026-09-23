package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

public final class AutoCart extends Module {

    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
    
    private boolean isActive = false;
    private BlockPos targetPos = null;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private boolean hasRail = false;
    private boolean hasTntCart = false;

    public AutoCart() {
        super("Auto Cart", "Places TNT minecarts on rails when shooting arrows", -1, Category.COMBAT);
        this.addSettings(autoSwitch);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;

        if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() == Items.BOW) {
            if (!isActive) {
                startPlacing();
            }
        } else if (isActive && !mc.player.isUsingItem()) {
            if (tickCounter == 0) {
                tickCounter = 1;
            }
        }

        if (!isActive) return;

        if (tickCounter == 1) {
            placeRail();
            tickCounter = 2;
        } else if (tickCounter == 2) {
            placeTntCart();
            stopPlacing();
        }
    }

    private void startPlacing() {
        if (isActive) return;

        if (mc.player.getMainHandItem().getItem() != Items.BOW) {
            return;
        }

        BlockPos targetPos = getTargetPosition();
        if (targetPos == null) return;

        this.targetPos = targetPos;
        isActive = true;
        tickCounter = 0;
        originalSlot = mc.player.getInventory().getSelectedSlot();
    }


    private void stopPlacing() {
        if (!isActive) return;

        if (autoSwitch.getValue() && originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }

        resetState();
    }

    private void resetState() {
        isActive = false;
        targetPos = null;
        originalSlot = -1;
        tickCounter = 0;
        hasRail = false;
        hasTntCart = false;
    }

    private void placeRail() {
        if (hasRail) return;

        int railSlot = findAnyRailInHotbar();
        if (railSlot == -1) return;

        mc.player.getInventory().setSelectedSlot(railSlot);
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        hasRail = true;
    }

    private void placeTntCart() {
        if (hasTntCart) {
            stopPlacing();
            return;
        }

        int tntCartSlot = findItemInHotbar();
        if (tntCartSlot == -1) return;

        mc.player.getInventory().setSelectedSlot(tntCartSlot);
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        hasTntCart = true;
    }

    private BlockPos getTargetPosition() {
        HitResult hitResult = mc.hitResult;
        if (hitResult == null) return null;
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        } else if (hitResult.getType() == HitResult.Type.ENTITY) {
            return mc.player.blockPosition().offset(0, 1, 0);
        } else {
            Vec3 cameraPos = mc.player.getEyePosition(1.0f);
            Vec3 rotation = mc.player.getViewVector(1.0f);
            Vec3 end = cameraPos.add(rotation.scale(5.0));
            
            BlockHitResult blockHit = mc.level.clip(new ClipContext(cameraPos, end, 
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
            
            if (blockHit != null) {
                return blockHit.getBlockPos().relative(blockHit.getDirection());
            } else {
                return mc.player.blockPosition().offset(0, 1, 0);
            }
        }
    }

    private int findItemInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.TNT_MINECART) {
                return i;
            }
        }
        return -1;
    }

    private int findAnyRailInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && isRail(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isRail(Item item) {
        return item == Items.RAIL || 
               item == Items.POWERED_RAIL || 
               item == Items.DETECTOR_RAIL || 
               item == Items.ACTIVATOR_RAIL;
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        if (isActive) {
            stopPlacing();
        }
    }
}
