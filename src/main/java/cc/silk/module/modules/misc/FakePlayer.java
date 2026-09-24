package cc.silk.module.modules.misc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.HashMultimap;
import cc.silk.event.impl.player.AttackEvent;
import cc.silk.event.impl.level.WorldChangeEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

import java.util.UUID;

public class FakePlayer extends Module {
    private final BooleanSetting invincible = new BooleanSetting("Invincible", false);
    private final BooleanSetting criticalHits = new BooleanSetting("Critical Hits", true);
    private final BooleanSetting useTotem = new BooleanSetting("Use Totem", false);
    private RemotePlayer fakePlayer;
    private float fakePlayerHealth = 20.0f;
    private long lastHitTime = 0;
    private int hitCount = 0;

    public FakePlayer() {
        super("Fake Player", "Spawns a fake player for making configs (Only works in single player)", Category.MISC);
        addSettings(invincible, criticalHits, useTotem);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        spawnFakePlayer();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        despawnFakePlayer();
    }

    @EventHandler
    private void onWorldChange(WorldChangeEvent event) {
        despawnFakePlayer();
    }

    @EventHandler
    private void onAttack(AttackEvent event) {
        if (fakePlayer == null || isNull())
            return;

        if (event.getTarget() == fakePlayer) {
            handleFakePlayerHit(isPlayerCriticalHit());
        }
    }

    private boolean isPlayerCriticalHit() {
        if (mc.player == null)
            return false;

        boolean isFalling = mc.player.getDeltaMovement().y < -0.08F;
        boolean isSneaking = mc.player.isShiftKeyDown();
        boolean onGround = mc.player.onGround();
        boolean isUsingItem = mc.player.isUsingItem();
        boolean isRiding = mc.player.getVehicle() != null;
        boolean isInWater = mc.player.isInWater();
        boolean isInLava = mc.player.isInLava();

        return isFalling && !isSneaking && !onGround && !isUsingItem && !isRiding && !isInWater && !isInLava;
    }

    private void handleFakePlayerHit(boolean isCritical) {
        if (System.currentTimeMillis() - lastHitTime < 500)
            return;
        lastHitTime = System.currentTimeMillis();
        hitCount++;
        boolean shouldPopTotem = false;
        if (useTotem.getValue()) {
            if (invincible.getValue()) {
                if (hitCount % 2 == 0) {
                    shouldPopTotem = true;
                }
            } else {
                float baseDamage = 2.0f + (float) (Math.random() * 4.0f);
                float damage = isCritical ? baseDamage * 1.5f : baseDamage;
                if (fakePlayerHealth - damage <= 0) {
                    shouldPopTotem = true;
                    fakePlayerHealth = 1.0f;
                }
            }
        }
        if (shouldPopTotem) {
            popTotem();
        }
        if (!invincible.getValue() && !shouldPopTotem) {
            float baseDamage = 2.0f + (float) (Math.random() * 4.0f);
            float damage = isCritical ? baseDamage * 1.5f : baseDamage;
            fakePlayerHealth = Math.max(0, fakePlayerHealth - damage);
        }
        addDamageEffects(isCritical);
        if (fakePlayerHealth <= 0 && !invincible.getValue()) {
            respawnFakePlayer();
        }
    }

    private void addDamageEffects(boolean isCritical) {
        if (fakePlayer == null || mc.level == null)
            return;
        int particleCount = isCritical ? 8 : 5;
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = Math.random() * 1.8;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            mc.level.addParticle(ParticleTypes.DAMAGE_INDICATOR,
                    fakePlayer.getX() + offsetX,
                    fakePlayer.getY() + offsetY,
                    fakePlayer.getZ() + offsetZ,
                    0, 0, 0);
        }
        if (isCritical && criticalHits.getValue()) {
            for (int i = 0; i < 5; i++) {
                double offsetX = (Math.random() - 0.5) * 0.8;
                double offsetY = Math.random() * 1.8;
                double offsetZ = (Math.random() - 0.5) * 0.8;
                mc.level.addParticle(ParticleTypes.CRIT,
                        fakePlayer.getX() + offsetX,
                        fakePlayer.getY() + offsetY,
                        fakePlayer.getZ() + offsetZ,
                        0, 0, 0);
            }
        }
        if (isCritical && criticalHits.getValue()) {
            mc.level.playSound(mc.player, fakePlayer.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, fakePlayer.getSoundSource(), 1.0f, 1.0f);
        } else {
            mc.level.playSound(mc.player, fakePlayer.blockPosition(),
                    SoundEvents.PLAYER_HURT, fakePlayer.getSoundSource(),
                    1.0f, 1.0f);
        }
        fakePlayer.hurtTime = 10;
    }

    private void respawnFakePlayer() {
        despawnFakePlayer();
        fakePlayerHealth = 20.0f;
        hitCount = 0;
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (this.isEnabled()) {
                    spawnFakePlayer();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void popTotem() {
        if (fakePlayer == null || mc.level == null)
            return;
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 800, 1));
        mc.level.playSound(null, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.TOTEM_USE, fakePlayer.getSoundSource(),
                1.0f, 1.0f);
        for (int i = 0; i < 30; i++) {
            double offsetX = (mc.level.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetY = mc.level.getRandom().nextDouble() * 2.0;
            double offsetZ = (mc.level.getRandom().nextDouble() - 0.5) * 2.0;
            mc.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                    fakePlayer.getX() + offsetX,
                    fakePlayer.getY() + offsetY,
                    fakePlayer.getZ() + offsetZ,
                    0, 0.1, 0);
        }
        fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        new Thread(() -> {
            try {
                Thread.sleep(500);
                if (fakePlayer != null) {
                    fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void spawnFakePlayer() {
        if (isNull())
            return;
        if (!mc.hasSingleplayerServer())
            return;
        if (fakePlayer != null)
            return;
        GameProfile original = mc.player.getGameProfile();
        // authlib 7 exposes an immutable PropertyMap on GameProfile, so the skin properties have to
        // be copied into a mutable one before the profile can be used for a RemotePlayer.
        PropertyMap properties = new PropertyMap(HashMultimap.create(original.properties()));
        GameProfile profile = new GameProfile(UUID.randomUUID(), original.name(), properties);

        RemotePlayer other = new RemotePlayer(mc.level, profile);
        other.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        other.setYRot(mc.player.getYRot());
        other.setXRot(mc.player.getXRot());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                other.setItemSlot(slot, stack.copy());
            }
        }

        mc.level.addEntity(other);

        fakePlayer = other;
    }

    private void despawnFakePlayer() {
        if (fakePlayer == null)
            return;
        if (!isNull()) {
            fakePlayer.discard();
        }
        fakePlayer = null;
    }
}