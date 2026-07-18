package net.mcblueice.bluemiscextension.features.ClickThrough;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;

public class ClickThrough implements Feature, Listener {
    private final BlueMiscExtension plugin;

    public ClickThrough(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onInteractSign(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material material = block.getType();
        if (!Tag.SIGNS.isTagged(material)) return;
        
        Player player = event.getPlayer();
        if (!player.hasPermission("bluemiscextension.clickthrough")) return;
        if (player.isSneaking()) return;

        Block targetBlock = null;
        BlockData data = block.getBlockData();

        if (Tag.WALL_SIGNS.isTagged(material)) {
            Directional directional = (Directional) data;
            targetBlock = block.getRelative(directional.getFacing().getOppositeFace());
        } else if (Tag.STANDING_SIGNS.isTagged(material)) { 
            targetBlock = block.getRelative(BlockFace.DOWN);
        }
        if (targetBlock == null || targetBlock.getType().isAir()) return;

        PlayerInteractEvent fakeEvent = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                event.getItem(),
                targetBlock,
                event.getBlockFace(),
                event.getHand()
        );
        Bukkit.getPluginManager().callEvent(fakeEvent);
        if (fakeEvent.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) return;

        if (targetBlock.getState() instanceof Container container) {
            player.openInventory(container.getInventory());
        } else if (targetBlock.getType() == Material.ENDER_CHEST) {
            player.openInventory(player.getEnderChest());
        } else {
            return;
        }
        player.swingHand(event.getHand());
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteractItemframe(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame itemFrame)) return;

        Player player = event.getPlayer();
        if (player.isSneaking()) return;

        Block targetBlock = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
        if (targetBlock == null || targetBlock.getType().isAir()) return;
        
        PlayerInteractEvent fakeEvent = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItem(event.getHand()),
                targetBlock,
                itemFrame.getAttachedFace().getOppositeFace(),
                event.getHand()
        );
        Bukkit.getPluginManager().callEvent(fakeEvent);
        if (fakeEvent.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) return;

        if (targetBlock.getState() instanceof Container container) {
            player.openInventory(container.getInventory());
        } else if (targetBlock.getType() == Material.ENDER_CHEST) {
            player.openInventory(player.getEnderChest());
        } else {
            return;
        }
        player.swingHand(event.getHand());
        event.setCancelled(true);
    }
}