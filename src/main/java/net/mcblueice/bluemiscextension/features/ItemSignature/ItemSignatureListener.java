package net.mcblueice.bluemiscextension.features.ItemSignature;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.view.AnvilView;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;

import io.papermc.paper.event.player.CartographyItemEvent;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluelib.utils.TaskScheduler;
import net.mcblueice.bluelib.utils.TextUtil;

public class ItemSignatureListener implements Listener {
    private final BlueMiscExtension plugin;
    private final ItemSignature itemSignature;
    private final ConfigManager lang;
    private final boolean debug;

    public ItemSignatureListener(BlueMiscExtension plugin, ItemSignature itemSignature) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.itemSignature = itemSignature;
        this.debug = plugin.getConfig().getBoolean("Features.ItemSignature.debug", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Player player = resolveViewer(event.getView());
        if (player == null) return;

        String resultSigner = getForeignSignerName(event.getInventory().getResult(), player);
        if (resultSigner != null) {
            event.getInventory().setResult(null);
            if (debug) plugin.sendDebug("阻擋外人署名輸出槽結果: " + player.getName() + " / owner=" + resultSigner + " @" + player.getWorld().getName() + "," + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String resultSigner = getForeignSignerName(event.getCurrentItem(), player);
        if (resultSigner != null) {
            event.setCancelled(true);
            notifyMapCopyBlocked(player, resultSigner);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCartographyResult(PrepareResultEvent event) {
        Player player = resolveViewer(event.getView());
        if (player == null) return;

        String resultSigner = getForeignSignerName(event.getResult(), player);
        if (resultSigner != null) {
            event.setResult(null);
            if (debug) plugin.sendDebug("阻擋製圖台外人署名輸出槽結果: " + player.getName() + " / owner=" + resultSigner + " @" + player.getWorld().getName() + "," + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCartographyTakeResult(CartographyItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        String resultSigner = getForeignSignerName(event.getCurrentItem(), player);
        if (resultSigner != null) {
            event.setCancelled(true);
            notifyMapCopyBlocked(player, resultSigner);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvilResult(PrepareAnvilEvent event) {
        Player player = resolveViewer(event.getView());
        if (player == null) return;

        if (isSignedAnvilResult(event.getResult())) {
            event.setResult(null);
            if (debug) plugin.sendDebug("阻擋鐵砧改名輸出槽結果: " + player.getName() + " @" + player.getWorld().getName() + "," + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilTakeResult(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getView() instanceof AnvilView)) return;
        if (!isSignedAnvilResult(event.getCurrentItem())) return;

        event.setCancelled(true);
        notifyAnvilRenameBlocked(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (itemSignature.getSignerUuid(event.getResult()) != null) {
            event.setCancelled(true);
            if (debug) plugin.sendDebug("阻擋 Crafter 產生帶署名輸出結果 @" + event.getBlock().getWorld().getName() + "," + event.getBlock().getX() + " " + event.getBlock().getY() + " " + event.getBlock().getZ());
        }
    }

    private Player resolveViewer(InventoryView view) {
        if (view == null) return null;
        if (view.getPlayer() instanceof Player player) return player;
        return null;
    }

    private String getForeignSignerName(ItemStack item, Player player) {
        if (player == null) return null;
        UUID signerUuid = itemSignature.getSignerUuid(item);
        if (signerUuid == null || signerUuid.equals(player.getUniqueId()) || player.hasPermission("bluemiscextension.itemsign.override")) return null;
        return itemSignature.getSignerName(item);
    }

    private boolean isSignedAnvilResult(ItemStack result) {
        return itemSignature.getSignerUuid(result) != null;
    }

    private void notifyMapCopyBlocked(Player player, String signerName) {
        TaskScheduler.runTask(player, plugin, () -> {
            String message = lang.get("Prefix.Default") + lang.get("ItemSignature.MapCopyBlocked", signerName);
            player.sendMessage(TextUtil.parse(message, true, false));
            player.updateInventory();
        });
    }

    private void notifyAnvilRenameBlocked(Player player) {
        TaskScheduler.runTask(player, plugin, () -> {
            String message = lang.get("Prefix.Default") + lang.get("ItemSignature.AnvilRenameBlocked");
            player.sendMessage(TextUtil.parse(message, true, false));
            player.updateInventory();
        });
    }
}