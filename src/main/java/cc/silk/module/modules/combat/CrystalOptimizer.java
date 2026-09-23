package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.AttackEvent;
import cc.silk.event.impl.player.DoAttackEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.mc.InventoryUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class CrystalOptimizer extends Module {

    public CrystalOptimizer() {
        super("Crystal Optimizer", "Makes crystals disappear faster client-side for quicker placement.", -1, Category.COMBAT);
    }

    @EventHandler
    private void onAttackEvent(DoAttackEvent event) {
        if (isNull()) return;
        if (mc.hitResult == null) return;

        if (mc.hitResult.getType() != HitResult.Type.ENTITY) return;
        if (!(mc.hitResult instanceof EntityHitResult hit)) return;

        Entity target = hit.getEntity();
        if (!(target instanceof EndCrystal crystal)) return;

        MobEffectInstance weakness = mc.player.getEffect(MobEffects.WEAKNESS);
        MobEffectInstance strength = mc.player.getEffect(MobEffects.STRENGTH);
        ItemStack mainHand = mc.player.getMainHandItem();

        boolean canAttack =
                (weakness == null)
                        || (strength != null && strength.getAmplifier() > weakness.getAmplifier())
                        || (InventoryUtil.isMiningTool(mainHand))
                        || (InventoryUtil.isSword(mainHand));

        if (!canAttack) return;

        crystal.setRemoved(Entity.RemovalReason.KILLED);
    }
}
