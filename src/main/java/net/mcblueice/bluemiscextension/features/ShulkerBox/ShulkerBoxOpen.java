package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.UUID;

import org.bukkit.Tag;
import org.bukkit.Sound;
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
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ShulkerBox.ShulkerBox.ShulkerBoxHolder;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class ShulkerBoxOpen implements Listener {
    private final BlueMiscExtension plugin;
    private final boolean debug;

    public ShulkerBoxOpen(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("Features.ShulkerBox.debug", false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        if (!player.hasPermission("bluemiscextension.shulkerbox.open.hand")) return;

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (!player.isSneaking()) return;
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand == null || !Tag.SHULKER_BOXES.isTagged(mainHand.getType())) return;
            if (action == Action.PHYSICAL || ShulkerBoxUtil.isInteractableBlock(event.getClickedBlock())) return;

            event.setCancelled(openShulker(player, mainHand));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClick() != ClickType.SHIFT_RIGHT) return;
        if (!player.hasPermission("bluemiscextension.shulkerbox.open.inventory")) return;

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(player.getInventory())) return;

        ItemStack clickItem = event.getCurrentItem();
        if (clickItem == null || !Tag.SHULKER_BOXES.isTagged(clickItem.getType())) return;

        UUID itemUuid = ShulkerBoxUtil.getUUID(clickItem);
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top == null) return;

        if (top.getHolder() instanceof ShulkerBoxHolder holder) {
            if (itemUuid != null && itemUuid.equals(holder.getShulkerUuid())) return;
        } else {
            if (top.getType() != InventoryType.PLAYER && top.getType() != InventoryType.CRAFTING) return;
        }

        event.setCancelled(openShulker(player, clickItem));
    }

    private boolean openShulker(Player player, ItemStack item) {
        if (player == null || item == null) return false;
        if (item.getAmount() != 1) return false;
        UUID uuid = player.getUniqueId();

        long openCooldown = plugin.getConfig().getLong("Features.ShulkerBox.cooldown", 1000L);
        if (ShulkerBox.openCooldowns.containsKey(uuid)) {
            long lastOpen = ShulkerBox.openCooldowns.get(uuid);
            if (System.currentTimeMillis() - lastOpen < openCooldown) {
                long remain = (openCooldown - (System.currentTimeMillis() - lastOpen)) / 1000L + 1L;
                plugin.sendMessage(player, "§e界伏盒開啟冷卻中 剩餘§6 " + remain + " §e秒");
                return true;
            }
        }
        ShulkerBox.openCooldowns.put(uuid, System.currentTimeMillis());

        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta blockStateMeta)) return false;
        if (!(blockStateMeta.getBlockState() instanceof InventoryHolder itemBlockState)) return false;
        Inventory itemInv = itemBlockState.getInventory();
        ItemStack[] contents = itemInv.getContents();

        TaskScheduler.runTask(player, plugin, () -> {
            Component title = Component.translatable("block.minecraft.shulker_box");
            if (itemMeta != null && itemMeta.displayName() != null) title = itemMeta.displayName();

            UUID shulkerBoxUuid = ShulkerBoxUtil.getUUID(item);
            if (shulkerBoxUuid == null) shulkerBoxUuid = ShulkerBoxUtil.addUUID(item);

            ShulkerBoxHolder holder = new ShulkerBoxHolder(shulkerBoxUuid, player.getUniqueId());
            Inventory cloneInv = plugin.getServer().createInventory(holder, InventoryType.SHULKER_BOX, title);
            holder.setInventory(cloneInv);
            cloneInv.setContents(contents);
            player.openInventory(cloneInv);
            player.updateInventory();

            if (debug) plugin.sendDebug("§e已為 §b" + player.getName() + " §e開啟界伏盒 UUID: §7" + shulkerBoxUuid);
            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 1f);
        });

        long delayTicks = Math.max(200L, openCooldown / 5L);
        TaskScheduler.runTaskLater(plugin, () -> {
            ShulkerBox.openCooldowns.computeIfPresent(uuid, (key, value) -> null);
        }, delayTicks);

        return true;
    }
}