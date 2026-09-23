package cc.silk.module.modules.render;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.friend.FriendManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.PlayerTeam;

import java.awt.*;

public class OutlineESP extends Module {

    private static OutlineESP instance;

    private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
    private final BooleanSetting teamCheck = new BooleanSetting("Team Check", false);
    private final BooleanSetting showPassives = new BooleanSetting("Show Passives", false);
    private final BooleanSetting showHostiles = new BooleanSetting("Show Hostiles", false);
    private final NumberSetting range = new NumberSetting("Range", 10, 200, 100, 5);
    private final ColorSetting playerColor = new ColorSetting("Player Color", new Color(255, 255, 255));
    private final ColorSetting passiveColor = new ColorSetting("Passive Color", new Color(0, 255, 0));
    private final ColorSetting hostileColor = new ColorSetting("Hostile Color", new Color(255, 0, 0));

    private final java.util.Set<Integer> handledEntities = new java.util.HashSet<>();

    public OutlineESP() {
        super("Outline ESP", "Uses Minecraft's glowing effect for entity outlines", Category.RENDER);
        instance = this;
        addSettings(showSelf, teamCheck, showPassives, showHostiles, range, playerColor, passiveColor, hostileColor);
    }

    public static OutlineESP getInstance() {
        return instance;
    }

    public boolean shouldEntityGlow(Entity entity) {
        if (!isEnabled() || isNull())
            return false;
        return shouldRender(entity);
    }

    public boolean wasHandledByModule(Entity entity) {
        return handledEntities.contains(entity.getId());
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.level == null)
            return;

        try {
            for (Entity entity : mc.level.players()) {
                if (shouldRender(entity)) {
                    applyColor(entity);
                } else {
                    removeGlow(entity);
                }
            }
        } catch (Exception e) {
        }
    }

    private void removeGlow(Entity entity) {
        if (!handledEntities.contains(entity.getId())) {
            return;
        }

        if (mc.level == null || mc.level.getScoreboard() == null) {
            return;
        }

        handledEntities.remove(entity.getId());

        try {
            String teamName = "outlineESP_" + entity.getId();
            PlayerTeam team = mc.level.getScoreboard().getPlayersTeam(teamName);

            if (team != null) {
                mc.level.getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), team);
                mc.level.getScoreboard().removePlayerTeam(team);
            }

            PlayerTeam currentTeam = entity.getTeam();
            if (currentTeam != null && currentTeam.getName().startsWith("outlineESP_")) {
                mc.level.getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), currentTeam);
            }
        } catch (Exception e) {
        }
    }

    private boolean shouldRender(Entity entity) {
        if (mc.player.distanceTo(entity) > range.getValue())
            return false;

        if (entity instanceof Player player) {
            if (player == mc.player && !showSelf.getValue())
                return false;
            if (teamCheck.getValue() && isTeammate(player))
                return false;
            return true;
        }

        if (entity instanceof Animal) {
            return showPassives.getValue();
        }

        if (entity instanceof Enemy) {
            return showHostiles.getValue();
        }

        return false;
    }

    private boolean isTeammate(Player player) {
        if (mc.player.getTeam() == null || player.getTeam() == null) {
            return false;
        }
        return mc.player.getTeam().equals(player.getTeam());
    }

    private void applyColor(Entity entity) {
        if (mc.level == null || mc.level.getScoreboard() == null) {
            return;
        }

        handledEntities.add(entity.getId());

        try {
            String teamName = "outlineESP_" + entity.getId();
            PlayerTeam existingTeam = entity.getTeam();

            if (existingTeam != null && !existingTeam.getName().startsWith("outlineESP_")) {
                mc.level.getScoreboard().removePlayerFromTeam(entity.getScoreboardName(), existingTeam);
            }

            PlayerTeam team = mc.level.getScoreboard().getPlayersTeam(teamName);
            if (team == null) {
                team = mc.level.getScoreboard().addPlayerTeam(teamName);
            }

            if (entity.getTeam() != team) {
                mc.level.getScoreboard().addPlayerToTeam(entity.getScoreboardName(), team);
            }

            Color color = getColorForEntity(entity);
            team.setColor(getClosestMinecraftColor(color));
        } catch (Exception e) {
        }
    }

    private Color getColorForEntity(Entity entity) {
        if (entity instanceof Player player) {
            if (FriendManager.isFriend(player.getUUID())) {
                return new Color(128, 0, 128);
            }
            return playerColor.getValue();
        }

        if (entity instanceof Animal) {
            return passiveColor.getValue();
        }

        if (entity instanceof Enemy) {
            return hostileColor.getValue();
        }

        return playerColor.getValue();
    }

    private net.minecraft.ChatFormatting getClosestMinecraftColor(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        if (r > 200 && g < 100 && b < 100)
            return net.minecraft.ChatFormatting.RED;
        if (r < 100 && g > 200 && b < 100)
            return net.minecraft.ChatFormatting.GREEN;
        if (r < 100 && g < 100 && b > 200)
            return net.minecraft.ChatFormatting.BLUE;
        if (r > 200 && g > 200 && b < 100)
            return net.minecraft.ChatFormatting.YELLOW;
        if (r > 200 && g < 100 && b > 200)
            return net.minecraft.ChatFormatting.LIGHT_PURPLE;
        if (r < 100 && g > 200 && b > 200)
            return net.minecraft.ChatFormatting.AQUA;
        if (r > 200 && g > 200 && b > 200)
            return net.minecraft.ChatFormatting.WHITE;
        if (r < 100 && g < 100 && b < 100)
            return net.minecraft.ChatFormatting.DARK_GRAY;

        return net.minecraft.ChatFormatting.WHITE;
    }

    @Override
    public void onDisable() {
        if (!isNull() && mc.level != null && mc.level.getScoreboard() != null) {
            try {
                java.util.Set<Integer> entitiesToRemove = new java.util.HashSet<>(handledEntities);

                for (Entity entity : mc.level.players()) {
                    if (entitiesToRemove.contains(entity.getId())) {
                        removeGlow(entity);
                    }
                }

                java.util.List<PlayerTeam> teamsToRemove = new java.util.ArrayList<>();
                for (PlayerTeam team : mc.level.getScoreboard().getPlayerTeams()) {
                    if (team.getName().startsWith("outlineESP_")) {
                        teamsToRemove.add(team);
                    }
                }

                for (PlayerTeam team : teamsToRemove) {
                    mc.level.getScoreboard().removePlayerTeam(team);
                }

                handledEntities.clear();
            } catch (Exception e) {
                handledEntities.clear();
            }
        }
        super.onDisable();
    }
}
