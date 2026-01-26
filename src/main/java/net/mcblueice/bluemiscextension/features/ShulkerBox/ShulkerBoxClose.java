package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ShulkerBox.ShulkerBox.ShulkerBoxHolder;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class ShulkerBoxClose implements Listener {
    private final BlueMiscExtension plugin;
    private final boolean debug;

    public ShulkerBoxClose(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("Features.ShulkerBox.debug", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClickPersist(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShulkerBoxHolder holder)) return;
        if (!holder.getOwnerUuid().equals(player.getUniqueId())) return;

        UUID shulkerBoxUuid = holder.getShulkerUuid();
        if (shulkerBoxUuid == null) return;

        int rawSlot = event.getRawSlot();
        boolean affectsTop = rawSlot >= 0 && rawSlot < top.getSize();
        if (!affectsTop) {
            ClickType clickType = event.getClick();
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) affectsTop = true;
        }
        if (!affectsTop) return;

        ItemStack foundBox = ShulkerBoxUtil.findShulkerBoxItem(player, shulkerBoxUuid);
        if (foundBox == null) {
            handleMissingBoxInteraction(event, player);
            return;
        }

        TaskScheduler.runTask(player, plugin, () -> {
            saveInventoryToItem(player, top, shulkerBoxUuid, false);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDragPersist(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShulkerBoxHolder holder)) return;
        if (!holder.getOwnerUuid().equals(player.getUniqueId())) return;

        UUID shulkerUuid = holder.getShulkerUuid();
        if (shulkerUuid == null) return;

        boolean affectsTop = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < top.getSize()) {
                affectsTop = true;
                break;
            }
        }
        if (!affectsTop) return;

        ItemStack foundBox = ShulkerBoxUtil.findShulkerBoxItem(player, shulkerUuid);
        if (foundBox == null) {
            handleMissingBoxInteraction(event, player);
            return;
        }

        TaskScheduler.runTask(player, plugin, () -> {
            saveInventoryToItem(player, top, shulkerUuid, false);
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory closedInv = event.getInventory();
        if (!(closedInv.getHolder() instanceof ShulkerBoxHolder holder)) return;
        if (!holder.getOwnerUuid().equals(player.getUniqueId())) return;

        UUID shulkerUuid = holder.getShulkerUuid();
        if (shulkerUuid == null) return;

        TaskScheduler.runTask(player, plugin, () -> {
            saveInventoryToItem(player, closedInv, shulkerUuid, true);
            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 1f, 1f);
        });
    }

    private void saveInventoryToItem(Player player, Inventory inv, UUID shulkerUuid, boolean checkEmpty) {
        if (debug) plugin.sendDebug("§6開始保存 §b" + player.getName() + " §6的界伏盒內容 UUID: §7" + shulkerUuid);

        if (player == null || inv == null || shulkerUuid == null) return;

        ItemStack shulkerBoxItem = ShulkerBoxUtil.findShulkerBoxItem(player, shulkerUuid);

        if (shulkerBoxItem == null) {
            plugin.getLogger().severe("[Critical] 玩家 " + player.getName() + " 的界伏盒(UUID: " + shulkerUuid + ") 在保存時遺失!");
            plugin.getLogger().severe("[Critical] 可能導致數據丟失");
            player.sendMessage("§7§l[§c§l錯誤§7§l] §4無法保存界伏盒數據 請聯繫管理員!");
            return;
        }

        ItemMeta shulkerBoxMeta = shulkerBoxItem.getItemMeta();
        if (!(shulkerBoxMeta instanceof BlockStateMeta)) return;
        BlockStateMeta blockMeta = (BlockStateMeta) shulkerBoxMeta;
        BlockState blockState = blockMeta.getBlockState();
        if (!(blockState instanceof InventoryHolder)) return;

        Inventory holderInv = ((InventoryHolder) blockState).getInventory();

        holderInv.setContents(inv.getContents());
        blockMeta.setBlockState(blockState);
        shulkerBoxItem.setItemMeta(blockMeta);

        if (checkEmpty && inv.isEmpty()) {
            if (debug) plugin.sendDebug("§e界伏盒已清空 正在移除 §b" + player.getName() + " §e的界伏盒 UUID: §7" + shulkerUuid);
            ShulkerBoxUtil.removeUUID(shulkerBoxItem);
        } else {
            if (debug) plugin.sendDebug("§a成功保存界伏盒內容 §b" + player.getName() + " §aUUID: §7" + shulkerUuid);
        }

        player.updateInventory();
    }

    private void handleMissingBoxInteraction(Cancellable event, Player player) {
        event.setCancelled(true);
        player.updateInventory();

        TaskScheduler.runTask(player, plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof ShulkerBoxHolder) player.closeInventory();
            player.sendMessage("§7§l[§c§l錯誤§7§l] §4界伏盒遺失 操作已取消!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        });
    }
}