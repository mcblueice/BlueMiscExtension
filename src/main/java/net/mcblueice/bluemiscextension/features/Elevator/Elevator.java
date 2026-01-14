package net.mcblueice.bluemiscextension.features.Elevator;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.utils.ServerUtil;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;
import net.mcblueice.bluemiscextension.features.Feature;

public class Elevator implements Listener, Feature {

    private final BlueMiscExtension plugin;
    private final boolean debug;

    public Elevator(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("Features.Elevator.debug", false);
    }

    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) return;

        Location playerLoc = player.getLocation();
        Location particeLocation = playerLoc.clone().getBlock().getLocation().add(0.5, 1, 0.5);

        Block[] belowBlocks = getBelowBlocks(playerLoc);

        if (debug) plugin.sendDebug("蹲下 - 下方1格:" + belowBlocks[0].getType().name() + ", 下方2格:" + belowBlocks[1].getType().name() + ", 下方3格:" + belowBlocks[2].getType().name());

        String matchedCombo = isElevator(belowBlocks);
        if (matchedCombo != null) {
            Location target = findElevatorTarget(player, matchedCombo, false);
            if (target != null) {
                TaskScheduler.runTask(player, plugin, () -> {
                    if (ServerUtil.isNewParticle()) {
                        particeLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, particeLocation, 15, 0.2, 0.4, 0.2, 0.005, 1.0f);
                    } else {
                        particeLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, particeLocation, 15, 0.2, 0.4, 0.2, 0.005);
                    }
                    try {
                        player.teleportAsync(target);
                    } catch (NoSuchMethodError e) {
                        player.teleport(target);
                    }
                    player.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1.0f, 1.0f));
                    if (debug) plugin.sendDebug("電梯下降: " + player.getName() + " 到 Y=" + target.getBlockY());
                });
            }
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();

        if (fromLoc.getY() >= toLoc.getY()) return;
        if (player.isFlying()) return;
        if (player.getVelocity().getY() <= 0) return;

        Location playerLoc = fromLoc;
        Location particeLocation = playerLoc.clone().getBlock().getLocation().add(0.5, 1, 0.5);

        Block[] belowBlocks = getBelowBlocks(playerLoc);

        if (debug) plugin.sendDebug("跳躍 -  下方1格:" + belowBlocks[0].getType().name() + ", 下方2格:" + belowBlocks[1].getType().name() + ", 下方3格:" + belowBlocks[2].getType().name());

        String matchedCombo = isElevator(belowBlocks);
        if (matchedCombo != null) {
            Location target = findElevatorTarget(player, matchedCombo, true);
            if (target != null) {
                TaskScheduler.runTask(player, plugin, () -> {
                    if (ServerUtil.isNewParticle()) {
                        particeLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, particeLocation, 15, 0.2, 0.4, 0.2, 0.005, 1.0f);
                    } else {
                        particeLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, particeLocation, 15, 0.2, 0.4, 0.2, 0.005);
                    }
                    try {
                        player.teleportAsync(target);
                    } catch (NoSuchMethodError e) {
                        player.teleport(target);
                    }
                    player.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1.0f, 0.5f));
                    if (debug) plugin.sendDebug("電梯上升: " + player.getName() + " 到 Y=" + target.getBlockY());
                });
            }
        }
    }

// #region Utils
    private Block[] getBelowBlocks(Location playerLoc) {
        double y = playerLoc.getY();
        double fractionalY = y - Math.floor(y);

        Block blockBelow1 = playerLoc.clone().subtract(0, 1, 0).getBlock();
        Block blockBelow2 = playerLoc.clone().subtract(0, 2, 0).getBlock();
        Block blockBelow3 = playerLoc.clone().subtract(0, 3, 0).getBlock();
        if (fractionalY > 0.05 && fractionalY < 0.9) {
            blockBelow1 = playerLoc.clone().subtract(0, 0, 0).getBlock();
            blockBelow2 = playerLoc.clone().subtract(0, 1, 0).getBlock();
            blockBelow3 = playerLoc.clone().subtract(0, 2, 0).getBlock();
        }
        return new Block[]{blockBelow1, blockBelow2, blockBelow3};
    }

    private String isElevator(Block[] blocks) {
        List<String> elevators = plugin.getConfig().getStringList("Features.Elevator.elevators");
        for (String elevator : elevators) {
            String[] parts = elevator.split(",");
            if (parts.length <= blocks.length) {
                boolean match = true;
                for (int i = 0; i < parts.length; i++) {
                    String expected = parts[i].trim();
                    if (!blocks[i].getType().name().equals(expected)) {
                        match = false;
                        break;
                    }
                }
                if (match) return elevator;
            }
        }
        return null;
    }

    private Location findElevatorTarget(Player player, String matchedCombo, boolean upwards) {
        int maxDistance = plugin.getConfig().getInt("Features.Elevator.max_distance", 64);
        Location startLoc = player.getLocation();

        String[] parts = matchedCombo.split(",");
        for (int i = 1; i <= maxDistance; i++) {
            Location checkLoc = startLoc.clone().add(0, upwards ? i : -i, 0);
            Block[] checkBlocks = getBelowBlocks(checkLoc);
            boolean match = true;
            for (int j = 0; j < parts.length; j++) {
                String expected = parts[j].trim();
                if (!checkBlocks[j].getType().name().equals(expected)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                Block headBlock = checkLoc.clone().add(0, 1, 0).getBlock();
                Block feetBlock = checkLoc.getBlock();

                boolean headPassable = headBlock.getType().isAir() ||
                                       headBlock.getType().name().contains("WATER") ||
                                       headBlock.isPassable();
                boolean feetPassable = feetBlock.getType().isAir() ||
                                       feetBlock.getType().name().contains("WATER") ||
                                       feetBlock.getType().name().contains("CARPET") ||
                                       feetBlock.getType().name().contains("SLAB") ||
                                       feetBlock.isPassable();

                if (headPassable && feetPassable) return checkLoc;
            }
        }
        return null;
    }
// #endregion
}
