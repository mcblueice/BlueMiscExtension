package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class ShulkerBoxOpen implements Listener {
    private final ShulkerBox manager;

    public ShulkerBoxOpen(ShulkerBox manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        if (!player.hasPermission("bluemiscextension.shulkerbox.open.hand")) return;

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (!player.isSneaking()) return;
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand == null) return;
            if (!mainHand.getType().name().contains("SHULKER_BOX")) return;
            if (action == Action.PHYSICAL || ShulkerBoxUtil.isInteractableBlock(event.getClickedBlock())) return;

            event.setCancelled(true);
            openShulker(player, mainHand);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClick() != ClickType.SHIFT_RIGHT) return;
        if (!player.hasPermission("bluemiscextension.shulkerbox.open.inventory")) return;

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(player.getInventory())) return;
        if (event.getSlot() >= 36 && event.getSlot() <= 39) return;

        ItemStack clickItem = event.getCurrentItem();
        if (clickItem == null) return;
        if (!clickItem.getType().name().contains("SHULKER_BOX")) return;

        UUID uuid = player.getUniqueId();
        UUID shulkerBoxUuid = manager.getOpenedShulkerBoxes().get(uuid);
        UUID itemUuid = ShulkerBoxUtil.getUUID(clickItem);
        if (itemUuid != null && itemUuid.equals(shulkerBoxUuid)) return;

        if (player.getOpenInventory().getTopInventory() != null) {
            Inventory inventoryTop = player.getOpenInventory().getTopInventory();
            InventoryType topType = inventoryTop.getType();
            if (!(inventoryTop.getHolder() instanceof Player) && topType != InventoryType.SHULKER_BOX) return;
        }

        event.setCancelled(true);
        openShulker(player, clickItem);
    }

    private boolean openShulker(Player player, ItemStack item) {
        if (player == null || item == null) return false;
        if (item.getAmount() > 1) return false;
        UUID uuid = player.getUniqueId();

        long openCooldown = manager.getPlugin().getConfig().getLong("Features.ShulkerBox.cooldown", 1000L);
        if (manager.getOpenCooldowns().containsKey(uuid) && (System.currentTimeMillis() - manager.getOpenCooldowns().get(uuid) < openCooldown)) {
            long remain = (openCooldown - System.currentTimeMillis() + manager.getOpenCooldowns().get(uuid)) / 1000L + 1L;
            player.sendMessage("§7§l[§e§l雜項§7§l]§r§e界伏盒開啟冷卻中 剩餘§6 " + remain + " §e秒");
            return false;
        }
        manager.getOpenCooldowns().put(uuid, System.currentTimeMillis());

        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) return false;
        BlockState itemBlockState = ((BlockStateMeta) itemMeta).getBlockState();
        if (!(itemBlockState instanceof InventoryHolder)) return false;
        Inventory itemInv = ((InventoryHolder) itemBlockState).getInventory();

        TaskScheduler.runTask(player, manager.getPlugin(), () -> {
            Component title = Component.text("界伏盒");
            if (itemMeta != null && itemMeta.displayName() != null) title = itemMeta.displayName();
            Inventory cloneInv = manager.getPlugin().getServer().createInventory( player, InventoryType.SHULKER_BOX, title );
            cloneInv.setContents(itemInv.getContents());

            UUID shulkerBoxUuid = ShulkerBoxUtil.getUUID(item);
            if (shulkerBoxUuid == null ) shulkerBoxUuid = ShulkerBoxUtil.addUUID(item);
            player.closeInventory();
            player.openInventory(cloneInv);

            manager.getPlugin().sendDebug("§e已為 §b" + player.getName() + " §e開啟界伏盒: §a" + shulkerBoxUuid);
            manager.getOpenedShulkerBoxes().put(uuid, shulkerBoxUuid);
            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 1f);
        });

        return true;
    }
}
