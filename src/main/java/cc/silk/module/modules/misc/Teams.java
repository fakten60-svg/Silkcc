package cc.silk.module.modules.misc;

import cc.silk.SilkClient;
import cc.silk.module.Category;
import cc.silk.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class Teams extends Module {

    public Teams() {
        super("Teams", "Stops you from targeting teammates", Category.MISC);
    }

    public static boolean isTeammate(Entity entity) {
        var opt = SilkClient.INSTANCE.getModuleManager().getModule(Teams.class);
        if (opt.isEmpty()) return false;
        Teams teamsModule = opt.get();
        if (!teamsModule.isEnabled()) {
            return false;
        }

        if (entity == null || entity.getName() == null || !(entity instanceof LivingEntity)) {
            return false;
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.getTeam() == null) {
                return false;
            }

            return mc.player.isAlliedTo(entity);
        } catch (IllegalStateException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
