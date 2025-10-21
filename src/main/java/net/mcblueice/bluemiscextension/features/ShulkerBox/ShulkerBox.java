package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.event.HandlerList;

import net.mcblueice.bluemiscextension.BlueMiscExtension;

public class ShulkerBox {
    private final BlueMiscExtension plugin;
    private final Map<UUID, Long> openCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> openedShulkerBoxes = new ConcurrentHashMap<>();

    private ShulkerBoxOpen openListener;
    private ShulkerBoxClose closeListener;
    private ShulkerBoxLock lockListener;

    public ShulkerBox(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    public void register() {
        openListener = new ShulkerBoxOpen(this);
        closeListener = new ShulkerBoxClose(this);
        lockListener = new ShulkerBoxLock(this);
        plugin.getServer().getPluginManager().registerEvents(openListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(closeListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(lockListener, plugin);
    }

    public void unregister() {
        if (openListener != null) HandlerList.unregisterAll(openListener);
        if (closeListener != null) HandlerList.unregisterAll(closeListener);
        if (lockListener != null) HandlerList.unregisterAll(lockListener);
    }

    public BlueMiscExtension getPlugin() { return plugin; }

    public Map<UUID, Long> getOpenCooldowns() { return openCooldowns; }

    public Map<UUID, UUID> getOpenedShulkerBoxes() { return openedShulkerBoxes; }
}
