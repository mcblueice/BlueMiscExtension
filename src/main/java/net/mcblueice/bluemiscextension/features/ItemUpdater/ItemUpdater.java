package net.mcblueice.bluemiscextension.features.ItemUpdater;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.api.PlayerInventoryUpdateEvent;
import net.mcblueice.bluemiscextension.features.Feature;
import net.william278.husksync.event.BukkitSyncCompleteEvent;

public class ItemUpdater implements Feature, Listener {
    private final BlueMiscExtension plugin;
    private final boolean debug;
    private final Set<UUID> bmeReady = ConcurrentHashMap.newKeySet();
    private final Set<UUID> huskSyncReady = ConcurrentHashMap.newKeySet();

    public ItemUpdater(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("Features.ItemUpdater.debug", false);
    }

    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
        bmeReady.clear();
        huskSyncReady.clear();
    }

    // BME
    @Override
    public void onPlayerDataLoaded(Player player) {
        UUID uuid = player.getUniqueId();
        if (plugin.getHuskSyncAPIHook() != null) {
            if (huskSyncReady.remove(uuid)) {
                Bukkit.getPluginManager().callEvent(new PlayerInventoryUpdateEvent(player));
            } else {
                bmeReady.add(uuid);
            }
        } else {
            Bukkit.getPluginManager().callEvent(new PlayerInventoryUpdateEvent(player));
        }
    }

    // HuskSync
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHuskSyncSyncComplete(BukkitSyncCompleteEvent event) {
        UUID uuid = event.getUser().getUuid();
        if (bmeReady.remove(uuid)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) Bukkit.getPluginManager().callEvent(new PlayerInventoryUpdateEvent(player));
        } else {
            huskSyncReady.add(uuid);
        }
    }

    @Override
    public void onPlayerDataUnload(Player player) {
        UUID uuid = player.getUniqueId();
        bmeReady.remove(uuid);
        huskSyncReady.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryUpdate(PlayerInventoryUpdateEvent event) {
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;

        player.updateInventory();
        if (debug) plugin.sendDebug("已更新玩家 " + player.getName() + " 的物品欄");
    }
}
