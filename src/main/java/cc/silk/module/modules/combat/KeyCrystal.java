package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.ItemUseEvent;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Random;

public final class KeyCrystal extends Module {

    private final KeybindSetting crystalKeybind = new KeybindSetting("Crystal Key", GLFW.GLFW_MOUSE_BUTTON_4, false);
    private final BooleanSetting antiSuicide = new BooleanSetting("Anti Suicide", true);
    private final BooleanSetting antiWeakness = new BooleanSetting("Anti Weakness", true);
    private final BooleanSetting stopOnKill = new BooleanSetting("Stop On Kill", false);

    private final NumberSetting placeChance = new NumberSetting("Place Chance (%)", 0, 100, 100, 1);
    private final NumberSetting breakChance = new NumberSetting("Break Chance (%)", 0, 100, 100, 1);

    private final NumberSetting minBreakDelay = new NumberSetting("Min Break Delay (MS)", 10, 500, 50, 1);
    private final NumberSetting maxBreakDelay = new NumberSetting("Max Break Delay (MS)", 10, 500, 100, 1);
    private final NumberSetting minPlaceDelay = new NumberSetting("Min Place Delay (MS)", 10, 500, 30, 1);
    private final NumberSetting maxPlaceDelay = new NumberSetting("Max Place Delay (MS)", 10, 500, 80, 1);

    private final TimerUtil breakTimer = new TimerUtil();
    private final TimerUtil placeTimer = new TimerUtil();
    private final Random random = new Random();

    private boolean keyPressed = false;
    private boolean isActive = false;
    private int originalSlot = -1;
    private boolean hasPlacedObsidian = false;

    private long currentBreakDelay;
    private long currentPlaceDelay;

    public KeyCrystal() {
        super("Key Crystal", "Automatically places and explodes crystals and obsidian for PvP", -1, Category.COMBAT);
        this.addSettings(
                crystalKeybind,
                placeChance, breakChance, stopOnKill,
                minBreakDelay, maxBreakDelay, minPlaceDelay, maxPlaceDelay,
                antiSuicide, antiWeakness
        );

        this.getSettings().removeIf(setting -> setting instanceof KeybindSetting && !setting.equals(crystalKeybind));
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull() || mc.screen != null) return;

        if (minBreakDelay.getValueFloat() >= maxBreakDelay.getValueFloat())
            minBreakDelay.setValue(maxBreakDelay.getValueFloat() - 1);
        if (minPlaceDelay.getValueFloat() >= maxPlaceDelay.getValueFloat())
            minPlaceDelay.setValue(maxPlaceDelay.getValueFloat() - 1);

        boolean currentKeyState = KeyUtils.isKeyPressed(crystalKeybind.getKeyCode());

        if (currentKeyState && !keyPressed) startCrystalPvP();
        else if (!currentKeyState && keyPressed) stopCrystalPvP();

        keyPressed = currentKeyState;

