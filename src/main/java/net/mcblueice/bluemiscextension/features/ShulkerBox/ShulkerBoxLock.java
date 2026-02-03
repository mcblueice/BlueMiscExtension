package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ShulkerBox.ShulkerBox.ShulkerBoxHolder;

public class ShulkerBoxLock implements Listener {
    private final BlueMiscExtension plugin;
    private final boolean debug;

    public ShulkerBoxLock(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("Features.ShulkerBox.debug", false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClickWhileOpen(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof ShulkerBoxHolder holder)) return;

        UUID targetUuid = holder.getShulkerUuid();
        if (targetUuid == null) return;

        // 熱鍵檢查 (1-9)
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarSlot = event.getHotbarButton();
            ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);

            if (ShulkerBoxUtil.isMatchingShulker(hotbarItem, targetUuid)) {
                if (debug) plugin.sendDebug("§b" + player.getName() + "§c熱鍵交換被阻止");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
                event.setCancelled(true);
                return;
            }
        }

        // 副手交換(副手狀態)
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (ShulkerBoxUtil.isMatchingShulker(offhand, targetUuid)) {
                if (debug) plugin.sendDebug("§b" + player.getName() + "§c副手交換被阻止");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
                event.setCancelled(true);
                return;
            }
        }

        // 雙擊收集
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            ItemStack cursor = event.getCursor();
            if (ShulkerBoxUtil.isMatchingShulker(cursor, targetUuid)) {
                if (debug) plugin.sendDebug("§b" + player.getName() + "§c雙擊收集被阻止");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
                event.setCancelled(true);
                return;
            }
        }

        // 通用檢查 (一般點擊 Shift點擊 丟棄 副手交換(主手狀態))
        if (ShulkerBoxUtil.isMatchingShulker(event.getCurrentItem(), targetUuid)) {
            if (debug) plugin.sendDebug("§b" + player.getName() + "§c通用物品互動被阻止");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDragWhileOpen(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof ShulkerBoxHolder holder)) return;

        UUID targetUuid = holder.getShulkerUuid();
        if (targetUuid == null) return;

        // 拖曳物品檢查
        if (ShulkerBoxUtil.isMatchingShulker(event.getOldCursor(), targetUuid)) {
            if (debug) plugin.sendDebug("§b" + player.getName() + "§c拖曳物品互動被阻止");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
            event.setCancelled(true);
            return;
        }

        // 拖曳目標檢查
        for (ItemStack item : event.getNewItems().values()) {
            if (ShulkerBoxUtil.isMatchingShulker(item, targetUuid)) {
                if (debug) plugin.sendDebug("§b" + player.getName() + "§c拖曳目標互動被阻止");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.5f);
                event.setCancelled(true);
                return;
            }
        }
    }
}