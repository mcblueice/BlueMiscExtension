package net.mcblueice.bluemiscextension.features.PhantomSpawnLimiter;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;

public class PhantomSpawnLimiter implements Feature, Listener {
    private final BlueMiscExtension plugin;
    private final DatabaseUtil databaseUtil;

    public PhantomSpawnLimiter(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommandManager().register(new PhantomSpawnLimiterCommand(plugin, this), "bluemiscextension.phantom", new String[]{"phantom"});
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
        plugin.getCommandManager().unregister("phantom");
    }

    @EventHandler
    public void onPhantomPreSpawn(PhantomPreSpawnEvent event) {
        if (event.getSpawningEntity() instanceof Player player) {
            if (!databaseUtil.getPlayerData(player.getUniqueId()).phantomSpawn()) event.setCancelled(true);
        }
    }
}