        if (isActive) processCrystalPvP();
    }

    private void startCrystalPvP() {
        if (isActive) return;
        isActive = true;
        originalSlot = mc.player.getInventory().getSelectedSlot();
        hasPlacedObsidian = false;
        resetDelays();
    }

    private void stopCrystalPvP() {
        if (!isActive) return;
        if (originalSlot != -1) mc.player.getInventory().setSelectedSlot(originalSlot);
        isActive = false;
        originalSlot = -1;
        hasPlacedObsidian = false;
        resetDelays();
    }

    private void resetDelays() {
        breakTimer.reset();
        placeTimer.reset();

        int minBreak = minBreakDelay.getValueInt();
        int maxBreak = maxBreakDelay.getValueInt();
        int minPlace = minPlaceDelay.getValueInt();
        int maxPlace = maxPlaceDelay.getValueInt();

        if (minBreak >= maxBreak) {
            maxBreak = minBreak + 1;
            maxBreakDelay.setValue(maxBreak);
        }
        if (minPlace >= maxPlace) {
            maxPlace = minPlace + 1;
            maxPlaceDelay.setValue(maxPlace);
        }

        currentBreakDelay = minBreak + random.nextInt(maxBreak - minBreak);
        currentPlaceDelay = minPlace + random.nextInt(maxPlace - minPlace);
    }

    private void processCrystalPvP() {
        if (antiSuicide.getValue() && !mc.player.onGround()) return;
        if (stopOnKill.getValue() && isDeadPlayerNearby()) return;

        int randomInt = random.nextInt(100) + 1;

        if (mc.hitResult instanceof EntityHitResult entityHit && breakTimer.hasElapsedTime(currentBreakDelay)) {
            if (entityHit.getEntity() instanceof EndCrystal crystal && randomInt <= breakChance.getValueInt()) {
                if (mc.player.position().distanceTo(crystal.position()) <= 6.0) {
                    if (antiWeakness.getValue() &&
                            mc.player.hasEffect(MobEffects.WEAKNESS)) {
                        InventoryUtil.swapToSword();
                    }

                    ((MinecraftClientAccessor) mc).invokeDoAttack();


                    breakTimer.reset();
                    currentBreakDelay = random.nextLong(minBreakDelay.getValueInt(), maxBreakDelay.getValueInt());
                }
                return;
            }
        }


        if (mc.hitResult instanceof BlockHitResult blockHit && placeTimer.hasElapsedTime(currentPlaceDelay)) {
            BlockPos targetBlock = blockHit.getBlockPos();
            BlockPos placementPos = targetBlock.relative(blockHit.getDirection());

            if (isObsidianOrBedrock(targetBlock) && isValidCrystalPosition(placementPos)
                    && randomInt <= placeChance.getValueInt()) {
                if (hasItemInHotbar(Items.END_CRYSTAL)) {
                    InventoryUtil.swapToSlot(Items.END_CRYSTAL);
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    placeTimer.reset();
                    currentPlaceDelay = random.nextLong(minPlaceDelay.getValueInt(), maxPlaceDelay.getValueInt());
                }
            } else if (isValidPosition(placementPos) && !hasPlacedObsidian) {
                BlockPos below = placementPos.below();
                if (!mc.level.getBlockState(below).isAir()) {
                    if (hasItemInHotbar(Items.OBSIDIAN)) {
                        InventoryUtil.swapToSlot(Items.OBSIDIAN);
                        ((MinecraftClientAccessor) mc).invokeDoItemUse();
                        hasPlacedObsidian = true;
                        placeTimer.reset();
                        currentPlaceDelay = random.nextLong(minPlaceDelay.getValueInt(), maxPlaceDelay.getValueInt());
                    }
                }
            }
        }
    }

    private boolean hasItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) return true;
        }
        return false;
    }

    private boolean isValidPosition(BlockPos pos) {
        if (mc.level == null) return false;
        if (mc.player.position().distanceTo(Vec3.atCenterOf(pos)) > 4.5) return false;
        if (!mc.level.getBlockState(pos).isAir()) return false;

        BlockPos playerPos = mc.player.blockPosition();
        return !pos.equals(playerPos) && !pos.equals(playerPos.above());
    }

    private boolean isObsidianOrBedrock(BlockPos pos) {
        if (mc.level == null) return false;
        var block = mc.level.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
    }

    private boolean isValidCrystalPosition(BlockPos pos) {
        if (mc.level == null) return false;
        if (mc.player.position().distanceTo(Vec3.atCenterOf(pos)) > 4.5) return false;
        if (!mc.level.getBlockState(pos).isAir()) return false;
        if (!mc.level.getBlockState(pos.above()).isAir()) return false;

        BlockPos playerPos = mc.player.blockPosition();
        return !pos.equals(playerPos) && !pos.equals(playerPos.above()) &&
                !pos.above().equals(playerPos) && !pos.above().equals(playerPos.above());
    }

    private boolean isDeadPlayerNearby() {
        if (mc.level == null) return false;
        List<? extends Entity> players = mc.level.players();
        for (Entity e : players) {
            if (e == mc.player) continue;
            if (e.isRemoved() || ((LivingEntity) e).getHealth() <= 0.0f)
                if (e.distanceToSqr(mc.player) < 36)
                    return true;
        }
        return false;
    }


    @Override
    public void onDisable() {
        super.onDisable();
        stopCrystalPvP();
    }

    @Override
    public int getKey() {
        return -1;
    }
}