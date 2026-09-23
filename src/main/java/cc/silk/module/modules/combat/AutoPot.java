package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.NumberSetting;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.utils.math.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public final class AutoPot extends Module {
    private static final NumberSetting healthThreshold = new NumberSetting("Health Threshold", 1, 20, 10, 0.5);
    private static final NumberSetting throwCooldown = new NumberSetting("Throw Cooldown", 50, 1000, 250, 50);
    private static final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 1, 20, 10, 0.5);
    private static final NumberSetting swapDelay = new NumberSetting("Swap Delay", 0, 200, 50, 10);
    private static final NumberSetting minPlayerDistance = new NumberSetting("Min Player Distance", 0, 10, 0, 0.5);
    private static final BooleanSetting requireOnGround = new BooleanSetting("Require On Ground", true);

    private final TimerUtil throwTimer = new TimerUtil();
    private final TimerUtil swapTimer = new TimerUtil();
    private final List<Integer> availablePotionSlots = new ArrayList<>();
    private int savedHotbarSlot = -1;
    private float savedPitch = 0;
    private boolean isRotating = false;
    private boolean isWaitingToThrow = false;
    private float targetPitch = 0;
    private float rotationProgress = 0;

    public AutoPot() {
        super("Auto Pot", "Automatically throws health potions when health is low", -1, Category.COMBAT);
        this.addSettings(healthThreshold, throwCooldown, rotationSpeed, swapDelay, minPlayerDistance, requireOnGround);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull() || mc.screen != null || mc.player.isUsingItem()) return;

        if (isWaitingToThrow) {
            if (swapTimer.hasElapsedTime(swapDelay.getValueInt())) {
                executeThrow();
            }
            return;
        }

        if (isRotating) {
            handleRotation();
            return;
        }

        if (shouldThrowPotion()) {
            if (!canThrowPotion()) return;
            
            findAvailablePotions();
            if (availablePotionSlots.isEmpty()) return;

            if (savedHotbarSlot == -1) {
                savedHotbarSlot = mc.player.getInventory().getSelectedSlot();
                savedPitch = mc.player.getXRot();
            }

            startRotation(89.9f);
        }
    }

    private void startRotation(float targetPitch) {
        isRotating = true;
        this.targetPitch = targetPitch;
        rotationProgress = 0;
    }

    private void handleRotation() {
        if (rotationSpeed.getValueFloat() <= 1.0f) {
            mc.player.setXRot(targetPitch);
            isRotating = false;
            if (targetPitch == 89.9f) {
                startThrowSequence();
            } else {
                if (mc.player != null && mc.player.getInventory() != null) mc.player.getInventory().setSelectedSlot(savedHotbarSlot);
                resetState();
            }
        } else {
            float speed = (rotationSpeed.getValueFloat() - 1.0f) * 0.2f;
            rotationProgress += speed;

            if (rotationProgress >= 1.0f) {
                mc.player.setXRot(targetPitch);
                isRotating = false;
                if (targetPitch == 89.9f) {
                    startThrowSequence();
                } else {
                    if (mc.player != null && mc.player.getInventory() != null) mc.player.getInventory().setSelectedSlot(savedHotbarSlot);
                    resetState();
                }
            } else {
                mc.player.setXRot(Mth.lerp(rotationProgress, savedPitch, targetPitch));
            }
        }
    }

    private void startThrowSequence() {
        if (swapDelay.getValueInt() <= 0) {
            executeThrow();
        } else {
            isWaitingToThrow = true;
            swapTimer.reset();
        }
    }

    private void executeThrow() {
        if (mc.player != null && mc.player.getInventory() != null) mc.player.getInventory().setSelectedSlot(availablePotionSlots.get(0));
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        isWaitingToThrow = false;
        startRotation(savedPitch);
    }

    private void resetState() {
        savedHotbarSlot = -1;
        savedPitch = 0;
        rotationProgress = 0;
        isWaitingToThrow = false;
        availablePotionSlots.clear();
    }

    private void findAvailablePotions() {
        availablePotionSlots.clear();
        for (int i = 0; i < 9; i++) {
            if (isHealthPotion(mc.player.getInventory().getItem(i))) {
                availablePotionSlots.add(i);
            }
        }
    }

    private boolean isHealthPotion(ItemStack stack) {
        if (stack.getItem() != Items.SPLASH_POTION) return false;
        
        PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null) return false;

        if (potionContents.potion().isPresent()) {
            Holder<Potion> potionEntry = potionContents.potion().get();
            return potionEntry.value().getEffects().stream()
                    .anyMatch(effect -> effect.getEffect().equals(MobEffects.INSTANT_HEALTH));
        }

        return potionContents.customEffects().stream()
                .anyMatch(effect -> effect.getEffect().equals(MobEffects.INSTANT_HEALTH));
    }

    @Override
    public void onEnable() {
        resetState();
        throwTimer.reset();
        swapTimer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (savedHotbarSlot != -1) {
            if (mc.player != null && mc.player.getInventory() != null) mc.player.getInventory().setSelectedSlot(savedHotbarSlot);
            mc.player.setXRot(savedPitch);
        }
        resetState();
        super.onDisable();
    }

    private boolean shouldThrowPotion() {
        return mc.player.getHealth() <= healthThreshold.getValueFloat() && 
               throwTimer.hasElapsedTime(throwCooldown.getValueInt());
    }
    
    private boolean canThrowPotion() {
        if (minPlayerDistance.getValueFloat() > 0 && isPlayerTooClose()) return false;
        if (requireOnGround.getValue() && !mc.player.onGround()) return false;
        return true;
    }
    
    private boolean isPlayerTooClose() {
        if (mc.level == null) return false;
        
        double minDistance = minPlayerDistance.getValueFloat();
        return mc.level.players().stream()
                .anyMatch(player -> player != mc.player && 
                        mc.player.distanceTo(player) < minDistance);
    }

    @Override
    public int getKey() {
        return -1;
    }
}