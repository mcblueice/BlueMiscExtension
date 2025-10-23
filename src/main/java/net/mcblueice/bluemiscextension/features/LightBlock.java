package net.mcblueice.bluemiscextension.features;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import net.mcblueice.bluemiscextension.BlueMiscExtension;

public class LightBlock implements Listener {

    private final BlueMiscExtension plugin;

    public LightBlock(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void displayLightBlock(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("bluemiscextension.lightblock")) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.LIGHT) return;

        Location location = player.getLocation().getBlock().getLocation();
        for (int x = -16; x < 16; x++) {
            for (int y = -16; y < 16; y++) {
                for (int z = -16; z < 16; z++) {
                    Block block = location.clone().add(x, y, z).getBlock();
                    if (block.getType() != Material.LIGHT) continue;
                    Location particleLocation = block.getLocation().add(0.5, 0.5, 0.5);
                    block.getWorld().spawnParticle(Particle.BLOCK_MARKER, particleLocation, 1, block.getBlockData());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interactLightBlock(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (!player.hasPermission("bluemiscextension.lightblock")) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.LIGHT) return;
        if (block == null || block.getType() != Material.LIGHT) return;

        Action action = event.getAction();
        Location blockLoc = block.getLocation().clone().add(0.5, 0.5, 0.5);
        if (action.isLeftClick()) {
            if (player.breakBlock(block)) {
                block.getWorld().dropItem(blockLoc, new ItemStack(Material.LIGHT));
                block.getWorld().playSound(blockLoc, "block.stone.break", SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
        }
        if (action.isRightClick()) {
            if (player.isSneaking()) return;
            Levelled lightLevel = (Levelled) block.getBlockData().clone();
            lightLevel.setLevel((lightLevel.getLevel() + 1) % 16);
            block.setBlockData(lightLevel);
            Location particleLocation = blockLoc;
            if (block.getType() == Material.LIGHT) block.getWorld().spawnParticle(Particle.BLOCK_MARKER, particleLocation, 1, block.getBlockData());
        }
    }

    public void unregister() {}
}
