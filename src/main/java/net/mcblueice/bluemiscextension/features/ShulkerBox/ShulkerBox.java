package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;

public class ShulkerBox implements Feature {
    private final BlueMiscExtension plugin;
    public static final Map<UUID, Long> openCooldowns = new ConcurrentHashMap<>();

    private ShulkerBoxOpen openListener;
    private ShulkerBoxClose closeListener;
    private ShulkerBoxLock lockListener;

    public ShulkerBox(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        openListener = new ShulkerBoxOpen(plugin);
        closeListener = new ShulkerBoxClose(plugin);
        lockListener = new ShulkerBoxLock(plugin);
        Bukkit.getPluginManager().registerEvents(openListener, plugin);
        Bukkit.getPluginManager().registerEvents(closeListener, plugin);
        Bukkit.getPluginManager().registerEvents(lockListener, plugin);
    }

    @Override
    public void unregister() {
        if (openListener != null) HandlerList.unregisterAll(openListener);
        if (closeListener != null) HandlerList.unregisterAll(closeListener);
        if (lockListener != null) HandlerList.unregisterAll(lockListener);
    }

    public static class ShulkerBoxHolder implements InventoryHolder {
        private final UUID shulkerUuid;
        private final UUID ownerUuid;
        private Inventory inventory;

        public ShulkerBoxHolder(UUID shulkerUuid, UUID ownerUuid) {
            this.shulkerUuid = shulkerUuid;
            this.ownerUuid = ownerUuid;
        }

        @Override
        public @NotNull Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }

        public UUID getShulkerUuid() { return shulkerUuid; }
        public UUID getOwnerUuid() { return ownerUuid; }
    }
}