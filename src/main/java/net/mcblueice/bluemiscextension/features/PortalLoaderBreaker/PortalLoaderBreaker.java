package net.mcblueice.bluemiscextension.features.PortalLoaderBreaker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;

public class PortalLoaderBreaker implements Feature, Listener {
    private final BlueMiscExtension plugin;
    private long serverStartTime;
    private long delayTime;
    private volatile boolean isExpired = false;

    public PortalLoaderBreaker(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        serverStartTime = System.currentTimeMillis();
        delayTime = plugin.getConfig().getLong("Features.PortalLoaderBreaker.DelayTime", 180000);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (isExpired) return;
        if (event.getEntity() instanceof Player) return;

        if (System.currentTimeMillis() - serverStartTime < delayTime) {
            event.getEntity().setPortalCooldown(600);
        } else {
            if (!isExpired) isExpired = true; 
        }
    }
}