package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.gui;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.ArmorSystem;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArmorSettingsGUI implements Listener {

    private final BlueMiscExtension plugin;
    private final ConfigManager lang;
    private final ArmorSystem armorSystem;
    private final Component GUI_TITLE;
    private final NamespacedKey GUI_ITEM_KEY;

    public ArmorSettingsGUI(ArmorSystem armorSystem) {
        this.plugin = BlueMiscExtension.getInstance();
        this.lang = plugin.getLanguageManager();
        this.armorSystem = armorSystem;
        this.GUI_TITLE = lang.getComponent("ArmorSystem.GUI.Settings.Title");
        this.GUI_ITEM_KEY = new NamespacedKey(plugin, "armor_gui_item");
    }

    // 使用自定義 InventoryHolder 來取代舊的 Title 比對方法
    public static class ArmorGUIHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() { return null; }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new ArmorGUIHolder(), 54, GUI_TITLE);

        ItemStack border = createBorderItem();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        List<String> enabledEffects = armorSystem.getEnabledEffects(player);
        List<String> allEffects = new ArrayList<>(armorSystem.effectRegistry.keySet());
        Collections.sort(allEffects);

        // 中央區域定義 (4行 x 7列)
        int[] validSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        int index = 0;
        for (String effectId : allEffects) {
            if (index >= validSlots.length) break;
            boolean isEnabled = enabledEffects.contains(effectId);
            inv.setItem(validSlots[index++], createIcon(effectId, isEnabled));
        }

        // 下一頁
        inv.setItem(53, createAttributePageItem());

        // BossBar Toggle
        inv.setItem(0, createBossBarToggleItem(player));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
    }

    private ItemStack createAttributePageItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.getComponent("ArmorSystem.GUI.Settings.NextPage.Name").color(NamedTextColor.AQUA));
        List<Component> lore = new ArrayList<>();
        lore.add(lang.getComponent("ArmorSystem.GUI.Settings.NextPage.Lore"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(""));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createIcon(String effectId, boolean isEnabled) {
        Material material;
        Component statusComp;

        if (isEnabled) {
            material = Material.LIME_WOOL;
            statusComp = lang.getComponent("ArmorSystem.GUI.Settings.Status.Enabled");
        } else {
            material = Material.RED_WOOL;
            statusComp = lang.getComponent("ArmorSystem.GUI.Settings.Status.Disabled");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(lang.get("ArmorSystem.Effects." + effectId), NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(lang.getComponent("ArmorSystem.GUI.Settings.Lore.Toggle"));
            lore.add(Component.text(""));
            lore.add(lang.getComponent("ArmorSystem.GUI.Settings.Lore.StatusPrefix").append(statusComp));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(GUI_ITEM_KEY, PersistentDataType.STRING, effectId);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBossBarToggleItem(Player player) {
        boolean isEnabled = armorSystem.getBossBarManager().isShowBossBar(player);
        Material mat = isEnabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(isEnabled ? "§a顯示能量條: 開啓" : "§c顯示能量條: 關閉"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7點擊切換顯示能量條"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 使用 Holder 檢查，而非過時的 Title 比對
        if (event.getInventory().getHolder() instanceof ArmorGUIHolder) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            if (event.getRawSlot() == 0) {
                boolean current = armorSystem.getBossBarManager().isShowBossBar(player);
                armorSystem.getBossBarManager().setShowBossBar(player, !current);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                open(player);
                return;
            }

            // 下一頁跳轉
            if (clickedItem.getType() == Material.NETHER_STAR) {
                armorSystem.getAttributeGUI().open(player);
                return;
            }

            if (!clickedItem.hasItemMeta()) return;

            ItemMeta meta = clickedItem.getItemMeta();
            if (!meta.getPersistentDataContainer().has(GUI_ITEM_KEY, PersistentDataType.STRING)) return;

            String effectId = meta.getPersistentDataContainer().get(GUI_ITEM_KEY, PersistentDataType.STRING);

            boolean currentStatus = armorSystem.getEnabledEffects(player).contains(effectId);
            armorSystem.toggleEffect(player, effectId, !currentStatus);

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            open(player);
        }
    }
}
