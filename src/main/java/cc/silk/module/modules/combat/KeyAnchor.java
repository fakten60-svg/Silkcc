package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.mc.InventoryUtil;
import cc.silk.module.setting.KeybindSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.keybinding.KeyUtils;
import cc.silk.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class KeyAnchor extends Module {

    private final KeybindSetting anchorKeybind = new KeybindSetting("Anchor Key", GLFW.GLFW_KEY_X, false);
    private final NumberSetting delay = new NumberSetting("Delay (MS)", 1, 500, 50, 1);
    private final NumberSetting restoreDelayTicks = new NumberSetting("Restore Delay", 1, 20, 2, 1);

    private final TimerUtil timer = new TimerUtil();
    private boolean keyPressed = false;
    private boolean isActive = false;
    private int originalSlot = -1;
    private boolean hasPlacedThisCycle = false;
    private boolean pendingRestoreSlot = false;
    private int pendingRestoreTicksLeft = 0;

    public KeyAnchor() {
        super("Key Anchor", "Automatically places and explodes respawn anchors for PvP", -1, Category.COMBAT);
        this.addSettings(anchorKeybind, delay, restoreDelayTicks);
        this.getSettings().removeIf(setting -> setting instanceof KeybindSetting && !setting.equals(anchorKeybind));
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull() || !isEnabled()) return;
        if (mc.screen != null) return;

        boolean currentKeyState = KeyUtils.isKeyPressed(anchorKeybind.getKeyCode());

        if (currentKeyState && !keyPressed) {
            startAnchorPvP();
        } else if (!currentKeyState && keyPressed) {
            stopAnchorPvP();
        } else if (!currentKeyState) {
            hasPlacedThisCycle = false;
        }

        keyPressed = currentKeyState;

        if (isActive && timer.hasElapsedTime(delay.getValueInt())) {
            processAnchorPvP();
            timer.reset();
        }

        if (pendingRestoreSlot) {
            if (pendingRestoreTicksLeft <= 0) {
                restoreOriginalSlot();
                pendingRestoreSlot = false;
            } else {
                pendingRestoreTicksLeft--;
            }
        }
    }

    private void startAnchorPvP() {
        if (isActive) return;

        isActive = true;
        originalSlot = mc.player.getInventory().getSelectedSlot();
        hasPlacedThisCycle = false;
        timer.reset();
    }

    private void stopAnchorPvP() {
        if (!isActive) return;

        if (originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        isActive = false;
        originalSlot = -1;
        pendingRestoreSlot = false;
        pendingRestoreTicksLeft = 0;
    }

    private void processAnchorPvP() {
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;

        BlockPos targetBlock = blockHit.getBlockPos();
        var blockState = mc.level.getBlockState(targetBlock);

        if (blockState.isAir()) return;

        if (blockState.getBlock() == Blocks.RESPAWN_ANCHOR) {
            int charges = blockState.getValue(RespawnAnchorBlock.CHARGE);
            if (charges > 0) {
                if (swapToItem(Items.TOTEM_OF_UNDYING) || swapToSword()) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    scheduleRestoreOriginalSlot();
                    hasPlacedThisCycle = true;
                }
            } else {
                if (swapToItem(Items.GLOWSTONE)) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    hasPlacedThisCycle = true;
                }
            }
            return;
        }

        BlockPos placementPos = targetBlock.relative(blockHit.getDirection());
        if (isValidAnchorPosition(placementPos) && !hasPlacedThisCycle) {
            if (swapToItem(Items.RESPAWN_ANCHOR)) {
                hasPlacedThisCycle = true;
                ((MinecraftClientAccessor) mc).invokeDoItemUse();

            }
        }
    }


    private boolean isValidAnchorPosition(BlockPos pos) {
        if (mc.level == null || mc.player == null) return false;
        if (mc.player.position().distanceTo(Vec3.atCenterOf(pos)) > 4.5) return false;
        if (!mc.level.getBlockState(pos).isAir()) return false;

        BlockPos playerPos = mc.player.blockPosition();
        return !pos.equals(playerPos) && !pos.equals(playerPos.above());
    }

    private boolean swapToItem(net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private boolean swapToSword() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && InventoryUtil.isSword(stack)) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private void restoreOriginalSlot() {
        if (originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
    }

    private void scheduleRestoreOriginalSlot() {
        if (originalSlot != -1) {
            pendingRestoreSlot = true;
            pendingRestoreTicksLeft = restoreDelayTicks.getValueInt();
        }
    }

    @Override
    public void onEnable() {
        keyPressed = false;
        isActive = false;
        originalSlot = -1;
        hasPlacedThisCycle = false;
        pendingRestoreSlot = false;
        pendingRestoreTicksLeft = 0;
        timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        stopAnchorPvP();
        super.onDisable();
    }

    @Override
    public int getKey() {
        return -1;
    }
}