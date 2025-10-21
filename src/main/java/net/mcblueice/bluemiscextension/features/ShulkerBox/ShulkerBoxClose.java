package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class ShulkerBoxClose implements Listener {
    private final ShulkerBox manager;

    public ShulkerBoxClose(ShulkerBox manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClickPersist(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        UUID uuid = player.getUniqueId();
        UUID shulkerBoxUuid = manager.getOpenedShulkerBoxes().get(uuid);
        if (shulkerBoxUuid == null) return;

        Inventory top = player.getOpenInventory().getTopInventory();
        if (top == null) return;
        if (top.getType() != InventoryType.SHULKER_BOX) return;

        int rawSlot = event.getRawSlot();
        boolean affectsTop = rawSlot >= 0 && rawSlot < top.getSize();
        if (!affectsTop) {
            ClickType clickType = event.getClick();
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) affectsTop = true;
        }
        if (!affectsTop) return;

        saveInventoryToItem(player, top, shulkerBoxUuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDragPersist(InventoryDragEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        UUID shulkerUuid = manager.getOpenedShulkerBoxes().get(uuid);
        if (shulkerUuid == null) return;

        Inventory top = player.getOpenInventory() != null ? player.getOpenInventory().getTopInventory() : null;
        if (top == null) return;
        InventoryType topType = top.getType();
        if (topType != InventoryType.SHULKER_BOX) return;

        boolean affectsTop = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < top.getSize()) {
                affectsTop = true;
                break;
            }
        }
        if (!affectsTop) return;

        saveInventoryToItem(player, top, shulkerUuid);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        manager.getOpenedShulkerBoxes().remove(uuid);
        manager.getOpenCooldowns().remove(uuid);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        UUID shulkerUuid = manager.getOpenedShulkerBoxes().get(uuid);
        Inventory closedInv = event.getInventory();
        if (shulkerUuid == null || closedInv == null) return;
        if (closedInv.getType() == InventoryType.SHULKER_BOX) saveInventoryToItem(player, closedInv, shulkerUuid);
        
        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 1f, 1f);
        manager.getOpenedShulkerBoxes().remove(uuid);
    }

    private void saveInventoryToItem(Player player, Inventory inv, UUID shulkerUuid) {
        if (player == null || inv == null || shulkerUuid == null) return;

        ItemStack shulkerBoxItem = null;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        UUID mainUuid = ShulkerBoxUtil.getUUID(mainHand);
        if (mainUuid != null && mainUuid.equals(shulkerUuid)) shulkerBoxItem = mainHand;

        if (shulkerBoxItem == null) {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            UUID offUuid = ShulkerBoxUtil.getUUID(offHand);
            if (offUuid != null && offUuid.equals(shulkerUuid)) shulkerBoxItem = offHand;
        }
        if (shulkerBoxItem == null) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null) continue;
                UUID itemUuid = ShulkerBoxUtil.getUUID(item);
                if (itemUuid != null && itemUuid.equals(shulkerUuid)) {
                    shulkerBoxItem = item;
                    break;
                }
            }
        }
        if (shulkerBoxItem == null) return;

        ItemMeta shulkerBoxMeta = shulkerBoxItem.getItemMeta();
        if (!(shulkerBoxMeta instanceof BlockStateMeta)) return;
        BlockStateMeta blockMeta = (BlockStateMeta) shulkerBoxMeta;
        BlockState blockState = blockMeta.getBlockState();
        if (!(blockState instanceof InventoryHolder)) return;

        Inventory holderInv = ((InventoryHolder) blockState).getInventory();
        
        holderInv.setContents(inv.getContents());

        blockMeta.setBlockState(blockState);
        shulkerBoxItem.setItemMeta(blockMeta);
        TaskScheduler.runTaskLater(player, manager.getPlugin(), () -> player.updateInventory(), 1);
    }
}
