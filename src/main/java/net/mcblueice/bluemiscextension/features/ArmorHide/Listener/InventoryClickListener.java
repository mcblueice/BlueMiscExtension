package net.mcblueice.bluemiscextension.features.ArmorHide.Listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class InventoryClickListener implements Listener {
    private final BlueMiscExtension plugin;
    private final ArmorHide armorHide;

    public InventoryClickListener(BlueMiscExtension plugin, ArmorHide armorHide) {
        this.plugin = plugin;
        this.armorHide = armorHide;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getClickedInventory() instanceof PlayerInventory)) return;

        Player player = (Player) event.getWhoClicked();
        if (!armorHide.isArmorHidden(player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (event.isShiftClick()) {
            PlayerInventory inv = player.getInventory();
            String type = clickedItem.getType().name();

            if (!type.endsWith("_HELMET") && !type.endsWith("_CHESTPLATE") && !type.endsWith("_LEGGINGS") && !type.endsWith("_BOOTS")) return;

            if ((type.endsWith("_HELMET") && (inv.getHelmet() == null || inv.getHelmet().getType() == Material.AIR)) 
                || (type.endsWith("_CHESTPLATE") && (inv.getChestplate() == null || inv.getChestplate().getType() == Material.AIR))
                || (type.endsWith("_LEGGINGS") && (inv.getLeggings() == null || inv.getLeggings().getType() == Material.AIR))
                || (type.endsWith("_BOOTS") && (inv.getBoots() == null || inv.getBoots().getType() == Material.AIR))) {

                TaskScheduler.runTaskLater(player, plugin, () -> armorHide.updateSelf(player), 1L);
            }

        } else {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR) TaskScheduler.runTaskLater(player, plugin, () -> armorHide.updateSelf(player), 1L);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!armorHide.isArmorHidden(player)) return;

        for (int slot : event.getRawSlots()) {
            if (event.getView().getSlotType(slot) == InventoryType.SlotType.ARMOR) {
                TaskScheduler.runTaskLater(player, plugin, () -> armorHide.updateSelf(player), 1L);
                return;
            }
        }

    }
}
