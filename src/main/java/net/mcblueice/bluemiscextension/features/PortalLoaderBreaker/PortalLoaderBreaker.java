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
    private volatile long manualBlockUntil = 0;

    public PortalLoaderBreaker(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        serverStartTime = System.currentTimeMillis();
        delayTime = Math.max(1, plugin.getConfig().getLong("Features.PortalLoaderBreaker.DelayTime", 180000));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getCommandManager().register(new PortalLoaderBreakerCommand(plugin, this), "bluemiscextension.portalbreak", new String[]{"portalbreak"});
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
        plugin.getCommandManager().unregister("portalbreak");
    }

    public void stopPortalLoading(long durationMillis) {
        this.manualBlockUntil = System.currentTimeMillis() + durationMillis;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (event.getEntity() instanceof Player) return;

        long now = System.currentTimeMillis();
        if ((now - serverStartTime < delayTime) || (now < manualBlockUntil)) event.getEntity().setPortalCooldown(600);
    }
}