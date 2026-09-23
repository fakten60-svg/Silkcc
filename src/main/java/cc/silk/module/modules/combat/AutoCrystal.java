package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.KeybindSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.keybinding.KeyUtils;
import cc.silk.utils.math.TimerUtil;
import cc.silk.utils.mc.InventoryUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * @author koi
 */
public final class AutoCrystal extends Module {

    private final KeybindSetting crystalKey = new KeybindSetting("Crystal Key", GLFW.GLFW_MOUSE_BUTTON_4, false);

    private final NumberSetting delay = new NumberSetting("Delay (MS)", 1, 200, 50, 1);

    private final BooleanSetting antiSuicide = new BooleanSetting("Anti Suicide", true);

    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);

    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);

    private final BooleanSetting antiWeakness = new BooleanSetting("Anti Weakness", true);

    private final TimerUtil timer = new TimerUtil();

    private int originalSlot = -1;

    public AutoCrystal() {
        super("Auto Crystal", "Hold key to spam crystals", -1, Category.COMBAT);
        this.addSettings(crystalKey, delay, antiSuicide, autoSwitch, switchBack, antiWeakness);
        this.getSettings().removeIf(setting -> setting instanceof KeybindSetting && !setting.equals(crystalKey));
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;
        if (mc.screen != null) return;

        if (!KeyUtils.isKeyPressed(crystalKey.getKeyCode())) return;

        if (timer.hasElapsedTime(delay.getValueInt())) {
            processCrystal();
            timer.reset();
        }
    }

    private void processCrystal() {
        if (antiSuicide.getValue() && !mc.player.onGround()) return;
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof EndCrystal crystal) {
                if (!crystal.isRemoved() && crystal.isAlive() && mc.level.getEntity(crystal.getId()) != null) {

                    if (mc.player.position().distanceTo(crystal.position()) <= 4.5) {
                        if (antiWeakness.getValue() && mc.player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) {
                            InventoryUtil.swapToSword();
                        }
                        ((MinecraftClientAccessor) mc).invokeDoAttack();
                    }
                }
                return;
            }
        }

        if (mc.hitResult instanceof BlockHitResult blockHit) {
            BlockPos targetBlock = blockHit.getBlockPos();
            BlockPos placementPos = targetBlock.relative(blockHit.getDirection());

            if (isObsidianOrBedrock(targetBlock) && isValidCrystalPosition(placementPos)) {
                if (autoSwitch.getValue() && hasItemInHotbar()) {
                    InventoryUtil.swapToSlot(Items.END_CRYSTAL);
                }

                if (mc.player.getMainHandItem().getItem() == Items.END_CRYSTAL) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                }
            }
        }
    }

    private boolean hasItemInHotbar() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.END_CRYSTAL) return true;
        }
        return false;
    }

    private boolean isObsidianOrBedrock(BlockPos pos) {
        if (mc.level == null) return false;
        var block = mc.level.getBlockState(pos).getBlock();
        return block == net.minecraft.world.level.block.Blocks.OBSIDIAN || block == net.minecraft.world.level.block.Blocks.BEDROCK;
    }

    private boolean isValidCrystalPosition(BlockPos pos) {
        if (mc.level == null) return false;
        if (mc.player.position().distanceTo(Vec3.atCenterOf(pos)) > 4.5) return false;

        if (!mc.level.getBlockState(pos).isAir()) return false;
        if (!mc.level.getBlockState(pos.above()).isAir()) return false;

        BlockPos playerPos = mc.player.blockPosition();
        return !pos.equals(playerPos) && !pos.equals(playerPos.above());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (autoSwitch.getValue()) originalSlot = mc.player.getInventory().getSelectedSlot();
        timer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (switchBack.getValue() && originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
    }

    @Override
    public int getKey() {
        return -1;
    }
}// reverted