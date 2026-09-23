package cc.silk.module.modules.player;


import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;

 //  possibly make a better if in cobweb check but rn AABB is the best i can think of


public final class AutoDrain extends Module {

    private static final NumberSetting actionCooldownMs = new NumberSetting("Cooldown MS", 50, 2000, 250, 1);
    private static final NumberSetting switchBackDelayMs = new NumberSetting("SwitchBack MS", 0, 500, 75, 1);

    private final TimerUtil cooldownTimer = new TimerUtil();
    private final TimerUtil switchBackTimer = new TimerUtil();
    private int originalSlot = -1;
    private boolean pendingSwitchBack = false;

    public AutoDrain() {
        super("Auto Drain", "Swap to empty bucket and pick up water when aiming at a source", -1, Category.PLAYER);
        this.addSettings(actionCooldownMs, switchBackDelayMs);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull() || mc.screen != null || mc.level == null) return;

        if (pendingSwitchBack) {
            if (switchBackDelayMs.getValueInt() <= 0 || switchBackTimer.hasElapsedTime(switchBackDelayMs.getValueInt())) {
                if (originalSlot != -1) mc.player.getInventory().setSelectedSlot(originalSlot);
                originalSlot = -1;
                pendingSwitchBack = false;
            }
            return;
        }

        if (!cooldownTimer.hasElapsedTime(actionCooldownMs.getValueInt())) return;

        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        BlockPos waterPos = getWaterSourcePosFromMouseOver(blockHit);
        if (waterPos == null) return;

        if (isPlayerInCobweb()) return;

        int emptyBucketSlot = findEmptyBucketInHotbar();
        if (emptyBucketSlot == -1) return;

        originalSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(emptyBucketSlot);
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        pendingSwitchBack = true;
        switchBackTimer.reset();
        cooldownTimer.reset();
    }

    private boolean isWaterSource(FluidState fluidState) {
        return fluidState != null && fluidState.getType() == Fluids.WATER && fluidState.isSource();
    }


    private int findEmptyBucketInHotbar() {
        for (int hotbarIndex = 0; hotbarIndex < 9; hotbarIndex++) {
            ItemStack stack = mc.player.getInventory().getItem(hotbarIndex);
            if (!stack.isEmpty() && stack.getItem() == Items.BUCKET) return hotbarIndex;
        }
        return -1;
    }

    private boolean isPlayerInCobweb() {
        var box = mc.player.getBoundingBox(); // this seems messy but otherwise it dosnt work on the edge of the cobweb
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (mc.level.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBWEB)) return true;
                }
            }
        }
        return false;
    }

    private BlockPos getWaterSourcePosFromMouseOver(BlockHitResult blockHit) {
        BlockPos hitPos = blockHit.getBlockPos();
        BlockState hitState = mc.level.getBlockState(hitPos);
        if (hitState.getBlock() == Blocks.WATER && isWaterSource(mc.level.getFluidState(hitPos))) {
            return hitPos;
        }
        BlockPos towardPlayer = hitPos.relative(blockHit.getDirection());
        BlockState towardState = mc.level.getBlockState(towardPlayer);
        if (towardState.getBlock() == Blocks.WATER && isWaterSource(mc.level.getFluidState(towardPlayer))) {
            return towardPlayer;
        }
        return null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (pendingSwitchBack && originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        pendingSwitchBack = false;
    }
}


