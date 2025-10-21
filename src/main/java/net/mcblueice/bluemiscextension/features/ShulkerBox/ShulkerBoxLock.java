package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class ShulkerBoxLock implements Listener {
    private final ShulkerBox manager;

    public ShulkerBoxLock(ShulkerBox manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClickWhileOpen(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clickItem = event.getCurrentItem();
        if (clickItem == null) return;
        if (!clickItem.getType().name().contains("SHULKER_BOX")) return;

        UUID uuid = player.getUniqueId();
        UUID shulkerBoxUuid = manager.getOpenedShulkerBoxes().get(uuid);
        UUID itemUuid = ShulkerBoxUtil.getUUID(clickItem);

        if (itemUuid != null && itemUuid.equals(shulkerBoxUuid)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarButton = event.getHotbarButton();
            ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
            if (hotbarItem == null) return;
            if (!hotbarItem.getType().name().contains("SHULKER_BOX")) return;
            UUID hotbarItemUuid = ShulkerBoxUtil.getUUID(hotbarItem);
            if (hotbarItemUuid != null && hotbarItemUuid.equals(shulkerBoxUuid)) {
                event.setCancelled(true);
                return;
            }
        }
        return;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDragWhileOpen(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        UUID shulkerBoxUuid = manager.getOpenedShulkerBoxes().get(uuid);
        for (ItemStack item : event.getNewItems().values()) {
            if (item == null) continue;
            if (!item.getType().name().contains("SHULKER_BOX")) continue;
            UUID itemUuid = ShulkerBoxUtil.getUUID(item);
            if (itemUuid != null && itemUuid.equals(shulkerBoxUuid)) {
                event.setCancelled(true);
                return;
            }
        }
        return;
    }
}